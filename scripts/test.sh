#!/system/bin/sh
# name: GPU Boost
# desc: Naikkan clock GPU saat bermain
# version: 1

case "$1" in
  apply)
    cmd notification post -S bigtext -t "test" "test1" "test2" >/dev/null 2>&1 || true
    ;;
  restore)
    echo "done"
    ;;
esac