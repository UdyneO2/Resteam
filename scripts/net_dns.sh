#!/system/bin/sh
# name: Private DNS Cloudflare
# desc: Private DNS 1.1.1.1 + TCP init rwnd
# version: 1

NAME="net_dns"
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
    set_s global private_dns_specifier 1dot1dot1dot1.cloudflare-dns.com
    set_s global private_dns_mode hostname
    set_s global tcp_default_init_rwnd 60
    notify "Private DNS Cloudflare aktif"
    ;;
  restore)
    restore_s
    notify "Pengaturan jaringan dikembalikan"
    ;;
esac
