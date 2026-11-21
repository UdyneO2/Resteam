#!/system/bin/sh
# name: Phantom Process Unlimited
# desc: Cegah Android 12+ membunuh proses latar (butuh Android 12+)
# version: 1

NAME="phantom_process"
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
    set_s global settings_enable_monitor_phantom_procs false
    device_config set_sync_disabled_for_tests persistent >/dev/null 2>&1 || true
    device_config put activity_manager max_phantom_processes 2147483647 >/dev/null 2>&1 || true
    notify "Phantom process killer dimatikan"
    ;;
  restore)
    restore_s
    device_config delete activity_manager max_phantom_processes >/dev/null 2>&1 || true
    device_config set_sync_disabled_for_tests none >/dev/null 2>&1 || true
    notify "Phantom process dikembalikan"
    ;;
esac
