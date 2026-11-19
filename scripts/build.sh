#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# AGP 8.7 dan Kotlin 2.0 hanya jalan di JDK 17 sampai 21. JDK yang lebih baru (misalnya 25)
# bikin compiler Kotlin gagal baca versi Java, jadi JAVA_HOME bawaan sistem sengaja ditimpa.
JDK17=""
for dir in /usr/lib/jvm/java-17-openjdk-* /usr/lib/jvm/temurin-17* /usr/lib/jvm/jdk-17*; do
    if [ -x "$dir/bin/java" ]; then
        JDK17="$dir"
        break
    fi
done

if [ -z "$JDK17" ]; then
    echo "JDK 17 tidak ditemukan. Jalankan ./scripts/setup-ubuntu.sh dulu." >&2
    exit 1
fi

export JAVA_HOME="$JDK17"
export PATH="$JAVA_HOME/bin:$PATH"
echo "Pakai JDK: $(java -version 2>&1 | head -1) ($JAVA_HOME)"

# Gradle gagal bikin folder di cache kalau disk penuh atau ~/.gradle milik root (pernah jalan dengan sudo)
GRADLE_HOME_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}"
mkdir -p "$GRADLE_HOME_DIR"
if [ ! -w "$GRADLE_HOME_DIR" ]; then
    echo "$GRADLE_HOME_DIR tidak bisa ditulis. Perbaiki dengan: sudo chown -R \"\$USER\" $GRADLE_HOME_DIR" >&2
    exit 1
fi
FREE_MB=$(df -Pm "$GRADLE_HOME_DIR" | awk 'NR==2 {print $4}')
if [ "$FREE_MB" -lt 4096 ]; then
    echo "Ruang disk tinggal ${FREE_MB} MB, build butuh minimal sekitar 4 GB." >&2
    echo "Kosongkan dulu, contoh: rm -rf \"$GRADLE_HOME_DIR/caches\" && sudo apt-get clean && docker system prune -af" >&2
    exit 1
fi

# daemon lama yang masih jalan di JDK 25 harus dimatikan supaya tidak dipakai ulang
./gradlew --stop >/dev/null 2>&1 || true

TASK="${1:-assembleDebug}"
./gradlew "$TASK" --stacktrace

echo
echo "APK:"
find app/build/outputs/apk -name '*.apk' -print
