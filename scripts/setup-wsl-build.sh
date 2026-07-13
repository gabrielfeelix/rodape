#!/usr/bin/env bash
# Setup de build do Rodapé numa máquina WSL/Ubuntu, headless (sem Android Studio).
# Instala JDK + Android SDK cmdline-tools, aceita licenças e cria local.properties.
# Rode UMA vez por máquina:  bash scripts/setup-wsl-build.sh
set -euo pipefail

JDK_PKG="openjdk-17-jdk"          # AGP 9.x pede JDK 17+. Se reclamar, troque p/ openjdk-21-jdk.
SDK_ROOT="$HOME/Android/Sdk"
CMDLINE_ZIP_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
PLATFORM="platforms;android-36"
BUILD_TOOLS="build-tools;36.0.0"
REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "==> 1/5 JDK"
if ! command -v java >/dev/null 2>&1; then
  sudo apt-get update -y && sudo apt-get install -y "$JDK_PKG" unzip curl
else
  echo "    java já presente: $(java -version 2>&1 | head -1)"
fi
JAVA_HOME_DIR="$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")"

echo "==> 2/5 Android cmdline-tools em $SDK_ROOT"
mkdir -p "$SDK_ROOT/cmdline-tools"
if [ ! -d "$SDK_ROOT/cmdline-tools/latest" ]; then
  tmp="$(mktemp -d)"
  curl -L "$CMDLINE_ZIP_URL" -o "$tmp/cmdline.zip"
  unzip -q "$tmp/cmdline.zip" -d "$tmp"
  mv "$tmp/cmdline-tools" "$SDK_ROOT/cmdline-tools/latest"
  rm -rf "$tmp"
fi
SDKMGR="$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"

echo "==> 3/5 pacotes do SDK (platform-tools, $PLATFORM, $BUILD_TOOLS)"
export JAVA_HOME="$JAVA_HOME_DIR"
yes | "$SDKMGR" --sdk_root="$SDK_ROOT" --licenses >/dev/null || true
"$SDKMGR" --sdk_root="$SDK_ROOT" "platform-tools" "$PLATFORM" "$BUILD_TOOLS"

echo "==> 4/5 local.properties"
printf 'sdk.dir=%s\n' "$SDK_ROOT" > "$REPO_DIR/local.properties"

echo "==> 5/5 variáveis no ~/.bashrc (idempotente)"
grep -q 'ANDROID_HOME=' "$HOME/.bashrc" 2>/dev/null || {
  {
    echo ""
    echo "# Android/Java build env (rodape)"
    echo "export JAVA_HOME=\"$JAVA_HOME_DIR\""
    echo "export ANDROID_HOME=\"$SDK_ROOT\""
    echo "export PATH=\"\$PATH:\$ANDROID_HOME/platform-tools:\$ANDROID_HOME/cmdline-tools/latest/bin\""
  } >> "$HOME/.bashrc"
}

echo ""
echo "OK. Abra um novo terminal (ou 'source ~/.bashrc') e rode:"
echo "  cd $REPO_DIR && ./gradlew compileDebugKotlin"
