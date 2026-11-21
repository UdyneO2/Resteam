#!/system/bin/sh
# name: Game Mode ml
# desc: Performance mode, thermal override, driver opt-in (arg ke-2: package) for ml
# version: beta

NAME="game_mode"
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
    cmd thermalservice override-status 0 >/dev/null 2>&1 || true
    cmd power set-fixed-performance-mode-enabled true >/dev/null 2>&1 || true
    cmd game mode performance "$PKG" >/dev/null 2>&1 || true
    set_s global updatable_driver_prerelease_opt_in_apps "$PKG"
    notify "Game mode aktif: $PKG"
    ;;
  restore)
    cmd thermalservice reset >/dev/null 2>&1 || true
    cmd power set-fixed-performance-mode-enabled false >/dev/null 2>&1 || true
    cmd game mode standard "$PKG" >/dev/null 2>&1 || true
    restore_s
    notify "Game mode dikembalikan"
    ;;
esac
