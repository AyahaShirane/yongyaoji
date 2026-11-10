#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_HOME:=${ANDROID_SDK_ROOT:-}}"
: "${ANDROID_HOME:?Set ANDROID_HOME (or ANDROID_SDK_ROOT)}"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 21}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/build-tools/35.0.0:$PATH"
PLATFORM="$ANDROID_HOME/platforms/android-35/android.jar"
for tool in javac aapt2 d8 zipalign apksigner; do command -v "$tool" >/dev/null || { echo "Missing build tool: $tool" >&2; exit 2; }; done
[[ -f "$PLATFORM" ]] || { echo "Missing Android 35 platform" >&2; exit 2; }
OUT=dist
mkdir -p "$OUT/work/classes" "$OUT/work/dex" "$OUT/work/generated"
WORK="$OUT/work"
javac --release 8 -d "$WORK/icon-tools" tools/IconGenerator.java
java -Djava.awt.headless=true -cp "$WORK/icon-tools" IconGenerator src/main/res "$OUT"
aapt2 compile -o "$WORK/resources.zip" --dir src/main/res
aapt2 link -o "$WORK/base.apk" --manifest AndroidManifest.xml -I "$PLATFORM" --min-sdk-version 26 --target-sdk-version 35 --java "$WORK/generated" "$WORK/resources.zip"
javac --release 8 -cp "$PLATFORM" -d "$WORK/classes" "$WORK/generated/cn/medreminder/app/R.java" src/main/java/cn/medreminder/app/*.java
d8 --lib "$PLATFORM" --min-api 26 --output "$WORK/dex" "$WORK/classes/cn/medreminder/app"/*.class
python3 - "$WORK/base.apk" "$WORK/dex/classes.dex" "$WORK/unsigned.apk" <<'PY'
import sys,zipfile
base,dex,out=sys.argv[1:]
with zipfile.ZipFile(base) as src,zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as dst:
 for item in src.infolist(): dst.writestr(item,src.read(item.filename))
 dst.write(dex,'classes.dex')
PY
APK="$OUT/yongyaoji-debug-unsigned.apk"
SIGNED=0
if [[ -n "${SIGNING_KEYSTORE:-}" || -n "${KEYSTORE_PASSWORD:-}" || -n "${KEY_PASSWORD:-}" ]]; then
 : "${SIGNING_KEYSTORE:?Set SIGNING_KEYSTORE}"
 : "${KEYSTORE_PASSWORD:?Set KEYSTORE_PASSWORD}"
 : "${KEY_PASSWORD:?Set KEY_PASSWORD}"
 APK="$OUT/yongyaoji-v1.3.5.apk"
 SIGNED=1
fi
if [[ -n "${OUTPUT_APK:-}" ]]; then APK="$OUTPUT_APK"; fi
mkdir -p "$(dirname "$APK")"
[[ ! -e "$APK" ]] || { echo "Refusing to overwrite existing output: $APK" >&2; exit 3; }
zipalign -f 4 "$WORK/unsigned.apk" "$WORK/aligned.apk"
if [[ "$SIGNED" -eq 1 ]]; then
 apksigner sign --ks "$SIGNING_KEYSTORE" --ks-pass env:KEYSTORE_PASSWORD --key-pass env:KEY_PASSWORD --out "$APK" "$WORK/aligned.apk"
 apksigner verify --verbose "$APK"
else
 cp "$WORK/aligned.apk" "$APK"
 echo "Created unsigned debug APK: $APK (sign it with a key you control before installation)"
fi
