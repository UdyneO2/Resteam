#!/system/bin/sh
# name: Animasi Cepat
# desc: Skala animasi 0.5x agar UI terasa responsif
# version: 1

NAME="animation"
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

case "$1" in
  apply)
    set_s global window_animation_scale 0.5
    set_s global transition_animation_scale 0.5
    set_s global animator_duration_scale 0.5
    notify "Animasi 0.5x"
    ;;
  restore)
    restore_s
    notify "Animasi dikembalikan"
    ;;
esac
