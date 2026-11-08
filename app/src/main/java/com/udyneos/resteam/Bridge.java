package com.udyneos.resteam;
import android.app.*;
import android.content.*;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.net.*;
import android.net.wifi.*;
import android.os.Build;
import android.provider.Settings;
import android.util.Rational;
import android.view.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.lang.reflect.Method;
import rikka.shizuku.Shizuku;

/** Shim tipis: semua logika ada di C++. */
public class Bridge {
    public static int shizukuState() { // 0 mati, 1 butuh izin, 2 siap
        try {
            if (!Shizuku.pingBinder()) return 0;
            return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED ? 2 : 1;
        } catch (Throwable t) { return 0; }
    }
    public static void shizukuRequest() { try { Shizuku.requestPermission(1); } catch (Throwable t) {} }

    public static String exec(String cmd) {
        try {
            Method m = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
            m.setAccessible(true);
            Process p = (Process) m.invoke(null, new String[]{"sh", "-c", "(" + cmd + ") 2>&1"}, null, null);
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder(); String l;
            while ((l = r.readLine()) != null) sb.append(l).append('\n');
            p.waitFor();
            return sb.toString();
        } catch (Throwable t) { return ""; }
    }
    public static boolean canOverlay(Context c) { return Settings.canDrawOverlays(c); }
    public static void askOverlay(Context c) {
        Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + c.getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); c.startActivity(i);
    }
    public static void startOverlay(Context c, int id) { c.startService(new Intent(c, id == 0 ? OverlayService.class : SideService.class)); }
    public static void stopOverlay(Context c, int id) { c.stopService(new Intent(c, id == 0 ? OverlayService.class : SideService.class)); }
    public static void enterPip(final Activity a) {
        a.runOnUiThread(() -> a.enterPictureInPictureMode(
            new PictureInPictureParams.Builder().setAspectRatio(new Rational(16, 9)).build()));
    }

    /** Immersive penuh: sembunyikan status bar + navigasi, gambar sampai area notch. */
    public static void immersive(final Activity a) {
        a.runOnUiThread(() -> {
            try {
                Window w = a.getWindow();
                WindowManager.LayoutParams lp = w.getAttributes();
                lp.layoutInDisplayCutoutMode = Build.VERSION.SDK_INT >= 30
                    ? WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    : WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                w.setAttributes(lp);
                if (Build.VERSION.SDK_INT >= 30) {
                    w.setDecorFitsSystemWindows(false);
                    WindowInsetsController c = w.getInsetsController();
                    if (c != null) {
                        c.hide(WindowInsets.Type.systemBars());
                        c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                    }
                } else {
                    w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                }
            } catch (Throwable t) {}
        });
    }
    /** Margin aman notch dalam px: (atas << 20) | (kiri << 10) | kanan, masing-masing 10 bit. */
    public static int insets(Activity a) {
        try {
            WindowInsets wi = a.getWindow().getDecorView().getRootWindowInsets();
            if (wi == null) return 0;
            DisplayCutout dc = wi.getDisplayCutout();
            if (dc == null) return 0;
            return ((dc.getSafeInsetTop() & 0x3FF) << 20) | ((dc.getSafeInsetLeft() & 0x3FF) << 10) | (dc.getSafeInsetRight() & 0x3FF);
        } catch (Throwable t) { return 0; }
    }
    /** HTTP GET -> teks (maks ~400KB). Dipakai C++ untuk fetch info game. */
    public static String fetch(String url) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(6000); c.setReadTimeout(8000);
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36");
            c.setRequestProperty("Accept-Language", "id,en;q=0.8");
            if (c.getResponseCode() != 200) return "";
            InputStream in = c.getInputStream(); ByteArrayOutputStream o = new ByteArrayOutputStream();
            byte[] b = new byte[16384]; int n, tot = 0;
            while ((n = in.read(b)) > 0 && tot < 400000) { o.write(b, 0, n); tot += n; }
            return o.toString("UTF-8");
        } catch (Throwable t) { return ""; }
    }
    /** HTTP GET gambar -> [w(int32 LE), h(int32 LE), RGBA...], sisi terpanjang <= 384. */
    public static byte[] fetchImage(String url) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(6000); c.setReadTimeout(8000);
            if (c.getResponseCode() != 200) return null;
            Bitmap src = BitmapFactory.decodeStream(c.getInputStream());
            if (src == null) return null;
            float k = Math.min(1f, 384f / Math.max(src.getWidth(), src.getHeight()));
            int w = Math.max(1, Math.round(src.getWidth() * k)), h = Math.max(1, Math.round(src.getHeight() * k));
            Bitmap bm = Bitmap.createScaledBitmap(src, w, h, true);
            int[] px = new int[w * h]; bm.getPixels(px, 0, w, 0, 0, w, h);
            byte[] o = new byte[8 + w * h * 4];
            o[0] = (byte) w; o[1] = (byte) (w >> 8); o[4] = (byte) h; o[5] = (byte) (h >> 8);
            for (int i = 0; i < px.length; i++) {
                int p = px[i];
                o[8 + i * 4] = (byte) (p >> 16); o[9 + i * 4] = (byte) (p >> 8); o[10 + i * 4] = (byte) p; o[11 + i * 4] = (byte) (p >>> 24);
            }
            return o;
        } catch (Throwable t) { return null; }
    }
    /** Info lokal app: "versi|terpasang(ms)|update(ms)|ukuranApk(byte)" */
    public static String info(Context c, String pkg) {
        try {
            android.content.pm.PackageInfo pi = c.getPackageManager().getPackageInfo(pkg, 0);
            long size = new File(pi.applicationInfo.sourceDir).length();
            return pi.versionName + "|" + pi.firstInstallTime + "|" + pi.lastUpdateTime + "|" + size;
        } catch (Throwable t) { return ""; }
    }
    /** Ikon app 128x128 RGBA mentah (untuk tekstur GL di C++). */
    public static byte[] icon(Context c, String pkg) {
        try {
            Drawable d = c.getPackageManager().getApplicationIcon(pkg);
            final int S = 128;
            Bitmap bm = Bitmap.createBitmap(S, S, Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(bm); d.setBounds(0, 0, S, S); d.draw(cv);
            int[] px = new int[S * S]; bm.getPixels(px, 0, S, 0, 0, S, S);
            byte[] o = new byte[S * S * 4];
            for (int i = 0; i < px.length; i++) {
                int p = px[i];
                o[i * 4] = (byte) (p >> 16); o[i * 4 + 1] = (byte) (p >> 8); o[i * 4 + 2] = (byte) p; o[i * 4 + 3] = (byte) (p >>> 24);
            }
            return o;
        } catch (Throwable t) { return null; }
    }
    /** Baris: "paket\tlabel\tgame(1/0)"; hanya app non-sistem yang punya launcher. */
    public static String apps(Context c) {
        StringBuilder sb = new StringBuilder();
        try {
            PackageManager pm = c.getPackageManager();
            for (ApplicationInfo ai : pm.getInstalledApplications(0)) {
                boolean sys = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0 && (ai.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0;
                if (sys || ai.packageName.equals(c.getPackageName())) continue;
                if (pm.getLaunchIntentForPackage(ai.packageName) == null) continue;
                boolean game = ai.category == ApplicationInfo.CATEGORY_GAME || (ai.flags & ApplicationInfo.FLAG_IS_GAME) != 0;
                String lb = String.valueOf(pm.getApplicationLabel(ai)).replace('\t', ' ').replace('\n', ' ');
                sb.append(ai.packageName).append('\t').append(lb).append('\t').append(game ? "1" : "0").append('\n');
            }
        } catch (Throwable t) {}
        return sb.toString();
    }
    /** "level|suhuC|charging|jaringan" */
    public static String sysinfo(Context c) {
        String net = "Offline";
        try {
            ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network n = cm.getActiveNetwork();
            NetworkCapabilities nc = n == null ? null : cm.getNetworkCapabilities(n);
            if (nc != null) {
                if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    WifiManager wm = (WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    WifiInfo i = wm.getConnectionInfo();
                    net = "WiFi " + i.getLinkSpeed() + "Mbps " + i.getRssi() + "dBm";
                } else if (nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    net = "Seluler " + (nc.getLinkDownstreamBandwidthKbps() / 1000) + "Mbps";
                } else net = "Online";
            }
        } catch (Throwable t) {}
        int lv = -1, tp = 0, pl = 0;
        try {
            Intent b = c.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (b != null) {
                int sc = b.getIntExtra("scale", 100); if (sc <= 0) sc = 100;
                lv = b.getIntExtra("level", -1) * 100 / sc;
                tp = b.getIntExtra("temperature", 0); pl = b.getIntExtra("plugged", 0);
            }
        } catch (Throwable t) {}
        return lv + "|" + (tp / 10f) + "|" + (pl != 0 ? 1 : 0) + "|" + net;
    }
}
