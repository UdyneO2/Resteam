# Changelog

Format: satu bagian per versi dengan judul `## [x.y.z]`. Workflow rilis mengambil bagian yang cocok dengan tag `vX.Y.Z`.

## [1.8]
- Panel bubble didesain ulang ala game panel: kolom statistik (CPU, GPU, FPS, ping, waktu main), baris app PIP berikon, grid shortcut, tombol RESTEAM dan TUTUP
- Shortcut: Mode (BAL/PERF/BAT), Lock FPS (kunci refresh rate: auto atau rate yang didukung layar), DND, Rotasi, Settings
- Status Lock FPS dan DND disinkronkan dari sistem saat panel dibuka

## [1.7]
- Bubble menampilkan FPS live (bukan ikon), hanya bisa digeser dari bagian atas
- Tombol SETTINGS di bubble: kembali ke aplikasi (langsung ke tab Settings)
- Overlay statistik (FPS, CPU, GPU, ping) dihapus; statistik tetap ada di library, bubble, dan riwayat sesi
- Update dari GitHub Releases di tab ABOUT: cek versi, unduh APK, pasang (senyap via Shizuku, fallback installer sistem), cek otomatis saat dibuka
- Opsi rotasi aplikasi di Settings: auto landscape, landscape tetap, kunci rotasi saat ini

## [1.6]
- Auto-switch mode: otomatis ke profil game saat game berjalan, kembali Balanced saat game ditutup
- Resolusi layar per game (Native / 85% / 75% / 720P), dikembalikan otomatis setelah main
- Riwayat sesi: durasi, FPS rata-rata/minimum, GPU, CPU, suhu maks, baterai, grafik FPS dan suhu
- Tab ABOUT di samping SETTINGS, tab HISTORY baru
- Ikon aplikasi dari PNG, build GitHub Actions (debug + release, keystore sama, changelog otomatis)

## [1.5]
- Switch mode performa (BAL/PERF/BAT) di bubble, bubble lebih kecil dengan ikon petir
- Library vertikal, tweak mode performa lebih banyak dan bisa diatur di Settings

## [1.4]
- Landscape, fetch info game (Google Play + data lokal), waktu bermain
- Floating bubble bulat, metode FPS berlapis (SurfaceFlinger, display, frame counter)

## [1.3]
- Library gaya PS4 dengan ikon dan animasi, info baterai/jaringan di sidebar, sidebar bisa dipindah

## [1.2]
- LAUNCH, library hanya game, label app dari sistem

## [1.1]
- Sidebar PIP, tab Settings, immersive penuh, info baterai dan jaringan di top bar

## [1.0]
- Rilis awal: overlay statistik, mode performa, launch PIP, Shizuku, UI ala Steam
