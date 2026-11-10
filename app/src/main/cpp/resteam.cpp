// Resteam v1.7 - semua logika di C++ (UI ImGui landscape, stats, floating bubble, fetch info game, update GitHub, Shizuku shell via JNI shim)
#include <android_native_app_glue.h>
#include <android/native_window_jni.h>
#include <jni.h>
#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <atomic>
#include <mutex>
#include <thread>
#include <vector>
#include <string>
#include <map>
#include <chrono>
#include <algorithm>
#include <cfloat>
#include <cmath>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <ctime>
#include "imgui.h"
#include "imgui_impl_opengl3.h"

thread_local ImGuiContext* MyImGuiTLS = nullptr;
using std::string;
using Clock = std::chrono::steady_clock;

// ---------------- JNI ----------------
static JavaVM* g_vm; static jclass g_bridge, g_host_cls; static jobject g_act;
static jmethodID m_exec, m_state, m_req, m_canOv, m_askOv, m_start, m_stop, m_pip, m_imm, m_ins, m_sys, m_apps, m_icon, m_fetch, m_fimg, m_info, m_ver, m_dl, m_dlp, m_apk, m_inst, m_orient, m_open, m_resize, m_move;

struct Env {
    JNIEnv* e; bool att = false;
    Env() { if (g_vm->GetEnv((void**)&e, JNI_VERSION_1_6) != JNI_OK) { g_vm->AttachCurrentThread(&e, nullptr); att = true; } }
    ~Env() { if (att) g_vm->DetachCurrentThread(); }
};

extern "C" JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void*) {
    g_vm = vm; JNIEnv* e; vm->GetEnv((void**)&e, JNI_VERSION_1_6);
    jclass b = e->FindClass("com/udyneos/resteam/Bridge"); g_bridge = (jclass)e->NewGlobalRef(b);
    m_exec = e->GetStaticMethodID(b, "exec", "(Ljava/lang/String;)Ljava/lang/String;");
    m_state = e->GetStaticMethodID(b, "shizukuState", "()I");
    m_req = e->GetStaticMethodID(b, "shizukuRequest", "()V");
    m_canOv = e->GetStaticMethodID(b, "canOverlay", "(Landroid/content/Context;)Z");
    m_askOv = e->GetStaticMethodID(b, "askOverlay", "(Landroid/content/Context;)V");
    m_start = e->GetStaticMethodID(b, "startOverlay", "(Landroid/content/Context;I)V");
    m_stop = e->GetStaticMethodID(b, "stopOverlay", "(Landroid/content/Context;I)V");
    m_pip = e->GetStaticMethodID(b, "enterPip", "(Landroid/app/Activity;)V");
    m_imm = e->GetStaticMethodID(b, "immersive", "(Landroid/app/Activity;)V");
    m_ins = e->GetStaticMethodID(b, "insets", "(Landroid/app/Activity;)I");
    m_sys = e->GetStaticMethodID(b, "sysinfo", "(Landroid/content/Context;)Ljava/lang/String;");
    m_apps = e->GetStaticMethodID(b, "apps", "(Landroid/content/Context;)Ljava/lang/String;");
    m_icon = e->GetStaticMethodID(b, "icon", "(Landroid/content/Context;Ljava/lang/String;)[B");
    m_fetch = e->GetStaticMethodID(b, "fetch", "(Ljava/lang/String;)Ljava/lang/String;");
    m_fimg = e->GetStaticMethodID(b, "fetchImage", "(Ljava/lang/String;)[B");
    m_info = e->GetStaticMethodID(b, "info", "(Landroid/content/Context;Ljava/lang/String;)Ljava/lang/String;");
    m_ver = e->GetStaticMethodID(b, "version", "(Landroid/content/Context;)Ljava/lang/String;");
    m_dl = e->GetStaticMethodID(b, "download", "(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)Z");
    m_dlp = e->GetStaticMethodID(b, "dlProgress", "()I");
    m_apk = e->GetStaticMethodID(b, "apkPath", "(Landroid/content/Context;Ljava/lang/String;)Ljava/lang/String;");
    m_inst = e->GetStaticMethodID(b, "installApk", "(Landroid/content/Context;Ljava/lang/String;)V");
    m_orient = e->GetStaticMethodID(b, "setOrientation", "(Landroid/app/Activity;I)V");
    m_open = e->GetStaticMethodID(b, "openApp", "(Landroid/content/Context;)V");
    jclass o = e->FindClass("com/udyneos/resteam/HostService"); g_host_cls = (jclass)e->NewGlobalRef(o);
    m_resize = e->GetStaticMethodID(o, "resize", "(III)V");
    m_move = e->GetStaticMethodID(o, "moveBy", "(III)V");
    return JNI_VERSION_1_6;
}

static string jstr(JNIEnv* e, jstring r) {
    string o; if (r) { const char* p = e->GetStringUTFChars(r, 0); o = p; e->ReleaseStringUTFChars(r, p); e->DeleteLocalRef(r); } return o;
}
static string sh(const string& c) {
    Env v; jstring s = v.e->NewStringUTF(c.c_str());
    string o = jstr(v.e, (jstring)v.e->CallStaticObjectMethod(g_bridge, m_exec, s));
    v.e->DeleteLocalRef(s); return o;
}
static void bg(string c) { std::thread([c] { sh(c); }).detach(); }
static int shState() { Env v; return v.e->CallStaticIntMethod(g_bridge, m_state); }
static void trim(string& s) { while (!s.empty() && isspace((unsigned char)s.back())) s.pop_back(); }
static string callStr(jmethodID m, const string& a, bool withCtx) {
    Env v; jstring s = v.e->NewStringUTF(a.c_str());
    jstring r = withCtx ? (jstring)v.e->CallStaticObjectMethod(g_bridge, m, g_act, s) : (jstring)v.e->CallStaticObjectMethod(g_bridge, m, s);
    v.e->DeleteLocalRef(s); return jstr(v.e, r);
}
static std::vector<unsigned char> callBytes(jmethodID m, const string& a, bool withCtx) {
    Env v; std::vector<unsigned char> out; jstring s = v.e->NewStringUTF(a.c_str());
    jbyteArray r = withCtx ? (jbyteArray)v.e->CallStaticObjectMethod(g_bridge, m, g_act, s) : (jbyteArray)v.e->CallStaticObjectMethod(g_bridge, m, s);
    v.e->DeleteLocalRef(s);
    if (r) { jsize n = v.e->GetArrayLength(r); out.resize(n); v.e->GetByteArrayRegion(r, 0, n, (jbyte*)out.data()); v.e->DeleteLocalRef(r); }
    return out;
}

// ---------------- state ----------------
struct App { string pkg, label; bool game; };
struct Info { int state = 0; bool online = false; string title, desc, genre, dev, rating, count, ver; long long inst = 0, upd = 0, size = 0; };
static std::atomic<float> g_fps{-1}, g_cpu{-1}, g_gpu{-1}, g_ping{-1}, g_temp{0};
static std::atomic<int> g_sh{0}, g_mode{0}, g_tab{0}, g_bat{-1}, g_insL{0}, g_insR{0}, g_insT{0}, g_fpsSrc{0};
static std::atomic<bool> g_run{true}, g_sideOn{false}, g_sideOpen{false}, g_chg{false}, g_showAll{false}, g_updAvail{false};
static std::atomic<int> g_dlPct{0}, g_orient{0}; static string g_curVer;
static std::mutex g_mx, g_imx; static std::vector<App> g_apps; static std::vector<string> g_pinned;
static std::map<string, double> g_total, g_sess;                       // waktu bermain (detik), dilindungi g_mx
static std::map<string, Info> g_info; static std::map<string, std::vector<unsigned char>> g_coverPx;  // dilindungi g_imx
static string g_target, g_layer, g_net = "Offline", g_dir, g_selPkg;
static int g_miss = 0; static std::atomic<float> g_sideScroll{0};

static void saveListNL(const char* nm, const std::vector<string>& v) {
    FILE* f = fopen((g_dir + "/" + nm).c_str(), "w"); if (!f) return;
    for (auto& p : v) fprintf(f, "%s\n", p.c_str()); fclose(f);
}
static void loadList(const char* nm, std::vector<string>& v) {
    FILE* f = fopen((g_dir + "/" + nm).c_str(), "r"); if (!f) return;
    char b[512]; std::lock_guard<std::mutex> l(g_mx);
    while (fgets(b, sizeof b, f)) { string s = b; trim(s); if (!s.empty()) v.push_back(s); } fclose(f);
}
static bool inList(const std::vector<string>& v, const string& p) { return std::find(v.begin(), v.end(), p) != v.end(); }
static void savePlayNL() {
    FILE* f = fopen((g_dir + "/playtime.txt").c_str(), "w"); if (!f) return;
    for (auto& kv : g_total) fprintf(f, "%s\t%.0f\n", kv.first.c_str(), kv.second); fclose(f);
}
static void loadPlay() {
    FILE* f = fopen((g_dir + "/playtime.txt").c_str(), "r"); if (!f) return;
    char b[600]; std::lock_guard<std::mutex> l(g_mx);
    while (fgets(b, sizeof b, f)) { string s = b; trim(s); size_t t = s.find('\t'); if (t != string::npos) g_total[s.substr(0, t)] = atof(s.c_str() + t + 1); } fclose(f);
}
static void getPlayNL(const string& p, double& ses, double& tot) {
    auto a = g_sess.find(p); ses = a == g_sess.end() ? 0 : a->second;
    auto b = g_total.find(p); tot = b == g_total.end() ? 0 : b->second;
}

static float pingMs() {
    int s = socket(AF_INET, SOCK_STREAM, 0); if (s < 0) return -1;
    timeval tv{1, 0}; setsockopt(s, SOL_SOCKET, SO_SNDTIMEO, &tv, sizeof tv);
    sockaddr_in a{}; a.sin_family = AF_INET; a.sin_port = htons(53); inet_pton(AF_INET, "8.8.8.8", &a.sin_addr);
    auto t0 = Clock::now(); int r = connect(s, (sockaddr*)&a, sizeof a);
    float ms = std::chrono::duration<float, std::milli>(Clock::now() - t0).count(); close(s);
    return r == 0 ? ms : -1;
}

// Seksi output: C=/proc/stat G=gpu% B=kgsl gpubusy S=display measured_fps N=SurfaceFlinger frame counter R=game berjalan F=SF latency
static void parseStats(const string& out, long long& pt, long long& pi, double dt, std::vector<string>& runOut) {
    static long long lastCnt = -1; static Clock::time_point lastCntT;
    std::vector<long long> ts; std::vector<string> run; char m = 0; size_t p = 0; bool gotG = false; float sde = -1; long long cnt = -1;
    while (p < out.size()) {
        size_t e = out.find('\n', p); if (e == string::npos) e = out.size();
        string l = out.substr(p, e - p); p = e + 1;
        if (l.rfind("==", 0) == 0 && l.size() > 2) { m = l[2]; continue; }
        if (m == 'C' && l.rfind("cpu", 0) == 0) {
            long long v[8] = {0};
            sscanf(l.c_str(), "cpu %lld %lld %lld %lld %lld %lld %lld %lld", &v[0], &v[1], &v[2], &v[3], &v[4], &v[5], &v[6], &v[7]);
            long long idle = v[3] + v[4], tot = 0; for (int i = 0; i < 8; i++) tot += v[i];
            if (pt > 0 && tot > pt) g_cpu = 100.f * ((tot - pt) - (idle - pi)) / (float)(tot - pt);
            pt = tot; pi = idle;
        } else if (m == 'G' && !gotG && !l.empty() && isdigit((unsigned char)l[0])) {
            g_gpu = std::min(100.f, (float)atoi(l.c_str())); gotG = true;
        } else if (m == 'B' && !gotG) {
            long long a = 0, b = 0;
            if (sscanf(l.c_str(), "%lld %lld", &a, &b) == 2 && b > 0) { g_gpu = std::min(100.f, 100.f * a / b); gotG = true; }
        } else if (m == 'S' && sde < 0) {
            float v = -1; const char* q = strstr(l.c_str(), "fps");
            if (q && sscanf(q, "fps: %f", &v) == 1) sde = v; else if (sscanf(l.c_str(), "%f", &v) == 1) sde = v;
        } else if (m == 'N') {
            const char* q = strstr(l.c_str(), "Parcel("); unsigned long long a = 0, b = 0;
            if (q && sscanf(q + 7, "%llx %llx", &a, &b) == 2) cnt = (long long)b;
        } else if (m == 'R') { trim(l); if (!l.empty()) run.push_back(l); }
        else if (m == 'F') {
            long long a, b, c;
            if (sscanf(l.c_str(), "%lld %lld %lld", &a, &b, &c) == 3 && b > 0 && b < (1LL << 62)) ts.push_back(b);
        }
    }
    // --- FPS: 1) SurfaceFlinger latency per-layer, 2) display measured_fps, 3) selisih frame counter SF ---
    float cntFps = -1;
    if (cnt >= 0) {
        auto now = Clock::now();
        if (lastCnt >= 0 && cnt >= lastCnt) { float s = std::chrono::duration<float>(now - lastCntT).count(); if (s > 0.2f) cntFps = (cnt - lastCnt) / s; }
        lastCnt = cnt; lastCntT = now;
    }
    if (!ts.empty()) {
        long long mx = *std::max_element(ts.begin(), ts.end());
        int n = (int)std::count_if(ts.begin(), ts.end(), [&](long long t) { return t > mx - 1000000000LL; });
        g_fps = (float)n; g_fpsSrc = 1; g_miss = 0;
    } else {
        if (++g_miss >= 5) { std::lock_guard<std::mutex> l(g_mx); g_layer.clear(); g_miss = 0; }
        if (sde > 0) { g_fps = sde; g_fpsSrc = 2; }
        else if (cntFps > 0) { g_fps = cntFps; g_fpsSrc = 3; }
        else { g_fps = -1; g_fpsSrc = 0; }
    }
    runOut = run;
    // --- waktu bermain ---
    std::lock_guard<std::mutex> l(g_mx);
    for (auto& r : run) { g_total[r] += dt; g_sess[r] += dt; }
    for (auto it = g_sess.begin(); it != g_sess.end();) { if (std::find(run.begin(), run.end(), it->first) == run.end()) it = g_sess.erase(it); else ++it; }
}

static void refreshPkgs() {
    std::thread([] {
        string o = callStr(m_apps, "", true);
        std::vector<App> r; size_t p = 0;
        while (p < o.size()) {
            size_t e = o.find('\n', p); if (e == string::npos) e = o.size();
            string l = o.substr(p, e - p); p = e + 1;
            size_t t1 = l.find('\t'), t2 = (t1 == string::npos) ? t1 : l.find('\t', t1 + 1);
            if (t2 == string::npos) continue;
            r.push_back({l.substr(0, t1), l.substr(t1 + 1, t2 - t1 - 1), l.substr(t2 + 1) == "1"});
        }
        std::sort(r.begin(), r.end(), [](const App& x, const App& y) { return x.label < y.label; });
        std::lock_guard<std::mutex> g(g_mx); g_apps = r;
    }).detach();
}

static void sysInfo() {
    Env v; string s = jstr(v.e, (jstring)v.e->CallStaticObjectMethod(g_bridge, m_sys, g_act));
    string part[4]; size_t p = 0;
    for (int i = 0; i < 4; i++) { size_t e = (i < 3) ? s.find('|', p) : string::npos; if (e == string::npos) e = s.size(); part[i] = s.substr(p, e - p); p = e + 1; if (p > s.size()) break; }
    g_bat = atoi(part[0].c_str()); g_temp = (float)atof(part[1].c_str()); g_chg = part[2] == "1";
    { std::lock_guard<std::mutex> l(g_mx); g_net = part[3].empty() ? "Offline" : part[3]; }
    int ins = v.e->CallStaticIntMethod(g_bridge, m_ins, g_act);
    g_insT = (ins >> 20) & 0x3FF; g_insL = (ins >> 10) & 0x3FF; g_insR = ins & 0x3FF;
    g_dlPct = v.e->CallStaticIntMethod(g_bridge, m_dlp);
}

static void sessionTick(const std::vector<string>& run, double dt);
static void checkUpdate();
static bool autoUpdOn();

static const char* STATS_HEAD = R"SH(echo ==C;head -1 /proc/stat;echo ==G;for f in /sys/class/kgsl/kgsl-3d0/gpu_busy_percentage /sys/kernel/gpu/gpu_busy /sys/devices/platform/*mali*/utilization /sys/class/misc/mali0/device/utilization /sys/kernel/ged/hal/gpu_utilization /sys/class/devfreq/*gpu*/load /sys/class/devfreq/*mali*/load;do [ -r "$f" ] && { head -1 "$f"; break; };done;echo ==B;head -1 /sys/class/kgsl/kgsl-3d0/gpubusy 2>/dev/null;echo ==S;for f in /sys/class/drm/sde-crtc-0/measured_fps /sys/class/graphics/fb0/measured_fps;do [ -r "$f" ] && { head -1 "$f"; break; };done;echo ==N;service call SurfaceFlinger 1013 2>/dev/null|head -1;)SH";

static string pkgList() {
    std::vector<string> v; std::lock_guard<std::mutex> l(g_mx);
    auto add = [&](const string& p) { if (p.empty() || v.size() >= 80 || inList(v, p)) return;
        for (char c : p) if (!(isalnum((unsigned char)c) || c == '.' || c == '_')) return; v.push_back(p); };
    for (auto& a : g_apps) if (a.game) add(a.pkg);
    for (auto& p : g_pinned) add(p);
    add(g_target); string s; for (auto& p : v) s += p + " "; return s.empty() ? "x" : s;
}

static void statsLoop() {
    long long pt = 0, pi = 0; bool got = false; auto last = Clock::now(); int tick = 0;
    while (g_run) {
        g_ping = pingMs(); int st = shState(); g_sh = st; sysInfo();
        if (!got) { got = true; refreshPkgs(); }
        static bool updTried = false; if (!updTried && g_ping >= 0 && autoUpdOn()) { updTried = true; checkUpdate(); }
        auto now = Clock::now(); double dt = std::chrono::duration<double>(now - last).count(); last = now;
        if (st == 2) {
            string pkg, layer; { std::lock_guard<std::mutex> l(g_mx); pkg = g_target; layer = g_layer; }
            if (!pkg.empty() && layer.empty()) {
                string l = sh("dumpsys SurfaceFlinger --list 2>/dev/null | grep '" + pkg + "' | awk '/SurfaceView/{print;f=1;exit} {a=$0} END{if(!f)print a}'");
                trim(l); std::lock_guard<std::mutex> g(g_mx); if (g_target == pkg) g_layer = l; layer = l;
            }
            string cmd = string(STATS_HEAD) + "echo ==R;for p in " + pkgList() + ";do pidof $p >/dev/null 2>&1 && echo $p;done;echo ==F;";
            if (!layer.empty()) cmd += "dumpsys SurfaceFlinger --latency '" + layer + "' 2>/dev/null";
            std::vector<string> run; parseStats(sh(cmd), pt, pi, dt, run); sessionTick(run, dt);
        }
        if (++tick % 60 == 0) { std::lock_guard<std::mutex> l(g_mx); savePlayNL(); }
        std::this_thread::sleep_for(std::chrono::milliseconds(500));
    }
}

static string selPkg() { std::lock_guard<std::mutex> l(g_mx); return g_selPkg; }

// Tweak mode performa (bisa diatur di Settings): 0 refresh max, 1 kill app latar, 2 matikan doze, 3 DND, 4 animasi 0.5x
static std::atomic<bool> g_tw[7];
static const char* TW_NAME[7] = {"Paksa refresh rate maksimal", "Bersihkan app latar (kill-all)", "Matikan Doze saat main", "Mode Jangan Ganggu (DND)", "Percepat animasi sistem (0.5x)", "Auto-switch mode + resolusi saat game berjalan", "Cek update GitHub otomatis saat dibuka"};
static void saveTweaks() {
    FILE* f = fopen((g_dir + "/tweaks.txt").c_str(), "w"); if (!f) return;
    for (int i = 0; i < 7; i++) fputc(g_tw[i] ? '1' : '0', f); fputc('\n', f); fclose(f);
}
static void loadTweaks() {
    g_tw[0] = true; g_tw[1] = true; g_tw[2] = true; g_tw[3] = false; g_tw[4] = false; g_tw[5] = true; g_tw[6] = true;
    FILE* f = fopen((g_dir + "/tweaks.txt").c_str(), "r"); if (!f) return;
    char b[16] = {0}; if (fgets(b, sizeof b, f)) for (int i = 0; i < 7 && b[i] && b[i] != '\n'; i++) g_tw[i] = b[i] == '1'; fclose(f);
}

static void setMode(int m, const string& pkgO = "") {
    g_mode = m; string p = pkgO.empty() ? selPkg() : pkgO, c;
    static const char* gm[] = {"standard", "performance", "battery"};
    const string RR = "R=$(dumpsys display | grep -o 'fps=[0-9]*' | sed 's/fps=//' | sort -n | tail -1);[ -n \"$R\" ] && { settings put system min_refresh_rate $R;settings put system peak_refresh_rate $R; };";
    const string OFF = "cmd power set-fixed-performance-mode-enabled false;cmd power set-mode 3 false;cmd power set-mode 2 false;";
    const string ANIM = "settings put global window_animation_scale %s;settings put global transition_animation_scale %s;settings put global animator_duration_scale %s;";
    auto anim = [&](const char* v) { char b[256]; snprintf(b, sizeof b, ANIM.c_str(), v, v, v); return string(b); };
    if (m == 1) {
        c = "cmd power set-fixed-performance-mode-enabled true;cmd power set-mode 3 true;cmd power set-mode 2 true;settings put global low_power 0;";
        if (g_tw[1]) c += "am kill-all;";
        if (g_tw[2]) c += "dumpsys deviceidle disable;";
        if (g_tw[0]) c += RR;
        if (g_tw[3]) c += "cmd notification set_dnd priority;";
        if (g_tw[4]) c += anim("0.5");
    } else if (m == 2) {
        c = OFF + "settings put global low_power 1;dumpsys deviceidle enable;settings put system min_refresh_rate 60;settings put system peak_refresh_rate 60;";
    } else {
        c = OFF + "settings put global low_power 0;dumpsys deviceidle enable;settings delete system min_refresh_rate;settings delete system peak_refresh_rate;";
        if (g_tw[3]) c += "cmd notification set_dnd off;";
        if (g_tw[4]) c += anim("1.0");
    }
    if (!p.empty()) c += string("cmd game mode ") + gm[m] + " " + p;
    bg(c);
}
// freeform=true dipakai sidebar (jendela PIP/freeform); LAUNCH di library = false
static void launchPkg(const string& p, bool freeform) {
    if (p.empty()) return;
    { std::lock_guard<std::mutex> l(g_mx); g_target = p; g_layer.clear(); }
    g_fps = -1;
    if (freeform) bg("settings put global enable_freeform_support 1;settings put global force_resizable_activities 1;am start --windowingMode 5 -n $(cmd package resolve-activity --brief " + p + "|tail -n1)");
    else bg("monkey -p " + p + " -c android.intent.category.LAUNCHER 1");
}

// ---------------- Fetch info game (Google Play + data lokal) ----------------
static string htmlDecode(string s) {
    static const char* from[] = {"&amp;", "&quot;", "&#39;", "&lt;", "&gt;", "&#x27;", "&nbsp;"};
    static const char* to[] = {"&", "\"", "'", "<", ">", "'", " "};
    for (int i = 0; i < 7; i++) { size_t p; while ((p = s.find(from[i])) != string::npos) s.replace(p, strlen(from[i]), to[i]); }
    return s;
}
static string metaProp(const string& h, const char* prop) {
    size_t p = h.find(string("property=\"") + prop + "\""); if (p == string::npos) return "";
    size_t a = h.rfind('<', p), b = h.find('>', p); if (a == string::npos || b == string::npos) return "";
    size_t c = h.find("content=\"", a); if (c == string::npos || c > b) return ""; c += 9;
    size_t e = h.find('"', c); if (e == string::npos) return ""; return htmlDecode(h.substr(c, e - c));
}
static string jval(const string& h, const string& key, size_t from = 0) {
    size_t p = h.find(key, from); if (p == string::npos) return ""; p += key.size();
    while (p < h.size() && (h[p] == ' ' || h[p] == '"')) p++;
    size_t e = p; while (e < h.size() && h[e] != '"' && h[e] != ',' && h[e] != '}') e++; return h.substr(p, e - p);
}
static string cutUtf8(const string& s, size_t n) {
    if (s.size() <= n) return s; size_t k = n; while (k > 0 && ((unsigned char)s[k] & 0xC0) == 0x80) k--; return s.substr(0, k) + "...";
}
static void requestInfo(const string& pkg) {
    { std::lock_guard<std::mutex> l(g_imx); if (g_info.count(pkg)) return; g_info[pkg].state = 1; }
    std::thread([pkg] {
        Info in; in.state = 2;
        string loc = callStr(m_info, pkg, true); string part[4]; size_t p = 0;
        for (int i = 0; i < 4 && !loc.empty(); i++) { size_t e = (i < 3) ? loc.find('|', p) : string::npos; if (e == string::npos) e = loc.size(); part[i] = loc.substr(p, e - p); p = e + 1; if (p > loc.size()) break; }
        in.ver = part[0]; in.inst = atoll(part[1].c_str()); in.upd = atoll(part[2].c_str()); in.size = atoll(part[3].c_str());
        std::vector<unsigned char> cover;
        string html = callStr(m_fetch, "https://play.google.com/store/apps/details?id=" + pkg + "&hl=id&gl=ID", false);
        if (!html.empty()) {
            in.title = metaProp(html, "og:title"); size_t d = in.title.rfind(" - "); if (d != string::npos) in.title = in.title.substr(0, d);
            in.desc = cutUtf8(metaProp(html, "og:description"), 360);
            in.rating = jval(html, "\"ratingValue\":"); in.count = jval(html, "\"ratingCount\":");
            in.genre = jval(html, "\"applicationCategory\":"); if (in.genre.rfind("GAME_", 0) == 0) in.genre = in.genre.substr(5);
            size_t a = html.find("\"author\""); if (a != string::npos) in.dev = htmlDecode(jval(html, "\"name\":", a));
            in.online = !in.title.empty() || !in.desc.empty();
            string img = metaProp(html, "og:image");
            if (!img.empty()) { size_t q = img.rfind('='); if (img.find("googleusercontent.com") != string::npos && q != string::npos) img = img.substr(0, q) + "=s384";
                cover = callBytes(m_fimg, img, false); }
        }
        std::lock_guard<std::mutex> l(g_imx); g_info[pkg] = in; if (cover.size() > 8) g_coverPx[pkg] = cover;
    }).detach();
}

// ---------------- EGL + ImGui ----------------
struct Ctx {
    EGLDisplay d = EGL_NO_DISPLAY; EGLSurface s = EGL_NO_SURFACE; EGLContext c = EGL_NO_CONTEXT;
    ImGuiContext* im = nullptr; int w = 0, h = 0; float f = 30; Clock::time_point last = Clock::now();
};
static ImVec4 C(int r, int g, int b, int a = 255) { return ImVec4(r / 255.f, g / 255.f, b / 255.f, a / 255.f); }

static bool openCtx(Ctx& x, ANativeWindow* win, float f) {
    x.d = eglGetDisplay(EGL_DEFAULT_DISPLAY); eglInitialize(x.d, 0, 0);
    const EGLint a[] = {EGL_SURFACE_TYPE, EGL_WINDOW_BIT, EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
                        EGL_RED_SIZE, 8, EGL_GREEN_SIZE, 8, EGL_BLUE_SIZE, 8, EGL_ALPHA_SIZE, 8, EGL_NONE};
    EGLConfig cfg; EGLint n = 0; eglChooseConfig(x.d, a, &cfg, 1, &n); if (!n) return false;
    const EGLint ca[] = {EGL_CONTEXT_CLIENT_VERSION, 3, EGL_NONE};
    x.c = eglCreateContext(x.d, cfg, EGL_NO_CONTEXT, ca);
    x.s = eglCreateWindowSurface(x.d, cfg, win, nullptr);
    if (x.c == EGL_NO_CONTEXT || x.s == EGL_NO_SURFACE) return false;
    eglMakeCurrent(x.d, x.s, x.s, x.c);
    x.im = ImGui::CreateContext(); ImGui::SetCurrentContext(x.im);
    ImGuiIO& io = ImGui::GetIO(); io.IniFilename = nullptr;
    ImFontConfig fc; fc.SizePixels = f; io.Fonts->AddFontDefault(&fc); x.f = f;
    ImGui::StyleColorsDark(); ImGui::GetStyle().ScaleAllSizes(f / 13.f);
    ImGui_ImplOpenGL3_Init("#version 300 es"); x.last = Clock::now(); return true;
}
static void closeCtx(Ctx& x) {
    if (x.im) { eglMakeCurrent(x.d, x.s, x.s, x.c); ImGui::SetCurrentContext(x.im); ImGui_ImplOpenGL3_Shutdown(); ImGui::DestroyContext(x.im); x.im = nullptr; }
    if (x.d != EGL_NO_DISPLAY) {
        eglMakeCurrent(x.d, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        if (x.s != EGL_NO_SURFACE) eglDestroySurface(x.d, x.s);
        if (x.c != EGL_NO_CONTEXT) eglDestroyContext(x.d, x.c);
    }
    x.s = EGL_NO_SURFACE; x.c = EGL_NO_CONTEXT;
}
static void render(Ctx& x, void (*ui)(Ctx&), ImVec4 clr) {
    eglMakeCurrent(x.d, x.s, x.s, x.c); EGLint w, h;
    eglQuerySurface(x.d, x.s, EGL_WIDTH, &w); eglQuerySurface(x.d, x.s, EGL_HEIGHT, &h); x.w = w; x.h = h;
    ImGui::SetCurrentContext(x.im); ImGuiIO& io = ImGui::GetIO();
    auto t = Clock::now(); io.DeltaTime = std::max(1e-4f, std::chrono::duration<float>(t - x.last).count()); x.last = t;
    io.DisplaySize = ImVec2((float)w, (float)h);
    ImGui_ImplOpenGL3_NewFrame(); ImGui::NewFrame(); ui(x); ImGui::Render();
    glViewport(0, 0, w, h); glClearColor(clr.x, clr.y, clr.z, clr.w); glClear(GL_COLOR_BUFFER_BIT);
    ImGui_ImplOpenGL3_RenderDrawData(ImGui::GetDrawData()); eglSwapBuffers(x.d, x.s);
}

static string fv(float v, const char* u) { if (v < 0) return "--"; char b[32]; snprintf(b, sizeof b, "%.0f%s", v, u); return b; }
static ImU32 colPing(float p) { return p < 0 ? IM_COL32(143,152,160,255) : p < 60 ? IM_COL32(164,208,7,255) : p < 120 ? IM_COL32(255,200,40,255) : IM_COL32(230,70,60,255); }
static ImU32 colBat(int bat, bool chg) { return chg ? IM_COL32(26,159,255,255) : bat < 0 ? IM_COL32(143,152,160,255) : bat > 30 ? IM_COL32(164,208,7,255) : bat > 15 ? IM_COL32(255,200,40,255) : IM_COL32(230,70,60,255); }
static string shortName(const string& p) { size_t a = p.rfind('.'); if (a == string::npos || a == 0) return p; size_t b = p.rfind('.', a - 1); return b == string::npos ? p : p.substr(b + 1); }
static string labelOfNL(const string& pkg) { for (auto& a : g_apps) if (a.pkg == pkg) return a.label; return shortName(pkg); }
static string batText() { char b[48]; snprintf(b, sizeof b, "BAT %d%%  %.0fC%s", (int)g_bat, (float)g_temp, g_chg ? "  CHG" : ""); return b; }
static string clockText() { time_t t = time(nullptr); tm lt; localtime_r(&t, &lt); char b[16]; strftime(b, sizeof b, "%H:%M", &lt); return b; }
static string fmtHMS(double s) { int t = (int)s; char b[24]; snprintf(b, sizeof b, "%02d:%02d:%02d", t / 3600, (t % 3600) / 60, t % 60); return b; }
static string fmtTotal(double s) { int t = (int)s; char b[24]; snprintf(b, sizeof b, "%dj %02dm", t / 3600, (t % 3600) / 60); return b; }
static string fmtDate(long long ms) { if (ms <= 0) return "-"; time_t t = (time_t)(ms / 1000); tm lt; localtime_r(&t, &lt); char b[24]; strftime(b, sizeof b, "%Y-%m-%d", &lt); return b; }
static string fmtDateTime(long long sec) { if (sec <= 0) return "-"; time_t t = (time_t)sec; tm lt; localtime_r(&t, &lt); char b[24]; strftime(b, sizeof b, "%d/%m %H:%M", &lt); return b; }
static const char* fpsLabel() { switch (g_fpsSrc.load()) { case 1: return "FPS (GAME)"; case 2: return "FPS (DISPLAY)"; case 3: return "FPS (FRAME CNT)"; default: return "FPS"; } }

// ---------------- Profil game, riwayat sesi, auto-switch ----------------
struct Prof { int res = 100, mode = 1; };
struct Sess { string pkg, label; long long start = 0; int dur = 0; float avgFps = -1, minFps = -1, avgGpu = -1, avgCpu = -1, maxTemp = 0; int batS = -1, batE = -1; std::vector<float> fps, temp; };
struct SAcc { double dur = 0, fps = 0, gpu = 0, cpu = 0, tmr = 0; int nf = 0, ng = 0, nc = 0; };
static std::map<string, Prof> g_prof; static std::vector<Sess> g_hist; static Sess g_cur; static SAcc g_acc;   // dilindungi g_mx
static string g_active; static int g_gone = 0; static bool g_autoApplied = false, g_resApplied = false;

// Resolusi per game: persen dari ukuran fisik; density ikut diskalakan supaya UI tidak membesar
static string resCmd(int pct) {
    if (pct >= 100) return "wm size reset;wm density reset;";
    char b[800];
    snprintf(b, sizeof b, "S=$(wm size|grep -i physical|head -1|sed 's/.*: //');W=${S%%x*};H=${S#*x};D=$(wm density|grep -i physical|head -1|sed 's/.*: //');wm size $((W*%d/100/2*2))x$((H*%d/100/2*2));wm density $((D*%d/100));", pct, pct, pct);
    return b;
}
static std::vector<string> splitTab(const string& s) {
    std::vector<string> v; size_t p = 0;
    while (true) { size_t e = s.find('\t', p); if (e == string::npos) { v.push_back(s.substr(p)); break; } v.push_back(s.substr(p, e - p)); p = e + 1; }
    return v;
}
static string csvOut(const std::vector<float>& v, const char* fmt) {
    string s; size_t stride = v.size() > 90 ? (v.size() + 89) / 90 : 1; char b[24];
    for (size_t i = 0; i < v.size(); i += stride) { snprintf(b, sizeof b, fmt, v[i]); s += b; }
    return s.empty() ? "-" : s;
}
static std::vector<float> csvIn(const string& s) {
    std::vector<float> v; if (s == "-") return v; size_t p = 0;
    while (p < s.size()) { size_t e = s.find(',', p); if (e == string::npos) e = s.size(); if (e > p) v.push_back((float)atof(s.substr(p, e - p).c_str())); p = e + 1; }
    return v;
}
static void saveProfNL() {
    FILE* f = fopen((g_dir + "/profiles.txt").c_str(), "w"); if (!f) return;
    for (auto& kv : g_prof) fprintf(f, "%s\t%d\t%d\n", kv.first.c_str(), kv.second.res, kv.second.mode); fclose(f);
}
static void loadProf() {
    FILE* f = fopen((g_dir + "/profiles.txt").c_str(), "r"); if (!f) return;
    char b[600]; std::lock_guard<std::mutex> l(g_mx);
    while (fgets(b, sizeof b, f)) { string s = b; trim(s); auto v = splitTab(s); if (v.size() >= 3) { Prof p; p.res = atoi(v[1].c_str()); p.mode = std::max(0, std::min(2, atoi(v[2].c_str()))); g_prof[v[0]] = p; } }
    fclose(f);
}
static void saveHistNL() {
    FILE* f = fopen((g_dir + "/sessions.txt").c_str(), "w"); if (!f) return;
    for (auto& s : g_hist)
        fprintf(f, "%s\t%s\t%lld\t%d\t%.1f\t%.1f\t%.1f\t%.1f\t%.1f\t%d\t%d\t%s\t%s\n", s.pkg.c_str(), s.label.c_str(), s.start, s.dur, s.avgFps, s.minFps, s.avgGpu, s.avgCpu, s.maxTemp, s.batS, s.batE,
                csvOut(s.fps, "%.0f,").c_str(), csvOut(s.temp, "%.1f,").c_str());
    fclose(f);
}
static void loadHist() {
    FILE* f = fopen((g_dir + "/sessions.txt").c_str(), "r"); if (!f) return;
    char b[8192]; std::lock_guard<std::mutex> l(g_mx);
    while (fgets(b, sizeof b, f)) {
        string s = b; trim(s); auto v = splitTab(s); if (v.size() < 13) continue;
        Sess x; x.pkg = v[0]; x.label = v[1]; x.start = atoll(v[2].c_str()); x.dur = atoi(v[3].c_str());
        x.avgFps = (float)atof(v[4].c_str()); x.minFps = (float)atof(v[5].c_str()); x.avgGpu = (float)atof(v[6].c_str()); x.avgCpu = (float)atof(v[7].c_str());
        x.maxTemp = (float)atof(v[8].c_str()); x.batS = atoi(v[9].c_str()); x.batE = atoi(v[10].c_str()); x.fps = csvIn(v[11]); x.temp = csvIn(v[12]);
        g_hist.push_back(x);
    }
    fclose(f);
}

static void startSession(const string& pkg) {
    Prof pf;
    { std::lock_guard<std::mutex> l(g_mx); auto it = g_prof.find(pkg); if (it != g_prof.end()) pf = it->second;
      g_cur = Sess(); g_cur.pkg = pkg; g_cur.label = labelOfNL(pkg); g_cur.start = (long long)time(nullptr); g_cur.batS = g_bat; g_acc = SAcc();
      g_target = pkg; g_layer.clear(); }
    g_fps = -1;
    if (g_tw[5]) {  // auto-switch: mode + resolusi sesuai profil game
        setMode(pf.mode, pkg); g_autoApplied = true;
        if (pf.res < 100) { bg(resCmd(pf.res)); g_resApplied = true; }
    }
}
static void endSession(bool revert) {
    string pkg; bool au = g_autoApplied, rs = g_resApplied;
    { std::lock_guard<std::mutex> l(g_mx);
      if (g_cur.pkg.empty()) return;
      Sess s = g_cur; pkg = s.pkg; s.dur = (int)g_acc.dur; s.batE = g_bat;
      s.avgFps = g_acc.nf ? (float)(g_acc.fps / g_acc.nf) : -1; s.avgGpu = g_acc.ng ? (float)(g_acc.gpu / g_acc.ng) : -1; s.avgCpu = g_acc.nc ? (float)(g_acc.cpu / g_acc.nc) : -1;
      if (s.dur >= 30) { g_hist.push_back(s); if (g_hist.size() > 60) g_hist.erase(g_hist.begin()); saveHistNL(); }
      g_cur = Sess(); g_acc = SAcc(); }
    if (revert && au) { setMode(0, pkg); if (rs) bg(resCmd(100)); }
    g_autoApplied = false; g_resApplied = false;
}
static void sampleSession(double dt) {
    std::lock_guard<std::mutex> l(g_mx);
    if (g_cur.pkg.empty()) return;
    float fps = g_fps, gpu = g_gpu, cpu = g_cpu, tmp = g_temp;
    g_acc.dur += dt; g_cur.dur = (int)g_acc.dur;
    if (fps > 0) { g_acc.fps += fps; g_acc.nf++; if (g_acc.dur > 8 && (g_cur.minFps < 0 || fps < g_cur.minFps)) g_cur.minFps = fps; }
    if (gpu >= 0) { g_acc.gpu += gpu; g_acc.ng++; }
    if (cpu >= 0) { g_acc.cpu += cpu; g_acc.nc++; }
    if (tmp > g_cur.maxTemp) g_cur.maxTemp = tmp;
    g_acc.tmr += dt;
    if (g_acc.tmr >= 5) {
        g_acc.tmr = 0; g_cur.fps.push_back(fps > 0 ? fps : 0); g_cur.temp.push_back(tmp);
        if (g_cur.fps.size() >= 240) { std::vector<float> a, b; for (size_t i = 0; i < g_cur.fps.size(); i += 2) { a.push_back(g_cur.fps[i]); b.push_back(g_cur.temp[i]); } g_cur.fps = a; g_cur.temp = b; }
    }
}
// Dipanggil tiap siklus stats: deteksi game mulai/berhenti -> sesi + auto-switch
static void sessionTick(const std::vector<string>& run, double dt) {
    string cand, active;
    { std::lock_guard<std::mutex> l(g_mx); active = g_active;
      auto isCand = [&](const string& p) { if (p == g_target) return true; for (auto& a : g_apps) if (a.pkg == p && a.game) return true; return false; };
      if (!active.empty() && inList(run, active)) cand = active; else for (auto& r : run) if (isCand(r)) { cand = r; break; } }
    if (!active.empty() && cand == active) g_gone = 0;
    else if (!active.empty() && cand.empty()) {
        if (++g_gone >= 3) { endSession(true); std::lock_guard<std::mutex> l(g_mx); g_active.clear(); g_gone = 0; }  // toleransi 3 siklus
    } else if (cand != active) {
        if (!active.empty()) endSession(true);
        startSession(cand); { std::lock_guard<std::mutex> l(g_mx); g_active = cand; } g_gone = 0;
    }
    bool on; { std::lock_guard<std::mutex> l(g_mx); on = !g_active.empty(); }
    if (on) sampleSession(dt);
}

// ---------------- Update dari GitHub Releases + rotasi aplikasi ----------------
#ifndef GITHUB_REPO
#define GITHUB_REPO "udyneos/Resteam"
#endif
struct Upd { int state = 0; string latest, notes, url, err; };   // 0 idle, 1 cek, 2 terbaru, 3 tersedia, 4 unduh, 5 pasang, 6 galat
static Upd g_upd; static std::mutex g_umx;

static bool autoUpdOn() { return g_tw[6]; }
static string curVersion() { Env v; return jstr(v.e, (jstring)v.e->CallStaticObjectMethod(g_bridge, m_ver, g_act)); }
static std::vector<int> parseVer(string s) {
    if (!s.empty() && (s[0] == 'v' || s[0] == 'V')) s.erase(0, 1);
    size_t d = s.find('-'); if (d != string::npos) s = s.substr(0, d);
    std::vector<int> v; size_t p = 0;
    while (p <= s.size()) { size_t e = s.find('.', p); if (e == string::npos) e = s.size(); v.push_back(atoi(s.substr(p, e - p).c_str())); p = e + 1; }
    while (v.size() < 3) v.push_back(0); return v;
}
static string jsonStr(const string& h, const string& key, size_t from = 0, size_t* endPos = nullptr) {
    size_t p = h.find(key, from); if (p == string::npos) return ""; p += key.size();
    while (p < h.size() && (h[p] == ' ' || h[p] == ':')) p++;
    if (p >= h.size() || h[p] != '"') return ""; p++;
    string o;
    while (p < h.size() && h[p] != '"') {
        if (h[p] == '\\' && p + 1 < h.size()) { char c = h[p + 1]; p += 2; if (c == 'n') o += '\n'; else if (c == 'r') {} else if (c == 't') o += ' '; else if (c == 'u') { p += 4; o += '?'; } else o += c; }
        else o += h[p++];
    }
    if (endPos) *endPos = p; return o;
}
static void checkUpdate() {
    { std::lock_guard<std::mutex> l(g_umx); if (g_upd.state == 1 || g_upd.state == 4 || g_upd.state == 5) return; g_upd.state = 1; g_upd.err.clear(); }
    std::thread([] {
        string js = callStr(m_fetch, string("https://api.github.com/repos/") + GITHUB_REPO + "/releases/latest", false);
        Upd u; string tag = jsonStr(js, "\"tag_name\"");
        if (js.empty() || tag.empty()) { u.state = 6; u.err = "Tidak bisa cek GitHub (offline, repo privat, atau belum ada release)."; }
        else {
            u.latest = tag; u.notes = cutUtf8(jsonStr(js, "\"body\""), 700);
            size_t p = 0; string first;
            while (true) {
                size_t e = 0; string v = jsonStr(js, "\"browser_download_url\"", p, &e); if (v.empty()) break; p = e + 1;
                if (v.size() > 4 && v.substr(v.size() - 4) == ".apk") { if (first.empty()) first = v; if (v.find("release") != string::npos) { u.url = v; break; } }
            }
            if (u.url.empty()) u.url = first;
            if (parseVer(tag) > parseVer(g_curVer)) { u.state = u.url.empty() ? 6 : 3; if (u.state == 6) u.err = "Rilis baru ada tapi tanpa berkas APK."; } else u.state = 2;
        }
        g_updAvail = (u.state == 3);
        std::lock_guard<std::mutex> l(g_umx); g_upd = u;
    }).detach();
}
static void downloadUpdate() {
    string url; { std::lock_guard<std::mutex> l(g_umx); if (g_upd.state != 3) return; url = g_upd.url; g_upd.state = 4; }
    g_dlPct = 0;
    std::thread([url] {
        Env v; jstring a = v.e->NewStringUTF(url.c_str()), b = v.e->NewStringUTF("Resteam-update.apk");
        jboolean ok = v.e->CallStaticBooleanMethod(g_bridge, m_dl, g_act, a, b); v.e->DeleteLocalRef(a); v.e->DeleteLocalRef(b);
        if (!ok) { std::lock_guard<std::mutex> l(g_umx); g_upd.state = 6; g_upd.err = "Unduhan gagal."; return; }
        string path = callStr(m_apk, "Resteam-update.apk", true);
        { std::lock_guard<std::mutex> l(g_umx); g_upd.state = 5; }
        bool done = false;
        if (shState() == 2) { string o = sh("pm install -r -d '" + path + "'"); done = o.find("Success") != string::npos; }   // pasang senyap via Shizuku
        if (!done) { jstring p = v.e->NewStringUTF(path.c_str()); v.e->CallStaticVoidMethod(g_bridge, m_inst, g_act, p); v.e->DeleteLocalRef(p); }   // fallback: installer sistem
    }).detach();
}

static void saveOrient() { FILE* f = fopen((g_dir + "/orient.txt").c_str(), "w"); if (!f) return; fprintf(f, "%d\n", (int)g_orient); fclose(f); }
static void loadOrient() { FILE* f = fopen((g_dir + "/orient.txt").c_str(), "r"); if (!f) return; int v = 0; if (fscanf(f, "%d", &v) == 1) g_orient = std::max(0, std::min(2, v)); fclose(f); }
static void applyOrient() { Env v; v.e->CallStaticVoidMethod(g_bridge, m_orient, g_act, (jint)g_orient.load()); }

// ---------------- Floating bubble ----------------
static const float BUB = 40, SIDE_W = 220, SIDE_H = 390, INFO = 78, MODEH = 40, ROW = 44, SETBTN = 72;
struct Host { std::thread th; std::atomic<bool> run{false}; float d = 1; };
static Host g_host[2];

// Bubble: menampilkan FPS live (bukan ikon); cincin mengikuti mode (biru=balanced, oranye=performance, hijau=battery)
static ImU32 colFps(float v) { return v < 0 ? IM_COL32(143, 152, 160, 255) : v >= 50 ? IM_COL32(164, 208, 7, 255) : v >= 30 ? IM_COL32(255, 200, 40, 255) : IM_COL32(230, 70, 60, 255); }
static void drawBubble(ImDrawList* d, ImVec2 c, float R, float pulse, int mode, const string& txt) {
    ImU32 bc = mode == 1 ? IM_COL32(255, 170, 30, 255) : mode == 2 ? IM_COL32(164, 208, 7, 255) : IM_COL32(102, 192, 244, 255);
    d->AddCircleFilled(c, R, IM_COL32(23, 26, 33, 240), 40);
    d->AddCircle(c, R - 1.f, (bc & 0x00FFFFFF) | ((ImU32)(170 + 85 * pulse) << 24), 40, 2.f);
    ImFont* ft = ImGui::GetFont(); float sz = R * (txt.size() >= 3 ? 0.8f : 1.05f); ImVec2 ts = ft->CalcTextSizeA(sz, FLT_MAX, 0, txt.c_str());
    d->AddText(ft, sz, {c.x - ts.x * 0.5f, c.y - ts.y * 0.5f}, colFps(g_fps), txt.c_str());
}
static void openApp() {   // tombol Settings di bubble: kembali ke aplikasi, tab Settings
    g_tab = 2; Env v; v.e->CallStaticVoidMethod(g_bridge, m_open, g_act);
}

static void sideUi(Ctx& x) {
    ImDrawList* d = ImGui::GetBackgroundDrawList(); float W = x.w, H = x.h, f = x.f, dn = g_host[1].d;
    ImFont* ft = ImGui::GetFont(); float bub = BUB * dn, y0 = (BUB + 4) * dn, inf = INFO * dn, mh = MODEH * dn, rh = ROW * dn, fs = f * 0.82f;
    float pulse = 0.5f + 0.5f * sinf((float)ImGui::GetTime() * 3.f); int mode = g_mode;
    string fpsT = fv(g_fps, "");
    if (!g_sideOpen) { drawBubble(d, {W * 0.5f, H * 0.5f}, std::min(W, H) * 0.5f - 2.f, pulse, mode, fpsT); return; }
    drawBubble(d, {bub * 0.5f, bub * 0.5f}, bub * 0.5f - 2.f, pulse, mode, fpsT);
    d->AddText(ft, f * 1.0f, {bub + f * 0.4f, bub * 0.5f - f * 0.5f}, IM_COL32(143, 152, 160, 255), "RESTEAM");
    { float sbw = SETBTN * dn; ImVec2 b0(W - sbw - f * 0.4f, bub * 0.15f), b1(W - f * 0.4f, bub * 0.85f);   // tombol Settings -> kembali ke aplikasi
      d->AddRectFilled(b0, b1, IM_COL32(26, 159, 255, 255), f * 0.5f);
      float tw = ft->CalcTextSizeA(fs, FLT_MAX, 0, "SETTINGS").x; d->AddText(ft, fs, {b0.x + (sbw - tw) * 0.5f, (b0.y + b1.y) * 0.5f - fs * 0.5f}, IM_COL32(255, 255, 255, 255), "SETTINGS"); }
    // panel turun ke bawah dari bubble
    d->AddRectFilled({0, y0}, {W, H}, IM_COL32(23, 26, 33, 238), f * 0.8f);
    d->AddRect({1, y0}, {W - 1, H - 1}, IM_COL32(102, 192, 244, 170), f * 0.8f, 0, 2.f);
    string bt = batText(), ck = clockText(), net; double ses = 0, tot = 0;
    { std::lock_guard<std::mutex> l(g_mx); net = g_net; getPlayNL(g_target, ses, tot); }
    d->AddText(ft, fs, {f * 0.7f, y0 + f * 0.25f}, colBat(g_bat, g_chg), bt.c_str());
    d->AddText(ft, fs, {W - f * 0.7f - ft->CalcTextSizeA(fs, FLT_MAX, 0, ck.c_str()).x, y0 + f * 0.25f}, IM_COL32(255, 255, 255, 255), ck.c_str());
    d->AddText(ft, fs, {f * 0.7f, y0 + f * 1.25f}, IM_COL32(102, 192, 244, 255), net.c_str());
    char b[64]; snprintf(b, sizeof b, "FPS %s  GPU %s  %sms", fv(g_fps, "").c_str(), fv(g_gpu, "%").c_str(), fv(g_ping, "").c_str());
    d->AddText(ft, fs, {f * 0.7f, y0 + f * 2.25f}, IM_COL32(164, 208, 7, 255), b);
    snprintf(b, sizeof b, "PLAY %s", ses > 0 ? fmtHMS(ses).c_str() : "--:--:--"); d->AddText(ft, fs, {f * 0.7f, y0 + f * 3.25f}, IM_COL32(255, 255, 255, 255), b);
    // switch mode performa
    float my = y0 + inf; d->AddLine({0, my}, {W, my}, IM_COL32(61, 77, 92, 255));
    static const char* mnm[] = {"BAL", "PERF", "BAT"}; float segW = W / 3.f;
    for (int i = 0; i < 3; i++) {
        float x0 = i * segW; ImU32 col = mode == i ? (i == 1 ? IM_COL32(255, 150, 20, 255) : i == 2 ? IM_COL32(110, 150, 10, 255) : IM_COL32(26, 159, 255, 255)) : IM_COL32(61, 77, 92, 255);
        d->AddRectFilled({x0 + f * 0.4f, my + mh * 0.14f}, {x0 + segW - f * 0.4f, my + mh * 0.86f}, col, f * 0.4f);
        float tw = ft->CalcTextSizeA(fs, FLT_MAX, 0, mnm[i]).x; d->AddText(ft, fs, {x0 + (segW - tw) * 0.5f, my + (mh - fs) * 0.5f}, IM_COL32(255, 255, 255, 255), mnm[i]);
    }
    float ly = my + mh; d->AddLine({0, ly}, {W, ly}, IM_COL32(61, 77, 92, 255));
    std::lock_guard<std::mutex> l(g_mx);
    if (g_pinned.empty()) { d->AddText(ft, f * 0.9f, {f, ly + f}, IM_COL32(143, 152, 160, 255), "Pilih app di Settings"); return; }
    d->PushClipRect({0, ly}, {W, H}, true); float sc = g_sideScroll;
    for (size_t i = 0; i < g_pinned.size(); i++) {
        float y = ly + i * rh - sc; if (y + rh < ly || y > H) continue;
        d->AddRectFilled({f * 0.5f, y + rh * 0.08f}, {W - f * 0.5f, y + rh * 0.92f}, IM_COL32(61, 77, 92, 255), f * 0.3f);
        string nm = labelOfNL(g_pinned[i]); ImVec4 clip(f, y, W - f * 0.8f, y + rh);
        d->AddText(ft, f, {f, y + rh * 0.5f - f * 0.5f}, IM_COL32(235, 240, 245, 255), nm.c_str(), nullptr, 0.0f, &clip);
    }
    d->PopClipRect();
}

static void ovLoop(int id, ANativeWindow* win, float dens) {
    Ctx x;
    if (openCtx(x, win, 12.f * dens)) {
        eglSwapInterval(x.d, 0);
        while (g_host[id].run) { render(x, sideUi, ImVec4(0, 0, 0, 0)); std::this_thread::sleep_for(std::chrono::milliseconds(33)); }
    }
    closeCtx(x); ANativeWindow_release(win);
}
extern "C" JNIEXPORT void JNICALL Java_com_udyneos_resteam_HostService_nativeSurface(JNIEnv* e, jclass, jint id, jobject s, jfloat d) {
    Host& h = g_host[id]; h.run = false; if (h.th.joinable()) h.th.join();
    if (!s) return;
    ANativeWindow* w = ANativeWindow_fromSurface(e, s); h.d = d;
    g_sideOpen = false; g_sideScroll = 0;
    h.run = true; h.th = std::thread(ovLoop, (int)id, w, (float)d);
}
static void resizeHost(JNIEnv* e, int id, float wdp, float hdp) {
    float d = g_host[id].d; e->CallStaticVoidMethod(g_host_cls, m_resize, (jint)id, (jint)(wdp * d), (jint)(hdp * d));
}
extern "C" JNIEXPORT void JNICALL Java_com_udyneos_resteam_HostService_nativeTouch(JNIEnv* e, jclass, jint, jint a, jfloat rx, jfloat ry, jfloat x, jfloat y) {
    static float lx, ly, sx, sy, lyl; static bool drag; static int zone; float dn = g_host[1].d;
    // zone: 0 bubble tertutup, 1 baris atas, 5 tombol Settings, 3 info, 4 switch mode, 2 list.
    // Drag pindah HANYA di zone 0/1 (bagian atas). tap: 0 = buka, 1 = tutup, 5 = ke aplikasi, 4 = ganti mode, 2 = jalankan app (PIP); drag zone 2 = scroll
    float y0 = (BUB + 4) * dn, inf = INFO * dn, mh = MODEH * dn, listTop = y0 + inf + mh, rh = ROW * dn, W = SIDE_W * dn;
    if (a == 0) { drag = false; sx = lx = rx; sy = ly = ry; lyl = y; zone = !g_sideOpen ? 0 : (y < y0 ? (x > W - (SETBTN + 4) * dn ? 5 : 1) : (y < y0 + inf ? 3 : (y < listTop ? 4 : 2))); }
    else if (a == 2) {
        if (!drag && hypotf(rx - sx, ry - sy) > 8 * dn) drag = true;
        if (drag) {
            if (zone == 0 || zone == 1) { e->CallStaticVoidMethod(g_host_cls, m_move, 1, (jint)(rx - lx), (jint)(ry - ly)); lx = rx; ly = ry; }
            else if (zone == 2) {
                size_t n; { std::lock_guard<std::mutex> l(g_mx); n = g_pinned.size(); }
                float mx = std::max(0.f, n * rh - (SIDE_H * dn - listTop));
                g_sideScroll = std::min(mx, std::max(0.f, g_sideScroll - (y - lyl))); lyl = y;
            }
        }
    } else if (a == 1 && !drag) {
        if (zone == 0) { g_sideOpen = true; resizeHost(e, 1, SIDE_W, SIDE_H); return; }
        if (zone == 1) { g_sideOpen = false; resizeHost(e, 1, BUB, BUB); return; }
        if (zone == 5) { openApp(); g_sideOpen = false; resizeHost(e, 1, BUB, BUB); return; }
        if (zone == 4) { int i = (int)(x / (W / 3.f)); setMode(std::max(0, std::min(2, i))); return; }
        if (zone != 2) return;
        int idx = (int)((y + g_sideScroll - listTop) / rh); string p;
        { std::lock_guard<std::mutex> l(g_mx); if (idx >= 0 && idx < (int)g_pinned.size()) p = g_pinned[idx]; }
        if (!p.empty()) { launchPkg(p, true); g_sideOpen = false; resizeHost(e, 1, BUB, BUB); }
    }
}

// ---------------- Tekstur: ikon + cover ----------------
struct Icon { GLuint tex = 0; float r = 0.25f, g = 0.35f, b = 0.5f, w = 1, h = 1; bool tried = false; };
static std::map<string, Icon> g_icons, g_covers; static int g_iconBudget = 0;
static GLuint makeTex(const unsigned char* px, int w, int h) {
    GLuint t; glGenTextures(1, &t); glBindTexture(GL_TEXTURE_2D, t);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR); glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE); glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, px); return t;
}
static void loadIcon(const string& pkg, Icon& ic) {
    std::vector<unsigned char> px = callBytes(m_icon, pkg, true); const int S = 128;
    if (px.size() != (size_t)S * S * 4) return;
    long R = 0, G = 0, B = 0, N = 0;
    for (int i = 0; i < S * S; i++) if (px[i * 4 + 3] > 128) { R += px[i * 4]; G += px[i * 4 + 1]; B += px[i * 4 + 2]; N++; }
    if (N) { ic.r = R / (255.f * N); ic.g = G / (255.f * N); ic.b = B / (255.f * N); }
    ic.tex = makeTex(px.data(), S, S);
}
static Icon& iconFor(const string& pkg) {
    Icon& ic = g_icons[pkg];
    if (!ic.tried && g_iconBudget > 0) { g_iconBudget--; ic.tried = true; loadIcon(pkg, ic); }
    return ic;
}
static void uploadCovers() {
    std::lock_guard<std::mutex> l(g_imx);
    for (auto& kv : g_coverPx) {
        auto& b = kv.second; int w = b[0] | (b[1] << 8), h = b[4] | (b[5] << 8);
        if (w > 0 && h > 0 && b.size() >= (size_t)8 + w * h * 4) { Icon& c = g_covers[kv.first]; c.tex = makeTex(b.data() + 8, w, h); c.w = (float)w; c.h = (float)h; c.tried = true; }
    }
    g_coverPx.clear();
}

// ---------------- UI utama (landscape, gaya PS4/Steam) ----------------
static bool Btn(const char* l, ImVec4 c, ImVec2 s) {
    ImGui::PushStyleColor(ImGuiCol_Button, c);
    ImGui::PushStyleColor(ImGuiCol_ButtonHovered, c);
    ImGui::PushStyleColor(ImGuiCol_ButtonActive, ImVec4(c.x * 1.2f, c.y * 1.2f, c.z * 1.2f, 1));
    bool r = ImGui::Button(l, s); ImGui::PopStyleColor(3); return r;
}
static void card(ImDrawList* d, ImVec2 p, ImVec2 s, float f, const char* lab, const string& v, ImU32 col) {
    d->AddRectFilled(p, {p.x + s.x, p.y + s.y}, IM_COL32(23, 26, 33, 255), f * 0.3f);
    d->AddRectFilled(p, {p.x + s.x, p.y + f * 0.15f}, col, f * 0.3f);
    d->AddText({p.x + f * 0.7f, p.y + f * 0.6f}, IM_COL32(143, 152, 160, 255), lab);
    d->AddText(ImGui::GetFont(), f * 1.9f, {p.x + f * 0.7f, p.y + f * 1.9f}, col, v.c_str());
}
static void toggleHost(int id, std::atomic<bool>& on) {
    Env v;
    if (on) { v.e->CallStaticVoidMethod(g_bridge, m_stop, g_act, (jint)id); on = false; return; }
    if (!v.e->CallStaticBooleanMethod(g_bridge, m_canOv, g_act)) { v.e->CallStaticVoidMethod(g_bridge, m_askOv, g_act); return; }
    v.e->CallStaticVoidMethod(g_bridge, m_start, g_act, (jint)id); on = true;
}
static void selectApp(const string& pkg) {
    std::lock_guard<std::mutex> l(g_mx); g_selPkg = pkg; g_target = pkg; g_layer.clear(); g_fps = -1;
}

// Daftar game vertikal (turun ke bawah): geser atas/bawah, item terpilih di tengah dengan glow + animasi
static void vlist(float f, float cw, float chh, std::vector<App>& lst) {
    ImGuiIO& io = ImGui::GetIO(); int n = (int)lst.size();
    static float pos = 0, travel = 0, lastY = 0;
    string sel = selPkg(); int selIdx = -1;
    for (int i = 0; i < n; i++) if (lst[i].pkg == sel) selIdx = i;
    if (selIdx < 0 && n > 0) { selIdx = 0; selectApp(lst[0].pkg); sel = lst[0].pkg; }
    float spacing = f * 4.6f, isz = f * 3.4f;
    ImVec2 p = ImGui::GetCursorScreenPos(); float cy = p.y + chh * 0.5f;
    ImGui::InvisibleButton("vl", ImVec2(cw, chh));
    bool act = ImGui::IsItemActive();
    if (ImGui::IsItemActivated()) travel = 0;
    if (act && io.MousePos.y > -1e8f) { pos -= io.MouseDelta.y / spacing; travel += fabsf(io.MouseDelta.y); lastY = io.MousePos.y; }
    if (ImGui::IsItemDeactivated() && n > 0) {
        int ti = travel < f * 0.8f ? (int)lroundf(pos + (lastY - cy) / spacing) : (int)lroundf(pos);
        ti = std::max(0, std::min(n - 1, ti)); if (lst[ti].pkg != sel) selectApp(lst[ti].pkg); selIdx = ti;
    }
    if (!act && n > 0) pos += (selIdx - pos) * std::min(1.f, io.DeltaTime * 12.f);
    pos = std::max(-0.4f, std::min(n - 0.6f, pos));
    ImDrawList* d = ImGui::GetWindowDrawList(); d->PushClipRect(p, {p.x + cw, p.y + chh}, true); double tm = ImGui::GetTime();
    if (n == 0) d->AddText({p.x + f * 0.5f, cy - f}, IM_COL32(143, 152, 160, 255), "Tidak ada game. Aktifkan\n'Tampilkan semua app' di Settings.");
    for (int i = 0; i < n; i++) {
        float dd = i - pos, ad = fabsf(dd); if (ad * spacing > chh * 0.5f + spacing) continue;
        float k = std::min(1.f, ad * 0.7f), al = 1.f - 0.5f * k, sc = 1.f + 0.06f * (1.f - k), y = cy + dd * spacing, rowH = spacing * 0.9f * sc;
        float x0 = p.x + f * 0.3f + k * f * 0.8f, x1 = p.x + cw - f * 0.3f;
        ImVec2 a0(x0, y - rowH / 2), a1(x1, y + rowH / 2); Icon& ic = iconFor(lst[i].pkg); float rr = f * 0.9f;
        if (ad < 0.5f) {
            float pulse = 0.5f + 0.5f * sinf((float)tm * 3.f), ga = (1.f - ad * 2.f) * (0.28f + 0.22f * pulse), gw = f * (0.35f + 0.3f * pulse);
            d->AddRectFilled({a0.x - gw, a0.y - gw}, {a1.x + gw, a1.y + gw}, IM_COL32(102, 192, 244, (int)(ga * 255)), rr + gw);
        }
        d->AddRectFilled(a0, a1, IM_COL32((int)(ic.r * 100), (int)(ic.g * 100), (int)(ic.b * 100), (int)(al * 255)), rr);
        float is = isz * sc, ix = a0.x + (rowH - is) * 0.5f, iy = y - is * 0.5f;
        if (ic.tex) d->AddImageRounded((ImTextureID)(intptr_t)ic.tex, {ix, iy}, {ix + is, iy + is}, {0, 0}, {1, 1}, IM_COL32(255, 255, 255, (int)(al * 255)), rr * 0.7f);
        else { char c1[2] = {lst[i].label.empty() ? '?' : lst[i].label[0], 0}; d->AddText(ImGui::GetFont(), is * 0.5f, {ix + is * 0.3f, iy + is * 0.2f}, IM_COL32(255, 255, 255, (int)(al * 255)), c1); }
        float tx = ix + is + f * 0.8f; ImVec4 clip(tx, a0.y, x1 - f * 0.4f, a1.y);
        d->AddText(ImGui::GetFont(), f * 1.15f, {tx, y - f * 0.95f}, IM_COL32(235, 240, 245, (int)(al * 255)), lst[i].label.c_str(), nullptr, 0.0f, &clip);
        d->AddText(ImGui::GetFont(), f * 0.8f, {tx, y + f * 0.35f}, IM_COL32(143, 152, 160, (int)(al * 255)), lst[i].pkg.c_str(), nullptr, 0.0f, &clip);
        if (ad < 0.5f) d->AddRect(a0, a1, IM_COL32(255, 255, 255, (int)((1.f - ad * 2.f) * 210)), rr, 0, 3.f);
    }
    d->PopClipRect();
}

// Info game: cover + data online (Google Play) + data lokal + waktu bermain
static void gameInfo(float f, const string& sel) {
    requestInfo(sel); Info in; { std::lock_guard<std::mutex> l(g_imx); auto it = g_info.find(sel); if (it != g_info.end()) in = it->second; }
    double ses = 0, tot = 0; { std::lock_guard<std::mutex> l(g_mx); getPlayNL(sel, ses, tot); }
    ImVec2 cp = ImGui::GetCursorScreenPos(); float cvw = f * 9.f, cvh = f * 5.2f; bool hasCover = false;
    auto ci = g_covers.find(sel);
    if (ci != g_covers.end() && ci->second.tex) {
        hasCover = true; float k = std::min(cvw / ci->second.w, cvh / ci->second.h), w = ci->second.w * k, h = ci->second.h * k;
        ImGui::GetWindowDrawList()->AddImageRounded((ImTextureID)(intptr_t)ci->second.tex, {cp.x, cp.y}, {cp.x + w, cp.y + h}, {0, 0}, {1, 1}, IM_COL32_WHITE, f * 0.4f);
    }
    ImGui::SetCursorScreenPos({cp.x + (hasCover ? cvw + f : 0), cp.y}); ImGui::BeginGroup();
    if (in.state == 1) ImGui::TextColored(C(255, 200, 40), "Mengambil info game...");
    if (!in.dev.empty()) ImGui::TextColored(C(143, 152, 160), "Developer  %s", in.dev.c_str());
    if (!in.rating.empty()) ImGui::TextColored(C(143, 152, 160), "Rating  %s  (%s)   Genre  %s", in.rating.c_str(), in.count.c_str(), in.genre.c_str());
    if (!in.ver.empty()) ImGui::TextColored(C(143, 152, 160), "Versi %s  |  Terpasang %s  |  Update %s", in.ver.c_str(), fmtDate(in.inst).c_str(), fmtDate(in.upd).c_str());
    if (in.size > 0) ImGui::TextColored(C(143, 152, 160), "Ukuran APK  %.0f MB", in.size / 1048576.0);
    ImGui::TextColored(ses > 0 ? C(164, 208, 7) : C(143, 152, 160), "SESI  %s    TOTAL  %s", ses > 0 ? fmtHMS(ses).c_str() : "--:--:--", fmtTotal(tot).c_str());
    ImGui::EndGroup();
    float used = std::max(ImGui::GetCursorScreenPos().y - cp.y, hasCover ? cvh : 0.f); ImGui::SetCursorScreenPos({cp.x, cp.y + used + f * 0.3f});
    if (in.state == 2 && !in.online) ImGui::TextColored(C(255, 200, 40), "Info online tidak tersedia (cek koneksi).");
    if (!in.desc.empty()) ImGui::TextWrapped("%s", in.desc.c_str());
}

static void mainUi(Ctx& x) {
    ImGuiIO& io = ImGui::GetIO(); float W = io.DisplaySize.x, H = io.DisplaySize.y, f = x.f; g_iconBudget = 2; uploadCovers();
    float L = (float)g_insL, Rr = (float)g_insR, T = (float)g_insT, CW = W - L - Rr;
    float top = T + f * 2.6f, bot = f * 1.8f; int st = g_sh, tab = g_tab;
    ImDrawList* bg = ImGui::GetBackgroundDrawList(); ImFont* ft = ImGui::GetFont();
    bg->AddRectFilled({0, 0}, {W, H}, IM_COL32(27, 40, 56, 255));
    bg->AddRectFilled({0, 0}, {W, top}, IM_COL32(23, 26, 33, 255));
    bg->AddRectFilled({0, H - bot}, {W, H}, IM_COL32(23, 26, 33, 255));
    float ty = T + (f * 2.6f - f) * 0.5f;
    bg->AddText(ft, f * 1.5f, {L + f, T + (f * 2.6f - f * 1.5f) * 0.5f}, IM_COL32(255, 255, 255, 255), "RESTEAM");
    { float rx = W - Rr - f;   // pengganti status bar: jam, baterai, jaringan
      auto rt = [&](const string& s, ImU32 c) { float w = ImGui::CalcTextSize(s.c_str()).x; rx -= w; bg->AddText({rx, ty}, c, s.c_str()); rx -= f * 1.2f; };
      rt(clockText(), IM_COL32(255, 255, 255, 255)); rt(batText(), colBat(g_bat, g_chg));
      string net; { std::lock_guard<std::mutex> l(g_mx); net = g_net; } rt(net, IM_COL32(102, 192, 244, 255)); }
    char b[96]; snprintf(b, sizeof b, "Resteam v1.7  |  ping %s ms", fv(g_ping, "").c_str());
    bg->AddText({L + f, H - bot * 0.5f - f * 0.5f}, IM_COL32(143, 152, 160, 255), b);
    const char* chip = st == 2 ? "SHIZUKU: ON" : st == 1 ? "SHIZUKU: IZIN" : "SHIZUKU: OFF";
    bg->AddText({W - Rr - f - ImGui::CalcTextSize(chip).x, H - bot * 0.5f - f * 0.5f}, st == 2 ? IM_COL32(164, 208, 7, 255) : IM_COL32(255, 200, 40, 255), chip);

    ImGuiWindowFlags fl = ImGuiWindowFlags_NoDecoration | ImGuiWindowFlags_NoMove | ImGuiWindowFlags_NoSavedSettings |
                          ImGuiWindowFlags_NoBackground | ImGuiWindowFlags_NoBringToFrontOnFocus;
    ImGui::SetNextWindowPos({L + f * 10, T}); ImGui::SetNextWindowSize({f * 30, f * 2.6f});
    ImGui::Begin("tabs", 0, fl | ImGuiWindowFlags_NoScrollbar);
    ImGui::SetCursorPos({0, (f * 2.6f - f * 2) * 0.5f});
    static const char* TN[4] = {"LIBRARY", "HISTORY", "SETTINGS", "ABOUT"};
    for (int i = 0; i < 4; i++) { if (i) ImGui::SameLine(); if (Btn((i == 3 && g_updAvail) ? "ABOUT (!)" : TN[i], tab == i ? C(26, 159, 255) : C(46, 60, 76), ImVec2(f * 6.8f, f * 2))) g_tab = i; }
    ImGui::End();

    if (tab == 1) {  // ---- HISTORY: riwayat sesi ----
        std::vector<Sess> hs; Sess cur; string act; { std::lock_guard<std::mutex> l(g_mx); hs = g_hist; cur = g_cur; act = g_active; }
        static int selH = -1; float lw = CW * 0.46f;
        ImGui::SetNextWindowPos({L + f, top + f * 0.5f}); ImGui::SetNextWindowSize({lw - f, H - top - bot - f});
        ImGui::Begin("histL", 0, fl);
        ImGui::SetWindowFontScale(1.4f); ImGui::TextUnformatted("RIWAYAT SESI"); ImGui::SetWindowFontScale(1.f);
        if (!act.empty()) ImGui::TextColored(C(164, 208, 7), "Sedang main: %s  %s", cur.label.c_str(), fmtHMS(cur.dur).c_str());
        if (hs.empty()) ImGui::TextColored(C(143, 152, 160), "Belum ada sesi. Main game minimal 30 detik.");
        for (int i = (int)hs.size() - 1; i >= 0; i--) {
            const Sess& s = hs[i]; ImGui::PushID(i); ImVec2 p = ImGui::GetCursorScreenPos();
            if (ImGui::Selectable("##r", selH == i, 0, ImVec2(0, f * 3.6f))) selH = i;
            ImDrawList* d = ImGui::GetWindowDrawList(); char b[120];
            d->AddText({p.x + f * 0.5f, p.y + f * 0.35f}, IM_COL32(235, 240, 245, 255), s.label.c_str());
            snprintf(b, sizeof b, "%s  |  %s  |  FPS %s", fmtDateTime(s.start).c_str(), fmtHMS(s.dur).c_str(), fv(s.avgFps, "").c_str());
            d->AddText({p.x + f * 0.5f, p.y + f * 1.9f}, IM_COL32(143, 152, 160, 255), b);
            ImGui::PopID();
        }
        ImGui::End();
        ImGui::SetNextWindowPos({L + lw, top + f * 0.5f}); ImGui::SetNextWindowSize({CW - lw - f, H - top - bot - f});
        ImGui::Begin("histR", 0, fl);
        if (selH >= 0 && selH < (int)hs.size()) {
            const Sess& s = hs[selH];
            ImGui::SetWindowFontScale(1.4f); ImGui::TextUnformatted(s.label.c_str()); ImGui::SetWindowFontScale(1.f);
            ImGui::TextColored(C(143, 152, 160), "Mulai %s    Durasi %s", fmtDateTime(s.start).c_str(), fmtHMS(s.dur).c_str());
            ImGui::Text("FPS rata-rata %s   minimum %s", fv(s.avgFps, "").c_str(), fv(s.minFps, "").c_str());
            ImGui::Text("GPU rata-rata %s   CPU rata-rata %s", fv(s.avgGpu, "%").c_str(), fv(s.avgCpu, "%").c_str());
            ImGui::Text("Suhu maks %.1fC   Baterai %d%% -> %d%%", s.maxTemp, s.batS, s.batE);
            ImGui::Dummy({0, f * 0.3f}); ImGui::TextColored(C(164, 208, 7), "FPS");
            ImGui::PushStyleColor(ImGuiCol_PlotLines, C(164, 208, 7));
            if (s.fps.size() > 1) ImGui::PlotLines("##pf", s.fps.data(), (int)s.fps.size(), 0, nullptr, 0.f, FLT_MAX, ImVec2(-1, f * 7)); else ImGui::TextColored(C(143, 152, 160), "(data grafik belum cukup)");
            ImGui::PopStyleColor();
            ImGui::TextColored(C(255, 170, 30), "SUHU (C)");
            ImGui::PushStyleColor(ImGuiCol_PlotLines, C(255, 170, 30));
            if (s.temp.size() > 1) ImGui::PlotLines("##pt", s.temp.data(), (int)s.temp.size(), 0, nullptr, FLT_MAX, FLT_MAX, ImVec2(-1, f * 5)); else ImGui::TextColored(C(143, 152, 160), "(data grafik belum cukup)");
            ImGui::PopStyleColor();
        } else ImGui::TextColored(C(143, 152, 160), "Pilih sesi di kiri.");
        ImGui::Dummy({0, f * 0.5f});
        if (!hs.empty() && Btn("HAPUS SEMUA RIWAYAT", C(160, 60, 50), ImVec2(-1, f * 2.2f))) { std::lock_guard<std::mutex> l(g_mx); g_hist.clear(); saveHistNL(); selH = -1; }
        ImGui::End(); return;
    }

    if (tab == 3) {  // ---- ABOUT ----
        ImGui::SetNextWindowPos({L + f, top + f * 0.5f}); ImGui::SetNextWindowSize({CW - 2 * f, H - top - bot - f});
        ImGui::Begin("about", 0, fl);
        Icon& ic = iconFor("com.udyneos.resteam"); ImVec2 p = ImGui::GetCursorScreenPos(); float is = f * 6.f;
        if (ic.tex) ImGui::GetWindowDrawList()->AddImageRounded((ImTextureID)(intptr_t)ic.tex, p, {p.x + is, p.y + is}, {0, 0}, {1, 1}, IM_COL32_WHITE, f * 1.2f);
        else ImGui::GetWindowDrawList()->AddRectFilled(p, {p.x + is, p.y + is}, IM_COL32(61, 77, 92, 255), f * 1.2f);
        ImGui::SetCursorScreenPos({p.x + is + f, p.y});
        ImGui::BeginGroup();
        ImGui::SetWindowFontScale(2.2f); ImGui::TextUnformatted("Resteam"); ImGui::SetWindowFontScale(1.f);
        ImGui::TextColored(C(102, 192, 244), "Game Space untuk Android  |  v1.7");
        ImGui::TextColored(C(143, 152, 160), "Package  com.udyneos.resteam");
        ImGui::TextColored(C(143, 152, 160), "Dibangun dengan C++17 (NDK), Dear ImGui, dan Shizuku");
        ImGui::EndGroup();
        ImGui::SetCursorScreenPos({p.x, p.y + is + f});
        { Upd u; { std::lock_guard<std::mutex> l(g_umx); u = g_upd; }
          ImGui::TextColored(C(102, 192, 244), "PEMBARUAN  (GitHub: %s)", GITHUB_REPO);
          ImGui::Text("Terpasang  %s", g_curVer.c_str());
          if (u.state == 0) ImGui::TextColored(C(143, 152, 160), "Belum dicek.");
          if (u.state == 1) ImGui::TextColored(C(255, 200, 40), "Memeriksa rilis terbaru...");
          if (u.state == 2) ImGui::TextColored(C(164, 208, 7), "Sudah versi terbaru (rilis %s).", u.latest.c_str());
          if (u.state == 3) { ImGui::TextColored(C(164, 208, 7), "Tersedia versi baru: %s", u.latest.c_str()); if (!u.notes.empty()) ImGui::TextWrapped("%s", u.notes.c_str()); }
          if (u.state == 4) { float pc = std::max(0.f, std::min(100.f, (float)g_dlPct)); char pb[16]; snprintf(pb, sizeof pb, "%.0f%%", pc); ImGui::ProgressBar(pc / 100.f, ImVec2(-1, f * 1.6f), pb); }
          if (u.state == 5) ImGui::TextColored(C(255, 200, 40), "Memasang... aplikasi akan restart.");
          if (u.state == 6) ImGui::TextColored(C(230, 70, 60), "%s", u.err.c_str());
          ImGui::BeginDisabled(u.state == 1 || u.state == 4 || u.state == 5);
          if (Btn("CEK UPDATE", C(61, 77, 92), ImVec2(f * 9, f * 2.2f))) checkUpdate();
          ImGui::EndDisabled();
          if (u.state == 3) { ImGui::SameLine(); if (Btn("UNDUH & PASANG", C(91, 163, 43), ImVec2(f * 11, f * 2.2f))) downloadUpdate(); }
          ImGui::Dummy({0, f * 0.3f}); }
        ImGui::TextColored(C(102, 192, 244), "FITUR");
        ImGui::BulletText("Library game vertikal + info game online (Google Play) dan waktu bermain");
        ImGui::BulletText("Mode performa (Balanced / Performance / Battery) + tweak yang bisa diatur");
        ImGui::BulletText("Auto-switch mode dan resolusi layar per game, riwayat sesi dengan grafik FPS");
        ImGui::BulletText("Floating bubble: FPS live, switch mode, tombol Settings, dan daftar app untuk PIP; update dari GitHub");
        ImGui::Dummy({0, f * 0.3f}); ImGui::TextColored(C(102, 192, 244), "CATATAN VERSI TERBARU");
        ImGui::BulletText("v1.7  Bubble tampil FPS + tombol Settings, update GitHub, kunci rotasi, overlay statistik dihapus");
        ImGui::BulletText("v1.6  Auto-switch + resolusi per game, riwayat sesi, tab About, build GitHub Actions");
        ImGui::BulletText("v1.5  Switch performa di bubble, library vertikal, tweak performa lebih banyak");
        ImGui::BulletText("v1.4  Fetch info game, waktu bermain, bubble melayang, metode FPS berlapis");
        ImGui::Dummy({0, f * 0.3f}); ImGui::TextColored(C(102, 192, 244), "DATA LOKAL");
        ImGui::TextColored(C(143, 152, 160), "%s", g_dir.c_str());
        ImGui::TextColored(C(143, 152, 160), "pinned.txt  playtime.txt  tweaks.txt  profiles.txt  sessions.txt");
        ImGui::Dummy({0, f * 0.3f}); ImGui::TextColored(C(143, 152, 160), "Butuh Shizuku aktif untuk statistik, mode performa, resolusi, dan launch PIP.");
        ImGui::End(); return;
    }

    if (tab == 2) {  // ---- SETTINGS: tweak performa + pilih app untuk bubble ----
        ImGui::SetNextWindowPos({L + f, top + f * 0.5f}); ImGui::SetNextWindowSize({CW - 2 * f, H - top - bot - f});
        ImGui::Begin("set", 0, fl);
        ImGui::SetWindowFontScale(1.4f); ImGui::TextUnformatted("TWEAK & OTOMATISASI"); ImGui::SetWindowFontScale(1.f);
        ImGui::TextColored(C(143, 152, 160), "Dijalankan lewat Shizuku saat PERFORMANCE dipilih, dibalik saat BALANCED.");
        for (int i = 0; i < 7; i++) { bool v = g_tw[i]; if (ImGui::Checkbox(TW_NAME[i], &v)) { g_tw[i] = v; saveTweaks(); } }
        ImGui::TextColored(C(143, 152, 160), "Selalu aktif: fixed/sustained performance, game mode performance, matikan battery saver.");
        ImGui::Separator();
        ImGui::SetWindowFontScale(1.4f); ImGui::TextUnformatted("ROTASI APLIKASI"); ImGui::SetWindowFontScale(1.f);
        { static const char* ON[3] = {"AUTO LANDSCAPE", "LANDSCAPE TETAP", "KUNCI SAAT INI"};
          float bwo = (ImGui::GetContentRegionAvail().x - 2 * ImGui::GetStyle().ItemSpacing.x) / 3;
          for (int i = 0; i < 3; i++) { if (i) ImGui::SameLine(); if (Btn(ON[i], g_orient == i ? C(26, 159, 255) : C(61, 77, 92), ImVec2(bwo, f * 2.2f))) { g_orient = i; saveOrient(); applyOrient(); } }
          ImGui::TextColored(C(143, 152, 160), "Auto: ikut sensor (kiri/kanan). Tetap: satu arah. Kunci: mengunci orientasi layar saat ini."); }
        ImGui::Separator();
        ImGui::SetWindowFontScale(1.4f); ImGui::TextUnformatted("SIDEBAR APPS"); ImGui::SetWindowFontScale(1.f);
        ImGui::TextColored(C(143, 152, 160), "Centang app yang muncul di bubble. Tap app di bubble = jalan dalam PIP.");
        bool sa = g_showAll; if (ImGui::Checkbox("Tampilkan semua app di library (bukan hanya game)", &sa)) g_showAll = sa;
        if (Btn("REFRESH LIST", C(61, 77, 92), ImVec2(f * 10, f * 2))) refreshPkgs();
        ImGui::Dummy({0, f * 0.3f});
        { std::lock_guard<std::mutex> l(g_mx);
          if (g_apps.empty()) ImGui::TextColored(C(255, 200, 40), "Memuat daftar app...");
          for (auto& a : g_apps) {
              bool on = inList(g_pinned, a.pkg); string lb = a.label + "##" + a.pkg;
              if (ImGui::Checkbox(lb.c_str(), &on)) { if (on) g_pinned.push_back(a.pkg); else g_pinned.erase(std::find(g_pinned.begin(), g_pinned.end(), a.pkg)); saveListNL("pinned.txt", g_pinned); }
          } }
        ImGui::End(); return;
    }

    // ---- LIBRARY: kiri = daftar game vertikal, tengah = info game, kanan = kontrol ----
    float c1 = CW * 0.30f, c2 = CW * 0.34f;
    std::vector<App> lst; { std::lock_guard<std::mutex> l(g_mx); for (auto& a : g_apps) if (g_showAll || a.game) lst.push_back(a); }
    ImGui::SetNextWindowPos({L, top}); ImGui::SetNextWindowSize({c1, H - top - bot});
    ImGui::Begin("libL", 0, fl | ImGuiWindowFlags_NoScrollbar | ImGuiWindowFlags_NoScrollWithMouse);
    vlist(f, ImGui::GetContentRegionAvail().x, ImGui::GetContentRegionAvail().y - f * 0.3f, lst);
    ImGui::End();
    string sel = selPkg(), title; bool has = false; for (auto& a : lst) if (a.pkg == sel) { has = true; title = a.label; }

    ImGui::SetNextWindowPos({L + c1, top + f * 0.5f}); ImGui::SetNextWindowSize({c2 - f * 0.5f, H - top - bot - f});
    ImGui::Begin("libM", 0, fl);
    static string lastSel; static float tA = 0; if (sel != lastSel) { lastSel = sel; tA = 0; } tA = std::min(1.f, tA + io.DeltaTime * 5.f);
    ImGui::PushStyleVar(ImGuiStyleVar_Alpha, tA);
    ImGui::SetWindowFontScale(1.6f); ImGui::TextWrapped("%s", has ? title.c_str() : "Pilih game"); ImGui::SetWindowFontScale(1.f);
    if (has) ImGui::TextColored(C(143, 152, 160), "%s", sel.c_str());
    ImGui::PopStyleVar();
    if (has) {  // profil per game: dipakai saat auto-switch
        Prof pf; { std::lock_guard<std::mutex> l(g_mx); auto it = g_prof.find(sel); if (it != g_prof.end()) pf = it->second; }
        bool ch = false; float av2 = ImGui::GetContentRegionAvail().x, sp2 = ImGui::GetStyle().ItemSpacing.x;
        ImGui::TextColored(C(143, 152, 160), g_tw[5] ? "PROFIL GAME (auto-switch aktif)" : "PROFIL GAME (auto-switch mati, lihat Settings)");
        static const int RES[4] = {100, 85, 75, 67}; static const char* RN[4] = {"NATIVE", "85%", "75%", "720P"};
        float bw4 = (av2 - 3 * sp2) / 4;
        for (int i = 0; i < 4; i++) { if (i) ImGui::SameLine(); if (Btn(RN[i], pf.res == RES[i] ? C(26, 159, 255) : C(61, 77, 92), ImVec2(bw4, f * 2.f))) { pf.res = RES[i]; ch = true; } }
        static const char* MN2[3] = {"BAL", "PERF", "BAT"}; float bw3 = (av2 - 2 * sp2) / 3;
        for (int i = 0; i < 3; i++) { if (i) ImGui::SameLine(); if (Btn(MN2[i], pf.mode == i ? C(26, 159, 255) : C(61, 77, 92), ImVec2(bw3, f * 2.f))) { pf.mode = i; ch = true; } }
        ImGui::TextColored(C(143, 152, 160), "Resolusi (baris atas) dan mode (baris bawah) saat game berjalan");
        if (ch) { std::lock_guard<std::mutex> l(g_mx); g_prof[sel] = pf; saveProfNL(); }
        ImGui::Dummy({0, f * 0.3f});
    }
    if (has) gameInfo(f, sel);
    ImGui::End();

    ImGui::SetNextWindowPos({L + c1 + c2, top + f * 0.5f}); ImGui::SetNextWindowSize({CW - c1 - c2 - f, H - top - bot - f});
    ImGui::Begin("libR", 0, fl);
    if (st == 0) ImGui::TextColored(C(255, 200, 40), "Jalankan Shizuku dulu (start service).");
    if (st == 1 && Btn("IZINKAN SHIZUKU", C(255, 160, 30), ImVec2(-1, f * 2.2f))) { Env v; v.e->CallStaticVoidMethod(g_bridge, m_req); }
    ImGui::BeginDisabled(!has || st != 2);
    if (Btn("LAUNCH", C(91, 163, 43), ImVec2(-1, f * 3.f))) launchPkg(sel, false);
    ImGui::EndDisabled();
    float av = ImGui::GetContentRegionAvail().x, sp = ImGui::GetStyle().ItemSpacing.x, bw = (av - 2 * sp) / 3;
    ImGui::TextColored(C(143, 152, 160), "PERFORMANCE MODE");
    static const char* mn[] = {"BALANCED", "PERFORMANCE", "BATTERY"};
    ImGui::BeginDisabled(st != 2);
    for (int i = 0; i < 3; i++) { if (i) ImGui::SameLine(); if (Btn(mn[i], g_mode == i ? C(26, 159, 255) : C(61, 77, 92), ImVec2(bw, f * 2.2f))) setMode(i); }
    ImGui::EndDisabled();
    float bw2 = (av - sp) / 2;
    if (Btn(g_sideOn ? "BUBBLE: ON" : "BUBBLE: OFF", g_sideOn ? C(91, 163, 43) : C(61, 77, 92), ImVec2(bw2, f * 2.2f))) toggleHost(1, g_sideOn);
    ImGui::SameLine();
    if (Btn("MINIMIZE", C(61, 77, 92), ImVec2(bw2, f * 2.2f))) { Env v; v.e->CallStaticVoidMethod(g_bridge, m_pip, g_act); }
    ImGui::Dummy({0, f * 0.4f});
    ImVec2 p = ImGui::GetCursorScreenPos(); float gap = f * 0.6f, cw2 = (ImGui::GetContentRegionAvail().x - gap) / 2, ch = f * 4.6f;
    ImDrawList* d = ImGui::GetWindowDrawList();
    card(d, p, {cw2, ch}, f, fpsLabel(), fv(g_fps, ""), IM_COL32(164, 208, 7, 255));
    card(d, {p.x + cw2 + gap, p.y}, {cw2, ch}, f, "GPU LOAD", fv(g_gpu, "%"), IM_COL32(102, 192, 244, 255));
    card(d, {p.x, p.y + ch + gap}, {cw2, ch}, f, "CPU", fv(g_cpu, "%"), IM_COL32(102, 192, 244, 255));
    card(d, {p.x + cw2 + gap, p.y + ch + gap}, {cw2, ch}, f, "PING (ms)", fv(g_ping, ""), colPing(g_ping));
    ImGui::Dummy({0, 2 * ch + gap});
    ImGui::End();
}

static Ctx g_main; static bool g_hasWin = false;
static void immersive() { Env v; v.e->CallStaticVoidMethod(g_bridge, m_imm, g_act); }
static void onCmd(android_app* a, int32_t cmd) {
    if (cmd == APP_CMD_INIT_WINDOW && a->window) {
        g_icons.clear(); g_covers.clear();  // tekstur lama mati bersama konteks GL lama
        g_hasWin = openCtx(g_main, a->window, std::min(ANativeWindow_getWidth(a->window), ANativeWindow_getHeight(a->window)) / 32.f);
        if (g_hasWin) { ImGuiStyle& s = ImGui::GetStyle(); float f = g_main.f;
            s.FrameRounding = f * 0.2f; s.WindowRounding = 0; s.ScrollbarSize = f * 0.4f;
            s.ItemSpacing = ImVec2(f * 0.6f, f * 0.6f); s.FramePadding = ImVec2(f * 0.8f, f * 0.5f);
            s.Colors[ImGuiCol_Header] = C(61, 77, 92); s.Colors[ImGuiCol_HeaderHovered] = C(61, 77, 92);
            s.Colors[ImGuiCol_HeaderActive] = C(102, 192, 244, 120); s.Colors[ImGuiCol_Text] = C(235, 240, 245); }
        immersive();
    } else if (cmd == APP_CMD_GAINED_FOCUS) immersive();
    else if (cmd == APP_CMD_TERM_WINDOW) { g_hasWin = false; closeCtx(g_main); }
}
static int32_t onInput(android_app*, AInputEvent* e) {
    if (AInputEvent_getType(e) != AINPUT_EVENT_TYPE_MOTION || !g_main.im) return 0;
    ImGui::SetCurrentContext(g_main.im); ImGuiIO& io = ImGui::GetIO();
    int act = AMotionEvent_getAction(e) & AMOTION_EVENT_ACTION_MASK;
    float x = AMotionEvent_getX(e, 0), y = AMotionEvent_getY(e, 0), f = g_main.f;
    static float sx, sy, ly; static bool dragV;
    io.AddMouseSourceEvent(ImGuiMouseSource_TouchScreen);
    if (act == AMOTION_EVENT_ACTION_DOWN) {
        dragV = false; sx = x; sy = ly = y; io.AddMousePosEvent(x, y); io.AddMouseButtonEvent(0, true);
    } else if (act == AMOTION_EVENT_ACTION_MOVE) {
        // geser dominan vertikal = scroll halaman; dominan horizontal = dipakai carousel
        if (!dragV && fabsf(y - sy) > f * 0.8f && fabsf(y - sy) > fabsf(x - sx)) dragV = true;
        io.AddMousePosEvent(x, y);
        if (dragV) { io.AddMouseWheelEvent(0, (y - ly) / (2 * f)); ly = y; }
    } else if (act == AMOTION_EVENT_ACTION_UP || act == AMOTION_EVENT_ACTION_CANCEL) {
        if (dragV) io.AddMousePosEvent(-FLT_MAX, -FLT_MAX); else io.AddMousePosEvent(x, y);
        io.AddMouseButtonEvent(0, false); io.AddMousePosEvent(-FLT_MAX, -FLT_MAX);
    }
    return 1;
}

void android_main(android_app* app) {
    app->onAppCmd = onCmd; app->onInputEvent = onInput;
    { Env v; g_act = v.e->NewGlobalRef(app->activity->clazz); }
    g_dir = app->activity->internalDataPath ? app->activity->internalDataPath : "/data/local/tmp";
    loadList("pinned.txt", g_pinned); loadPlay(); loadTweaks(); loadProf(); loadHist(); loadOrient(); g_curVer = curVersion(); applyOrient();
    g_run = true; std::thread(statsLoop).detach();
    while (!app->destroyRequested) {
        int ev; android_poll_source* src;
        while (ALooper_pollOnce(g_hasWin ? 0 : -1, nullptr, &ev, (void**)&src) >= 0) {
            if (src) src->process(app, src);
            if (app->destroyRequested) break;
        }
        if (g_hasWin && g_main.im) render(g_main, mainUi, ImVec4(0.1f, 0.15f, 0.22f, 1));
    }
    g_run = false; endSession(false); { std::lock_guard<std::mutex> l(g_mx); savePlayNL(); } closeCtx(g_main);
}
