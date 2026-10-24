#!/bin/sh
# Builds build/laibach-launcher.apk with the Android SDK tools directly (no Gradle).
set -eu
cd "$(dirname "$0")"

SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
BT="$SDK/build-tools/35.0.0"
JAR="$SDK/platforms/android-36/android.jar"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21}"
export PATH="$JAVA_HOME/bin:$PATH"
OUT=build

# The signing key is made once. Keep keystore/ to be able to install updates
# over the installed app; a new key means uninstalling first.
if [ ! -f keystore/laibach.jks ]; then
  mkdir -p keystore
  openssl rand -hex 16 > keystore/password.txt
  keytool -genkeypair -keystore keystore/laibach.jks -alias laibach \
    -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Laibach Launcher" \
    -storepass:file keystore/password.txt -keypass:file keystore/password.txt
fi

rm -rf "$OUT"
mkdir -p "$OUT/classes" "$OUT/dex"

"$BT/aapt2" compile --dir res -o "$OUT/res.zip"
"$BT/aapt2" link -o "$OUT/unsigned.apk" -I "$JAR" --manifest AndroidManifest.xml "$OUT/res.zip"

javac -source 17 -target 17 -Xlint:-options -Xlint:deprecation -encoding UTF-8 \
  -classpath "$JAR" -d "$OUT/classes" $(find src -name '*.java')
"$BT/d8" --release --min-api 31 --lib "$JAR" --output "$OUT/dex" $(find "$OUT/classes" -name '*.class')
(cd "$OUT/dex" && zip -q ../unsigned.apk classes.dex)
# Tables kept beside their classes (the Slovenian texts) travel as they are.
(cd src && zip -q "../$OUT/unsigned.apk" $(find . -name '*.txt' | sed 's|^\./||'))

"$BT/zipalign" -f -p 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
"$BT/apksigner" sign --ks keystore/laibach.jks --ks-pass file:keystore/password.txt \
  --out "$OUT/laibach-launcher.apk" "$OUT/aligned.apk"
"$BT/apksigner" verify "$OUT/laibach-launcher.apk"
ls -l "$OUT/laibach-launcher.apk"
