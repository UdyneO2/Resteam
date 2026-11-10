<p align="center"><img src="art/icon.png" width="96" alt="Resteam"></p>

# Resteam v1.8

Game Space untuk Android (`com.udyneos.resteam`). Logika dan UI ditulis di C++17 (NDK) dengan Dear ImGui; Java hanya shim tipis (load library, Surface overlay, API Shizuku/HTTP). Orientasi landscape.

## Fitur
- **Library**: daftar game vertikal dengan ikon dan animasi, info game online (Google Play) + data lokal, waktu bermain (sesi dan total)
- **Mode performa**: Balanced / Performance / Battery lewat Shizuku, plus tweak yang bisa diatur (refresh rate maksimal, bersihkan app latar, matikan Doze, DND, animasi 0.5x)
- **Auto-switch + resolusi per game**: tiap game punya profil (mode dan resolusi: Native / 85% / 75% / 720P). Saat game berjalan profil dipasang otomatis, saat ditutup kembali ke Balanced dan resolusi normal
- **Riwayat sesi** (tab HISTORY): durasi, FPS rata-rata/minimum, GPU, CPU, suhu maks, baterai, grafik FPS dan suhu
- **Floating bubble**: kecil, menampilkan FPS live (cincin berwarna mengikuti mode), hanya bisa digeser dari bagian atas. Tap untuk membuka panel ala game panel:
  - kolom kiri: CPU, GPU, FPS, ping, waktu main
  - baris app berikon (dari daftar bubble di Settings): tap = jalan dalam PIP
  - shortcut: **Mode** (BAL/PERF/BAT), **Lock FPS** (auto, lalu tiap refresh rate yang didukung layar), **DND**, **Rotasi** (auto / kunci / landscape tetap), **Settings**
  - tombol **RESTEAM** (buka aplikasi) dan **TUTUP**
- **Update dari GitHub** (tab ABOUT): cek rilis terbaru, unduh APK, pasang (senyap via Shizuku, fallback installer sistem). Cek otomatis saat dibuka bisa dimatikan di Settings
- **Rotasi aplikasi** (Settings): auto landscape, landscape tetap, atau kunci rotasi saat ini
- **Immersive penuh** tanpa status bar, navigasi, dan batas notch; jam, baterai, dan jaringan tampil di top bar
- **FPS berlapis**: latency SurfaceFlinger layer game, lalu `measured_fps` display, lalu selisih frame counter SurfaceFlinger. Sumber aktif ditulis di label kartu

## Persyaratan
- Android 10+ (minSdk 29), arm64
- [Shizuku](https://shizuku.rikka.app) aktif dan diizinkan untuk statistik, mode performa, resolusi, dan launch PIP
- Izin tampil di atas app lain (bubble)
- Fitur update: izin "pasang app dari sumber ini" hanya diminta bila tidak memakai Shizuku

## Cara pakai
1. Start Shizuku, buka Resteam, izinkan Shizuku
2. SETTINGS: centang app untuk bubble, atur tweak dan auto-switch
3. LIBRARY: pilih game, atur profil (resolusi + mode), tekan **LAUNCH**
4. Nyalakan BUBBLE bila perlu; tombol SETTINGS di bubble membawa kembali ke aplikasi

Data lokal di folder data app: `pinned.txt`, `playtime.txt`, `tweaks.txt`, `profiles.txt`, `sessions.txt`, `orient.txt`.

## Build lokal
Butuh JDK 17, Android SDK 34, NDK `26.1.10909125`, CMake `3.22.1`, Gradle 8.7, dan internet (CMake mengunduh ImGui v1.91.9).
```
gradle assembleDebug          # app/build/outputs/apk/debug
gradle assembleRelease        # butuh keystore, lihat bawah
```
Versi dibaca dari `gradle.properties` (`appVersionName`, `appVersionCode`) atau `-PappVersionName=... -PappVersionCode=...`.
Repo untuk fitur update diatur lewat `githubRepo` di `gradle.properties` (default `udyneos/resteam`, **ganti dengan repo kamu**); build GitHub Actions mengisinya otomatis dengan `github.repository`.

## Build otomatis (GitHub Actions)
Workflow: `.github/workflows/build.yml`

| Pemicu | Hasil |
| --- | --- |
| push ke `main`, pull request | APK **debug** (artifact, 14 hari) |
| tag `vX.Y.Z` | APK **debug + release**, GitHub Release dengan changelog dan checksum |
| Run workflow manual | pilih `debug`, `release`, atau `both` (artifact) |

Debug dan release **ditandatangani dengan keystore yang sama**, jadi bisa saling menimpa saat install. Di PR dari fork (tanpa secret) debug memakai debug key bawaan.

### Secrets repository
Settings > Secrets and variables > Actions:

| Secret | Isi |
| --- | --- |
| `KEYSTORE_BASE64` | keystore dalam base64: `base64 -w0 resteam.jks` |
| `KEYSTORE_PASSWORD` | password keystore |
| `KEY_ALIAS` | alias key |
| `KEY_PASSWORD` | password key (kosong = sama dengan password keystore) |

Buat keystore jika belum ada:
```
keytool -genkeypair -v -keystore resteam.jks -alias resteam -keyalg RSA -keysize 2048 -validity 10000
```
Simpan keystore yang sama selamanya. Jangan di-commit (`*.jks` sudah ada di `.gitignore`).

### Rilis
1. Tambah bagian `## [x.y.z]` di `CHANGELOG.md`
2. `git tag v1.6.0 && git push origin v1.6.0` (tag dengan `-` seperti `v1.7.0-beta` menjadi pre-release)
3. Release berisi catatan dari `CHANGELOG.md`, daftar commit sejak tag sebelumnya, APK, dan `SHA256SUMS.txt`

versionCode dihitung dari tag: `major*10000 + minor*100 + patch` (v1.6.0 menjadi 10600).

## Struktur
```
app/src/main/cpp/resteam.cpp      UI, statistik, bubble, sesi, auto-switch
app/src/main/java/.../Bridge.java Shizuku, HTTP, info sistem (shim)
app/src/main/java/.../HostService Surface overlay (shim)
.github/                          workflow + composite action setup toolchain
art/icon.png                      ikon sumber (512px)
```

## Catatan
- Update dibaca dari rilis **terbaru yang bukan pre-release** pada repo di atas; APK harus ditandatangani keystore yang sama agar bisa menimpa
- Repo privat tidak bisa dicek tanpa token (muncul pesan galat di tab ABOUT)
- Resolusi per game memakai `wm size` dan `wm density`; bila app crash di tengah sesi, jalankan `wm size reset; wm density reset`
- Info online diambil dari halaman Google Play, jadi bisa kosong bila format halaman berubah atau perangkat offline
- Beberapa tweak tergantung versi Android dan ROM; perintah yang tidak didukung gagal tanpa efek
