#!/usr/bin/env bash
# Builds and signs the smallest possible Overcast Mini APK (~18-25 KB)
# directly using Android SDK CLI tools (aapt2 + javac + d8 + zipalign + apksigner).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -d "$SCRIPT_DIR/app/src/main/res" ]; then
    ROOT_DIR="$SCRIPT_DIR"
elif [ -d "./app/src/main/res" ]; then
    ROOT_DIR="$(pwd)"
elif [ -d "./overcast-mini-widget/app/src/main/res" ]; then
    ROOT_DIR="$(pwd)/overcast-mini-widget"
else
    FOUND="$(find . -maxdepth 4 -type d -path '*/app/src/main/res' | head -n 1 || true)"
    if [ -n "$FOUND" ]; then
        ROOT_DIR="$(cd "$(dirname "$(dirname "$(dirname "$(dirname "$FOUND")")")")" && pwd)"
    else
        echo "ERROR: Could not locate app/src/main/res from $(pwd)" >&2
        ls -la
        exit 1
    fi
fi

cd "$ROOT_DIR"
echo "Project root:      $ROOT_DIR"

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
BUILD_TOOLS=$(ls -d "$SDK"/build-tools/* | sort -V | tail -n 1)
PLATFORM=$(ls -d "$SDK"/platforms/android-* | sort -V | tail -n 1)
ANDROID_JAR="$PLATFORM/android.jar"

OUT="build_min"
rm -rf "$OUT"
mkdir -p "$OUT/gen" "$OUT/obj" "$OUT/dex"

echo "Using build-tools: $BUILD_TOOLS"
echo "Using platform:    $PLATFORM"

# 1. Compile resources with aapt2
"$BUILD_TOOLS/aapt2" compile --dir app/src/main/res -o "$OUT/res.zip"

# 2. Link resources & generate R.java (inject package attr into temp manifest for aapt2)
sed 's|<manifest |<manifest package="com.overcast.mini" |' app/src/main/AndroidManifest.xml > "$OUT/AndroidManifest.xml"
"$BUILD_TOOLS/aapt2" link \
    -I "$ANDROID_JAR" \
    --manifest "$OUT/AndroidManifest.xml" \
    --java "$OUT/gen" \
    --min-sdk-version 26 \
    --target-sdk-version 35 \
    -o "$OUT/unsigned.apk" \
    "$OUT/res.zip"

# 3. Compile pure Java sources (no debug info -> smallest bytecode)
javac -source 11 -target 11 -Xlint:-options -g:none \
    -classpath "$ANDROID_JAR" \
    -d "$OUT/obj" \
    "$OUT/gen/com/overcast/mini/R.java" \
    app/src/main/java/com/overcast/mini/*.java

# 4. Convert to optimized release DEX
"$BUILD_TOOLS/d8" --release --min-api 26 --lib "$ANDROID_JAR" \
    --output "$OUT/dex" $(find "$OUT/obj" -name '*.class')

# 5. Pack classes.dex into APK with maximum compression
(cd "$OUT/dex" && zip -9 -u ../unsigned.apk classes.dex)

# 6. Align and sign with a local debug keystore
"$BUILD_TOOLS/zipalign" -f -p 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"

KEYSTORE="$OUT/debug.keystore"
keytool -genkeypair -v -keystore "$KEYSTORE" -storepass android -keypass android \
    -alias overcast -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=OvercastMini,O=OpenSource,C=US" >/dev/null 2>&1

"$BUILD_TOOLS/apksigner" sign \
    --ks "$KEYSTORE" --ks-pass pass:android --key-pass pass:android \
    --out "$OUT/overcast-mini-widget.apk" "$OUT/aligned.apk"

"$BUILD_TOOLS/apksigner" verify "$OUT/overcast-mini-widget.apk"
ls -lh "$OUT/overcast-mini-widget.apk"
echo "Done! Install with: adb install -r $OUT/overcast-mini-widget.apk"
