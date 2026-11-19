# WiFi Share

Bagikan internet HP lewat WiFi Direct, cara kerjanya mirip PdaNet. Tampilan bergaya iOS dengan efek liquid glass
([Backdrop](https://kyant.gitbook.io/backdrop)). Full Kotlin + Jetpack Compose, minSdk 29 (efek penuh di Android 13+).

## Cara kerja

1. HP membuat grup WiFi Direct (HP jadi group owner, biasanya `192.168.49.1`).
2. Proxy lokal berjalan di port yang diatur (bawaan 8000). Satu port melayani HTTP/HTTPS dan SOCKS5.
3. Perangkat lain sambung ke jaringan `DIRECT-...`, lalu atur proxy manual ke `192.168.49.1:8000`.
4. Lalu lintas proxy keluar lewat koneksi data HP.

Proxy hanya menerima koneksi dari subnet WiFi Direct, jadi tidak terbuka untuk WiFi lain atau jaringan seluler.

## Batasan

- Aplikasi yang mengabaikan pengaturan proxy sistem tidak akan lewat. Ini sifat metode proxy, bukan bug.
- Hanya TCP. UDP (misalnya panggilan video atau game) tidak diteruskan.
- Perangkat tujuan harus mendukung proxy manual di pengaturan WiFi.

## Build di Ubuntu

```
./scripts/setup-ubuntu.sh   # JDK 17 + Android SDK, sekali saja
./scripts/build.sh          # APK debug di app/build/outputs/apk
./scripts/build.sh assembleRelease
```

Release perlu keystore sendiri, atur `signingConfigs` di `app/build.gradle.kts` sebelum dibagikan.

## Struktur

| Folder | Isi |
|--------|-----|
| `wifi/` | `WifiDirectController`, membuat dan memantau grup |
| `proxy/` | `ProxyServer` (HTTP, CONNECT, SOCKS5) dan `P2pGuard` |
| `service/` | `ShareService` (foreground) dan `ShareHub` |
| `ui/` | tema iOS, komponen glass, layar WiFi dan Pengaturan |

## Whitelist 10 update ke depan

Satu fitur per versi. Versi 1.1 (QR) sudah ada. Versi 1.2 menambah tab Klien dan komponen Backdrop (dialog kaca, tombol liquid, slider, scroll container) di luar daftar ini. Sisanya masih rencana.

| Versi | Fitur | Catatan |
|-------|-------|---------|
| 1.1 | Sambung via QR (sudah ada) | QR berisi SSID dan password, klien scan untuk join |
| 1.2 | Meter dan batas data | Hitung byte lewat proxy, putus otomatis saat batas tercapai |
| 1.3 | Batas kecepatan | Token bucket per koneksi klien di layer proxy |
| 1.4 | Kick dan blokir | `WifiP2pManager.removeClient` bila tersedia, kalau tidak tutup koneksi proxy |
| 1.5 | Timer otomatis mati | Pilihan 15 menit, 1 jam, 3 jam |
| 1.6 | Tile Quick Settings | `TileService` untuk toggle berbagi |
| 1.7 | Autentikasi proxy | Username dan password untuk HTTP dan SOCKS5 |
| 1.8 | Filter DNS | Daftar domain terblokir, dicek di proxy sebelum resolve |
| 1.9 | Allowlist perangkat | Hanya alamat MAC terdaftar yang boleh tersambung |
| 1.10 | Backup dan ekspor log | Ekspor pengaturan JSON dan log koneksi |

Setiap rilis: naikkan `versionCode` dan `versionName` di `app/build.gradle.kts`.
