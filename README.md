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

## Test
`./gradlew testDebugUnitTest` (JUnit 5 + Mockk)

## Catatan
- Game mode butuh Android 12+; di versi lebih lama perintah akan gagal dan error tampil di log.
- Teks UI masih hardcoded; pindahkan ke strings.xml sebelum rilis.
- Belum ada ikon launcher kustom.
