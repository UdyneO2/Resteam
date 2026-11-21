# ReSteam

Launcher game untuk Android dengan tampilan ala Steam, plus kemampuan menjalankan tweak sistem lewat [Shizuku](https://shizuku.rikka.app) tepat saat game dibuka, lalu mengembalikannya setelah game ditutup.

Package: `com.udyneos.resteam`

Saya buat ini karena bosan menjalankan script tweak secara manual tiap kali main. Sekarang cukup tekan Launch, sisanya diurus aplikasi.

> Proyek ini masih dalam pengembangan dan belum diuji di banyak perangkat. Kalau ada yang rusak, buka issue saja.

## Fitur

- **Library game.** Game terdeteksi otomatis dari aplikasi terpasang. Jam main dan riwayat aktivitas muncul kalau *Usage access* diaktifkan. Tiap game bisa diberi banner sendiri dari galeri.
- **Manage.** Kalau ada game yang tidak terdeteksi (atau ada app yang salah terdeteksi), atur manual di sini.
- **Preset per game.** Game mode (`standard`, `performance`, `battery`) dan compile filter, diterapkan sebelum game dibuka.
- **Shizuku Tweaks.** Daftar script global yang diambil dari folder `scripts/` di repo GitHub ini. Tiap script punya toggle sendiri, dan ada tombol Update untuk menarik versi terbaru. Branch bisa dipilih langsung di aplikasi.
- **Profiles.** Pilih preset default tiap game, dan nyalakan toggle "Jalankan Shizuku Tweaks" kalau mau script aktif ikut jalan saat Launch.
- **Logs.** Semua hasil apply dan restore tercatat di sini.
- Layout menyesuaikan layar: bottom bar di HP, sidebar di tablet atau foldable.

## Cara kerja script

Script jalan sebagai shell lewat Shizuku. Alurnya:

1. Kamu tekan **Launch Game**.
2. Kalau toggle di Profiles menyala, tiap script yang aktif dijalankan dengan `sh script.sh apply`.
3. Aplikasi memantau game lewat `pidof`. Setelah game tertutup, script dijalankan lagi dengan `restore`, urutannya dibalik.

Catatan apply dibuat sebelum script dijalankan dan disimpan di database. Jadi kalau ReSteam ditutup atau di-kill saat game masih jalan, restore tetap dilanjutkan begitu aplikasi dibuka lagi. Selama sesi, ada notifikasi foreground service supaya sistem tidak mematikan prosesnya.

### Format script

Taruh file `.sh` di folder `scripts/` repo ini. Metadata dibaca dari komentar di 30 baris pertama:

```sh
#!/system/bin/sh
# name: GPU Boost
# desc: Naikkan clock GPU saat bermain
# version: 1

case "$1" in
  apply)
    echo performance > /sys/class/kgsl/kgsl-3d0/devfreq/governor
    ;;
  restore)
    echo msm-adreno-tz > /sys/class/kgsl/kgsl-3d0/devfreq/governor
    ;;
esac
```

Template siap pakai ada di [`scripts/base.sh`](scripts/base.sh): sudah berisi notifikasi (`cmd notification post`) dan penyimpanan nilai asli ke file, karena `apply` dan `restore` berjalan sebagai proses terpisah. Salin file itu, ganti nama dan isi bagian tweak.

Tanpa `# name:`, nama file dipakai. Script dibatasi 100 KB dan 60 detik per eksekusi; proses yang lewat batas dimatikan paksa.

Path di contoh hanya ilustrasi. Nilai dan path sysfs beda di tiap perangkat, jadi cek dulu di HP kamu.

## Menjalankan

Butuh Android Studio (Ladybug atau lebih baru), JDK 17, dan perangkat dengan Android 8.0 (API 26) ke atas.

1. Buka folder ini di Android Studio dan tunggu Gradle sync.
2. Run ke perangkat atau emulator. Shizuku hanya berguna di perangkat sungguhan.
3. Pasang Shizuku, jalankan layanannya (Wireless debugging lalu Start, atau lewat ADB), lalu tekan "Berikan izin" di tab Shizuku.
4. Opsional: aktifkan *Usage access* untuk ReSteam supaya jam main tampil.

Game mode (`cmd game`) butuh Android 12+. Di versi lebih lama, perintah itu gagal dan errornya muncul di Logs. Script `.sh` tidak bergantung pada versi, kecuali isinya sendiri.

Aplikasi butuh internet hanya untuk mengambil daftar script dari GitHub, tanpa token, jadi kena batas 60 permintaan per jam dari GitHub. Kalau Update gagal karena batas itu, tunggu sebentar.

## Struktur kode

```
app/src/main/java/com/udyneos/resteam/
  ui/          layar Compose (home, scripts, profiles, manage, settings, logs)
  domain/      model, kontrak repository, use case (termasuk GameSessionManager)
  data/        Room, DataStore, sumber script GitHub, penyimpanan banner
  shizuku/     ShizukuManager, ShellService (AIDL UserService), executor
  session/     foreground service untuk apply, pantau, dan restore
  di/          modul Hilt
```

Stack: Kotlin, Jetpack Compose, Hilt, Room, DataStore, Coroutines/Flow, Coil.

## Keamanan

Script berjalan dengan hak akses shell (uid 2000, atau root kalau Shizuku jalan di root). Itu cukup kuat untuk merusak pengaturan sistem, jadi:

- Hanya aktifkan script yang kamu baca dan pahami.
- Pakai repo yang kamu kontrol. Aplikasi hanya mengunduh dari `raw.githubusercontent.com`, tetapi isi repo sepenuhnya tanggung jawab pemiliknya.
- Perintah dikirim sebagai array argumen, bukan string shell yang dirakit sendiri. Nama repo, branch, file, dan package divalidasi sebelum dipakai.

## Batasan yang saya tahu

- Game dianggap tertutup setelah dua pengecekan `pidof` berturut-turut kosong (sekitar 10 detik). Kalau perangkat kamu tidak punya `pidof`, restore tidak akan jalan.
- Script yang dihapus dari repo sebelum restore tidak bisa di-restore. Aplikasi hanya mencatatnya di Logs.
- Log hanya di memori, hilang saat proses mati.
- Foto banner dengan rotasi EXIF bisa tampil miring.
- Teks UI masih ditulis langsung di kode, belum dipindah ke `strings.xml`.

## Lisensi

[MIT](LICENSE). Pustaka yang dipakai punya lisensinya sendiri; Shizuku misalnya memakai Apache 2.0.
