# Setup completo — buildar e rodar o Rodapé numa máquina WSL nova

Guia pra replicar do zero numa máquina **WSL/Ubuntu** (headless, sem Android Studio).
Versões do projeto: **JDK 17**, **Android SDK 36**, **Gradle 9.3.1** (via wrapper), **AGP 9.1.1**.

---

## Passo 0 — clonar e trazer os segredos

```bash
git clone https://github.com/gabrielfeelix/rodape.git
cd rodape
```

Dois arquivos **não estão no git** (segredos) e precisam ser copiados pra cá manualmente
(de um pendrive, outro PC, etc.):

- **`.env`** (na raiz do repo) — URL/keys do Supabase + segredos de assinatura.
- **`app/google-services.json`** — config do Firebase (push). Baixe do Firebase Console:
  projeto **`aplicativos-497303`**, precisa ter os apps **`app.rodape`** E **`app.rodape.debug`**
  (o arquivo baixado contém os dois). Sem o `.debug`, o build de debug falha em
  `processDebugGoogleServices`.

Confere que o google-services tem os 2 packages:
```bash
python3 -c "import json;print([c['client_info']['android_client_info']['package_name'] for c in json.load(open('app/google-services.json'))['client']])"
# deve mostrar ['app.rodape', 'app.rodape.debug']
```

## Passo 1 — JDK + Android SDK (build)

```bash
bash scripts/setup-wsl-build.sh
```
Instala JDK 17 + Android SDK 36, aceita licenças, cria `local.properties` (gitignored) e
põe `JAVA_HOME`/`ANDROID_HOME` no `~/.bashrc`. **Uma vez por máquina.** Se o Gradle pedir
JDK 21, edite `JDK_PKG` no script pra `openjdk-21-jdk` e rode de novo.

Abra terminal novo (ou `source ~/.bashrc`) e valide:
```bash
./gradlew compileDebugKotlin      # só compila (rápido). Espera BUILD SUCCESSFUL.
```

## Passo 2 — emulador (ver o app sem celular)

Requer **KVM** (aceleração) e **WSLg** (tela GUI) — WSL2 no Windows 11 tem os dois.

```bash
bash scripts/setup-emulator.sh
```
Esse script: instala libs de sistema do emulador (libpulse0, libnss3, libx*, libgl1),
adiciona você ao grupo `kvm`, libera `/dev/kvm`, instala o emulador + imagem
**API 34 google_apis** (tem Google Play services → push funciona) e cria o AVD `rodape`.

> Se depois de um `wsl --shutdown` o `/dev/kvm` voltar sem permissão, rode
> `sudo chmod 666 /dev/kvm` (ou entre em sessão nova — o grupo kvm já foi setado).

Ligar o emulador e instalar o app:
```bash
~/Android/Sdk/emulator/emulator -avd rodape -gpu swiftshader_indirect &
# espera a home do Android aparecer (1ª boot ~1min, tela preta um tempo é normal)
./gradlew installDebug
~/Android/Sdk/platform-tools/adb shell monkey -p app.rodape.debug 1   # abre o app
```
Tela preta / janela não abre → troque `-gpu swiftshader_indirect` por `-gpu host`.

## Comandos do dia a dia

| Comando | O quê |
|---|---|
| `./gradlew compileDebugKotlin` | só compila o Kotlin (pega erros, rápido) |
| `./gradlew installDebug` | monta o APK e instala no emulador/celular ligado |
| `./gradlew assembleDebug` | gera o APK de debug (em `app/build/outputs/apk/debug/`) |
| `adb devices` | lista emulador/celulares conectados |

**`gradlew`** = Gradle Wrapper: baixa a versão certa do Gradle e roda o build. Nunca se
instala Gradle na mão.

## Gotchas que já pegamos (pra não repetir)

- `JAVA_HOME is not set` → faltou o Passo 1.
- `No matching client found for package name 'app.rodape.debug'` → o google-services.json
  só tinha `app.rodape`; registre o app `.debug` no Firebase e rebaixe.
- `libpulse.so.0: cannot open shared object file` → faltam libs de sistema (o setup-emulator
  instala).
- `This user doesn't have permissions to use KVM` → faltou entrar no grupo kvm / liberar
  `/dev/kvm` (o setup-emulator faz).

---

## Push (FCM) — estado

Backend **100% no ar** (projeto Supabase `zfbywoeajebvasnsrzfh`): tabela `device_tokens`,
trigger, Edge Function `send-push` (ACTIVE), secrets FCM e service_role no Vault. Cliente
Android (registro de token + serviço + permissão) já no código. Detalhes e teste
ponta-a-ponta em [`release/push-fcm-setup.md`](release/push-fcm-setup.md).
