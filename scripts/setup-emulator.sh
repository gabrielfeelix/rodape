#!/usr/bin/env bash
# Instala o emulador Android + uma imagem (API 34 com Google Play services, pro push)
# e cria um AVD chamado "rodape". Rode UMA vez:  bash scripts/setup-emulator.sh
# Requisitos (já checados nesta máquina): /dev/kvm (aceleração) e WSLg (tela GUI).
set -euo pipefail

SDK="$HOME/Android/Sdk"
SDKMGR="$SDK/cmdline-tools/latest/bin/sdkmanager"
AVDMGR="$SDK/cmdline-tools/latest/bin/avdmanager"
IMG="system-images;android-34;google_apis;x86_64"   # google_apis = tem GMS (push funciona)
NAME="rodape"

if [ ! -e /dev/kvm ]; then
  echo "ERRO: /dev/kvm ausente — sem aceleração o emulador é inviável. Ative virtualização no WSL." >&2
  exit 1
fi

echo "==> libs de sistema pro emulador (WSL enxuto não as tem)"
sudo apt-get update -y
sudo apt-get install -y libpulse0 libnss3 libxcursor1 libxcomposite1 libxi6 libxtst6 libgl1

echo "==> permissão KVM (grupo kvm)"
sudo gpasswd -a "$USER" kvm || true
# O grupo só vale em sessão nova (após 'wsl --shutdown' no PowerShell). Pra usar JÁ
# nesta sessão sem relogar, libera o device agora (reseta ao reiniciar o WSL):
sudo chmod 666 /dev/kvm || true

echo "==> instalando emulator + platform-tools + imagem ($IMG)"
yes | "$SDKMGR" --sdk_root="$SDK" --licenses >/dev/null || true
"$SDKMGR" --sdk_root="$SDK" "emulator" "platform-tools" "$IMG"

echo "==> criando AVD '$NAME' (Pixel 6)"
if "$AVDMGR" list avd 2>/dev/null | grep -q "Name: $NAME"; then
  echo "    AVD '$NAME' já existe — mantendo."
else
  echo "no" | "$AVDMGR" create avd -n "$NAME" -k "$IMG" -d pixel_6
fi

# Garante o emulador no PATH pra sessões futuras.
grep -q 'Sdk/emulator' "$HOME/.bashrc" 2>/dev/null || \
  echo 'export PATH="$PATH:$HOME/Android/Sdk/emulator"' >> "$HOME/.bashrc"

cat <<EOF

OK! Pra ver o app:

  1) Liga o emulador (abre uma janela — 1ª vez demora ~1min):
       ~/Android/Sdk/emulator/emulator -avd $NAME -gpu swiftshader_indirect &

  2) Espera aparecer a tela inicial do Android, então instala e abre o app:
       ./gradlew installDebug
       ~/Android/Sdk/platform-tools/adb shell monkey -p app.rodape.debug 1

  (Se a janela não abrir / tela preta, troque -gpu swiftshader_indirect por -gpu host.)
EOF
