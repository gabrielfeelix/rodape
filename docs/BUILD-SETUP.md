# Setup de build (WSL / Linux, headless — sem Android Studio)

Pra compilar o Rodapé numa máquina nova, você precisa de **JDK + Android SDK**.
O app usa: **JDK 17**, **Android SDK 36**, **Gradle 9.3.1** (via wrapper), **AGP 9.1.1**.

## Setup automático (1 comando)

```bash
cd <pasta-do-repo>
bash scripts/setup-wsl-build.sh
```
Ele instala JDK 17 + Android SDK 36, aceita as licenças, cria `local.properties`
(gitignored) e adiciona `JAVA_HOME`/`ANDROID_HOME` ao `~/.bashrc`. Roda **uma vez por
máquina**. Se o Gradle pedir JDK 21, edite `JDK_PKG` no script pra `openjdk-21-jdk`.

Depois, terminal novo (ou `source ~/.bashrc`):
```bash
./gradlew compileDebugKotlin   # confere se compila
./gradlew assembleDebug        # gera o APK de debug
```

## Conceitos rápidos

- **`gradlew`** (Gradle Wrapper): script no repo que baixa a versão certa do Gradle e
  roda o build. Você nunca instala o Gradle na mão.
- **`compileDebugKotlin`**: só compila o Kotlin (rápido, pega erros). Não gera APK.
- **`assembleDebug`**: monta o APK instalável de debug.

## Segredos que NÃO estão no git (precisam existir localmente)

Cada máquina precisa ter, na raiz do repo:
- **`.env`** — URL/keys do Supabase + segredos de assinatura (já usado pelo build).
- **`app/google-services.json`** — config do Firebase (push). Baixe do Firebase Console
  (projeto `aplicativos-497303`, apps `app.rodape` e `app.rodape.debug`).

Sem esses, o build de push falha. Ver `docs/release/push-fcm-setup.md`.
