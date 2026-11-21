#!/system/bin/sh
# name: Compile Game ml
# desc: Compile speed-profile untuk game (lama, jalankan sekali; arg ke-2: package) for ml
# version: beta

NAME="compile_game"
S="/data/local/tmp/tweaks/$NAME.state"
mkdir -p /data/local/tmp/tweaks

# Simpan nilai asli (sekali saja), lalu set nilai baru
set_s() {
  { [ -f "$S" ] && grep -q "^$1 $2 " "$S"; } || echo "$1 $2 $(settings get "$1" "$2")" >> "$S"
  settings put "$1" "$2" "$3"
}
# Kembalikan semua nilai asli; "null" berarti key aslinya belum ada -> dihapus
restore_s() {
  [ -f "$S" ] || return 0
  while read -r ns key val; do
    if [ "$val" = "null" ]; then settings delete "$ns" "$key"; else settings put "$ns" "$key" "$val"; fi
  done < "$S"
  rm -f "$S"
}
notify() { cmd notification post -S bigtext -t "$NAME" "tweak_$NAME" "$1" >/dev/null 2>&1 || true; }

PKG="${2:-com.mobile.legends}"
case "$1" in
  apply)
    cmd package compile -m speed-profile -f "$PKG" >/dev/null 2>&1 || true
    notify "Compile selesai: $PKG"
    ;;
  restore)
    cmd package compile --reset "$PKG" >/dev/null 2>&1 || true
    notify "Compile direset: $PKG"
    ;;
esac
