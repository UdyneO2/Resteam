# Game Launcher + Shizuku Dashboard

Aplikasi Android (Kotlin, Jetpack Compose, MVVM, Hilt, Coroutines/Flow) dengan layout adaptive:
HP memakai bottom bar, tablet/foldable memakai sidebar.

## Cara menjalankan
1. Buka folder ini di Android Studio (Ladybug atau lebih baru, JDK 17). Gradle wrapper akan dibuat/diunduh otomatis.
2. Sync project, lalu Run di device/emulator (minSdk 26).
3. Pasang & jalankan Shizuku (https://shizuku.rikka.app) agar pill status berubah menjadi "Ready".
4. Game terdeteksi otomatis. Untuk jam main & riwayat aktivitas, aktifkan *Usage access* untuk app ini (tombol tersedia di dalam aplikasi).

## Struktur
- `ui/theme`: warna, shape, typography (design system)
- `ui/components`: LauncherCard, pill, tombol ikon
- `ui/navigation`: shell adaptive (sidebar / bottom bar)
- `ui/home`: top bar, hero banner, achievements, heatmap aktivitas, ViewModel
- `domain`: model + kontrak repository
- `data`: InstalledGameRepository (PackageManager + UsageStats), GameLauncher
- `shizuku`: ShizukuManager (status reaktif via Flow), ShellService (AIDL UserService), ShizukuShellExecutor
- `domain/usecase`: ApplyPresetUseCase (whitelist perintah)
- `data/local`: Room (preset tweak per game)

## Tweak preset (Fase 6)
Tombol Tweak preset membuka daftar preset (disimpan di Room, di-seed otomatis: Performance, Balanced, Battery Saver).
Apply menjalankan via Shizuku:
- `cmd game set --mode <standard|performance|battery> <package>` (Android 12+)
- `cmd package compile -m <speed-profile|speed> -f <package>` (opsional)

Keamanan: command dikirim sebagai array argumen, bukan string shell; package divalidasi regex; hanya perintah whitelist yang dibuat oleh `buildCommands`.
Tombol terminal membuka log hasil eksekusi.

## Deteksi game (Fase 7)
- Daftar diambil dari app yang punya launcher activity dan `ApplicationInfo.category == CATEGORY_GAME` (tanpa izin QUERY_ALL_PACKAGES).
- Game lama / sideload yang tidak mengisi kategori tidak terdeteksi. Solusi lanjutan: daftar manual (include/exclude).
- Jam main dan riwayat harian memakai UsageStatsManager (butuh Usage access). Jam main = total waktu foreground 12 bulan terakhir (perkiraan, bukan angka resmi).
- Daftar disegarkan otomatis saat layar kembali aktif.
- Kartu Achievements dihapus karena sistem Android tidak menyediakan data achievement.

## Manage games & ikon (Fase 8)
- Layar **Manage**: cari semua aplikasi terpasang dan aktifkan/nonaktifkan sebagai game. Pilihan manual disimpan di Room (tabel `game_overrides`, migrasi 1→2 menjaga preset lama) dan menang atas deteksi otomatis.
- Ikon aplikasi: adaptive icon dari logo pentagon + kontroler (`res/drawable/ic_launcher_foreground.xml`, background `#0E0F11`, plus `ic_launcher_monochrome.xml` untuk themed icon Android 13+). Logo yang sama dipakai di sidebar (`ic_logo.xml`).
- Semua ikon berupa VectorDrawable XML (tanpa PNG), jadi tajam di semua kepadatan layar.

## Tab Shizuku
Status layanan (Ready/izin/tidak aktif), tombol aksi sesuai kondisi (buka Shizuku, minta izin, diagnostik), panduan aktivasi.
Diagnostik menjalankan perintah read-only (`id`, `getprop ro.build.version.release`) untuk memverifikasi koneksi.

## Settings
Disimpan di DataStore Preferences: nama tampilan (dipakai di sapaan Home), konfirmasi sebelum menerapkan preset, pintasan akses penggunaan, versi aplikasi.

## Tab Shizuku Tweaks
Kelola preset per game: buat, edit, hapus (Room), pilih game mode (standard/performance/battery) dan compile filter, lalu Apply langsung dari tab.

## Tab Profiles
Profil per game: pilih preset default dan aktifkan auto-apply. Saat Launch Game ditekan, preset diterapkan lewat Shizuku lebih dulu (jika Shizuku siap), lalu game dibuka. Kegagalan preset tidak menghalangi game dibuka.
Database sekarang versi 4 (migrasi 1→2→3→4 menjaga data lama).

## Tab Logs & System properties
- `ActivityLog` bersama (in-memory, maks 200 entri) dicatat otomatis oleh `ApplyPresetUseCase`, jadi apply dari Home, tab Tweaks, maupun auto-apply saat Launch muncul di tab **Logs** dan dialog terminal.
- Tombol info di hero membuka **System properties**: info perangkat, versi game, dan game mode saat ini (read-only `cmd game list-modes`, hanya jika Shizuku siap).
- Log hilang saat proses aplikasi dimatikan (belum dipersistenkan).

## Test
`./gradlew testDebugUnitTest` (JUnit 5 + Mockk)

## Catatan
- Game mode butuh Android 12+; di versi lebih lama perintah akan gagal dan error tampil di log.
- Teks UI masih hardcoded; pindahkan ke strings.xml sebelum rilis.
- Belum ada ikon launcher kustom.
