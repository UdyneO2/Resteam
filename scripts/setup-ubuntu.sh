#!/usr/bin/env bash
# Setup sekali jalan untuk build di Ubuntu: JDK 17, Android SDK command-line tools, lisensi.
set -euo pipefail

SDK="${ANDROID_HOME:-$HOME/Android/Sdk}"
CMDLINE_ZIP="commandlinetools-linux-11076708_latest.zip"

sudo apt-get update
sudo apt-get install -y openjdk-17-jdk unzip curl

mkdir -p "$SDK/cmdline-tools"
if [ ! -d "$SDK/cmdline-tools/latest" ]; then
    curl -fL -o "/tmp/$CMDLINE_ZIP" "https://dl.google.com/android/repository/$CMDLINE_ZIP"
    unzip -q "/tmp/$CMDLINE_ZIP" -d "$SDK/cmdline-tools"
    mv "$SDK/cmdline-tools/cmdline-tools" "$SDK/cmdline-tools/latest"
fi

yes | "$SDK/cmdline-tools/latest/bin/sdkmanager" --licenses >/dev/null || true
"$SDK/cmdline-tools/latest/bin/sdkmanager" "platform-tools" "platforms;android-35" "build-tools;35.0.0"

# supaya gradle tahu lokasi SDK
echo "sdk.dir=$SDK" > "$(dirname "$0")/../local.properties"

echo "Selesai. Jalankan: ./scripts/build.sh"
