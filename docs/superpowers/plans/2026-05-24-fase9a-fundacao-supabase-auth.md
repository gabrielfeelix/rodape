# Fase 9A — Fundação Supabase + Auth real — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Plugar o app Android no Supabase Auth real (Email/Senha + Google + reset por email), com `SupabaseClient` singleton vivo no `Application`, deep link OAuth funcionando — sem ainda tocar nos repos de dados (Room continua atendendo o app em paralelo).

**Arquitetura:** Adicionar dependências (supabase-kt 3.6.0 + Credential Manager + googleid + ktor + serialization plugin). Criar `RodapeApp : Application`, registrar no Manifest. Criar `object Supabase` singleton lendo `BuildConfig.SUPABASE_URL/KEY`. Criar `AuthRepository` thin wrapper sobre `Supabase.client.auth`. Reescrever `LoginScreen` removendo botão demo + dica `voce@rodape.com`, adicionar form real, fluxo Google via Credential Manager. Adicionar telas novas `SignUpScreen`, `ForgotPasswordScreen`, `ResetPasswordScreen` em arquivos próprios. Registrar rotas no NavHost. `MainActivity` chama `handleDeeplinks(intent)` em `onCreate` e `onNewIntent`. `MainViewModel.currentUserId` passa a observar `Supabase.client.auth.sessionStatus` em paralelo ao DataStore antigo.

**Tech Stack:** supabase-kt 3.6.0 (`auth-kt`, `postgrest-kt`, `realtime-kt`, `storage-kt`), Ktor CIO 3.5.0, Kotlin serialization plugin 2.2.10, AndroidX Credentials 1.6.0, Google Identity (`googleid`) 1.2.0, core library desugaring (minSdk 24 → java.time).

---

## File Structure

**Modify:**
- `gradle/libs.versions.toml` — adiciona `[versions]`, `[libraries]`, `[plugins]` para supabase-kt, ktor, credentials, googleid, kotlin-serialization, desugaring.
- `app/build.gradle.kts` — aplica plugin `kotlin("plugin.serialization")`, adiciona deps novas, habilita `isCoreLibraryDesugaringEnabled = true`.
- `app/src/main/AndroidManifest.xml` — `android:name=".RodapeApp"` + segundo intent-filter `app.rodape://login-callback`.
- `app/src/main/java/com/example/MainActivity.kt` — `handleDeeplinks(intent)` em `onCreate` + `onNewIntent`. `startDestination` deriva de `Supabase.client.auth.sessionStatus` (substitui `viewModel.currentUserId`). Adicionar rotas `signup`, `forgot_password`, `reset_password`.
- `app/src/main/java/com/example/ui/screens/WelcomeScreen.kt` — reescrever `LoginScreen` (linhas 174-490): remover botão "Conta de Teste", remover dica `voce@rodape.com`, remover toggle `isSignUp` (vai pra tela própria), trocar `onLoginSuccess` callback por `onSignInSuccess` chamando AuthRepository. Botão Google passa a chamar callback `onGoogleSignInClick`.
- `app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt` — adicionar `authRepository` em `init`; expor `sessionStatus: StateFlow<SessionStatus>`; `currentUserId` passa a derivar de `authRepository.sessionStatus.map { it.userIdOrNull() }`. NÃO remover ainda código antigo de seed/DataStore — sub-fase 9B fará isso.

**Create:**
- `app/src/main/java/com/example/RodapeApp.kt` — `class RodapeApp : Application()`.
- `app/src/main/java/com/example/data/remote/Supabase.kt` — `object Supabase` com `val client: SupabaseClient` lazy.
- `app/src/main/java/com/example/data/remote/AuthRepository.kt` — wrapper sobre `Supabase.client.auth`.
- `app/src/main/java/com/example/ui/screens/SignUpScreen.kt` — tela de cadastro Email/Senha.
- `app/src/main/java/com/example/ui/screens/ForgotPasswordScreen.kt` — tela "esqueci a senha".
- `app/src/main/java/com/example/ui/screens/ResetPasswordScreen.kt` — landing do deep link de reset.
- `app/src/main/java/com/example/ui/auth/GoogleSignInHelper.kt` — encapsula Credential Manager + extração de `idToken`.

**Delete:** nenhum (limpeza fica para 9C).

---

## Task 1: Adicionar versões no version catalog

**Files:**
- Modify: `gradle/libs.versions.toml`

- [ ] **Step 1: Adicionar entradas `[versions]`**

Em `gradle/libs.versions.toml`, dentro do bloco `[versions]`, adicionar (em ordem alfabética entre as existentes):

```toml
credentials = "1.6.0"
desugarJdkLibs = "2.1.5"
googleid = "1.2.0"
ktor = "3.5.0"
kotlinxSerializationJson = "1.7.3"
supabase = "3.6.0"
```

- [ ] **Step 2: Adicionar entradas `[libraries]`**

```toml
androidx-credentials = { group = "androidx.credentials", name = "credentials", version.ref = "credentials" }
androidx-credentials-play-services-auth = { group = "androidx.credentials", name = "credentials-play-services-auth", version.ref = "credentials" }
desugar-jdk-libs = { group = "com.android.tools", name = "desugar_jdk_libs", version.ref = "desugarJdkLibs" }
googleid = { group = "com.google.android.libraries.identity.googleid", name = "googleid", version.ref = "googleid" }
ktor-client-cio = { group = "io.ktor", name = "ktor-client-cio", version.ref = "ktor" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerializationJson" }
supabase-bom = { group = "io.github.jan-tennert.supabase", name = "bom", version.ref = "supabase" }
supabase-auth-kt = { group = "io.github.jan-tennert.supabase", name = "auth-kt" }
supabase-postgrest-kt = { group = "io.github.jan-tennert.supabase", name = "postgrest-kt" }
supabase-realtime-kt = { group = "io.github.jan-tennert.supabase", name = "realtime-kt" }
supabase-storage-kt = { group = "io.github.jan-tennert.supabase", name = "storage-kt" }
```

- [ ] **Step 3: Adicionar entrada `[plugins]`**

```toml
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

(Reusa `version.ref = "kotlin"` que já existe, pareando o plugin com a versão Kotlin do projeto — 2.2.10.)

- [ ] **Step 4: Validar sintaxe TOML**

Run: `./gradlew :app:dependencies --console=plain 2>&1 | head -5`
Expected: comando começa a executar sem erro de parse de TOML (pode dar warning de deps não usadas, ok).

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml
git commit -m "chore(fase9a): adicionar versions catalog pra supabase-kt + credentials + ktor"
```

---

## Task 2: Plugar deps + plugin serialization + desugaring no app/build.gradle.kts

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Adicionar plugin serialization**

No bloco `plugins { ... }` no topo de `app/build.gradle.kts`, adicionar:

```kotlin
alias(libs.plugins.kotlin.serialization)
```

Fica assim:

```kotlin
plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}
```

- [ ] **Step 2: Habilitar core library desugaring**

No bloco `android { compileOptions { ... } }`, adicionar `isCoreLibraryDesugaringEnabled = true`:

```kotlin
  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
```

- [ ] **Step 3: Adicionar dependências novas**

Dentro de `dependencies { ... }`, junto com as outras `implementation(...)`, adicionar:

```kotlin
  // --- Supabase ---
  implementation(platform(libs.supabase.bom))
  implementation(libs.supabase.auth.kt)
  implementation(libs.supabase.postgrest.kt)
  implementation(libs.supabase.realtime.kt)
  implementation(libs.supabase.storage.kt)
  implementation(libs.ktor.client.cio)
  implementation(libs.kotlinx.serialization.json)

  // --- Google Sign-In via Credential Manager ---
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services.auth)
  implementation(libs.googleid)

  // --- Core library desugaring (java.time em minSdk 24) ---
  coreLibraryDesugaring(libs.desugar.jdk.libs)
```

- [ ] **Step 4: Compilar pra validar resolução de dependências**

Run: `./gradlew :app:dependencies --configuration releaseRuntimeClasspath --console=plain 2>&1 | grep -E "supabase|credentials|ktor|googleid|desugar" | head -20`
Expected: lista mostra as deps Supabase 3.6.0, credentials 1.6.0, ktor 3.5.0, googleid 1.2.0 e desugar 2.1.5 resolvidas.

- [ ] **Step 5: Build debug pra garantir sem erro de versão**

Run: `./gradlew :app:assembleDebug --console=plain 2>&1 | tail -30`
Expected: BUILD SUCCESSFUL. (App ainda não usa nada do Supabase, só está disponível no classpath.)

- [ ] **Step 6: Commit**

```bash
git add app/build.gradle.kts
git commit -m "chore(fase9a): supabase-kt + credentials + ktor + desugaring no app gradle"
```

---

## Task 3: Criar BuildConfig fields pra SUPABASE_URL/KEY via plugin Secrets

**Files:**
- Modify: `app/build.gradle.kts`

> Contexto: o plugin Secrets injeta automaticamente todas as chaves do `.env` como campos `BuildConfig.X`. Vamos validar que `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`, `GOOGLE_WEB_CLIENT_ID` ficam disponíveis e expostos como `String` no `BuildConfig`.

- [ ] **Step 1: Confirmar que `buildConfig = true` está habilitado**

Já está em `buildFeatures { buildConfig = true }` — sem mudança.

- [ ] **Step 2: Confirmar config do plugin Secrets**

No final do arquivo já existe:

```kotlin
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}
```

Sem mudança. O plugin lê tudo automaticamente.

- [ ] **Step 3: Rebuildar pra gerar BuildConfig.kt**

Run: `./gradlew :app:generateDebugBuildConfig --console=plain 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Verificar que campos foram gerados**

Run: `find app/build -name "BuildConfig.java" -exec grep -E "SUPABASE_URL|SUPABASE_PUBLISHABLE_KEY|GOOGLE_WEB_CLIENT_ID" {} \;`
Expected: 3 linhas, cada uma `public static final String NOME = "...";` confirmando que o plugin Secrets gerou os campos.

- [ ] **Step 5: Commit (no-op de código mas valida)**

Sem mudanças a commitar nesta task. Pular para próxima.

---

## Task 4: Criar singleton `Supabase` em data/remote/

**Files:**
- Create: `app/src/main/java/com/example/data/remote/Supabase.kt`

- [ ] **Step 1: Criar diretório**

Run: `mkdir -p app/src/main/java/com/example/data/remote`
Expected: silêncio.

- [ ] **Step 2: Escrever o arquivo**

Conteúdo de `app/src/main/java/com/example/data/remote/Supabase.kt`:

```kotlin
package com.example.data.remote

import com.example.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

/**
 * Cliente Supabase único do app. Lazy thread-safe via Kotlin `object`.
 *
 * URL e chave vêm do .env via plugin Secrets → BuildConfig.
 * Usamos a publishable key (sb_publishable_...), nunca service_role.
 * RLS no servidor é a única autorização.
 */
object Supabase {
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        ) {
            install(Auth) {
                flowType = FlowType.PKCE
                scheme = "app.rodape"
                host = "login-callback"
            }
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
    }
}
```

- [ ] **Step 3: Compilar pra resolver imports**

Run: `./gradlew :app:compileDebugKotlin --console=plain 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL. (Se faltar import, ajustar pelo erro — supabase-kt 3.x usa `io.github.jan.supabase.*`.)

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/data/remote/Supabase.kt
git commit -m "feat(fase9a): singleton SupabaseClient lendo BuildConfig"
```

---

## Task 5: Criar `RodapeApp : Application` e registrar no Manifest

**Files:**
- Create: `app/src/main/java/com/example/RodapeApp.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Escrever `RodapeApp.kt`**

Conteúdo de `app/src/main/java/com/example/RodapeApp.kt`:

```kotlin
package com.example

import android.app.Application
import com.example.data.remote.Supabase

class RodapeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Toca no singleton pra forçar warm-up (lazy thread-safe).
        Supabase.client
    }
}
```

- [ ] **Step 2: Registrar no Manifest**

Em `app/src/main/AndroidManifest.xml`, dentro de `<application ...>`, adicionar atributo `android:name=".RodapeApp"`:

```xml
    <application
        android:name=".RodapeApp"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.MyApplication">
```

- [ ] **Step 3: Build pra validar**

Run: `./gradlew :app:assembleDebug --console=plain 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/RodapeApp.kt app/src/main/AndroidManifest.xml
git commit -m "feat(fase9a): RodapeApp Application + warm-up do Supabase client"
```

---

## Task 6: Adicionar intent-filter do deep link OAuth no Manifest

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Adicionar segundo intent-filter**

Dentro de `<activity android:name=".MainActivity" ...>`, **depois** do intent-filter existente (que tem `action.MAIN` + `category.LAUNCHER`), adicionar:

```xml
            <intent-filter android:autoVerify="false">
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="app.rodape" android:host="login-callback" />
            </intent-filter>
```

O bloco `<activity>` inteiro fica:

```xml
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.MyApplication">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter android:autoVerify="false">
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="app.rodape" android:host="login-callback" />
            </intent-filter>
        </activity>
```

- [ ] **Step 2: Build pra validar XML**

Run: `./gradlew :app:processDebugMainManifest --console=plain 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/AndroidManifest.xml
git commit -m "feat(fase9a): intent-filter pra deep link app.rodape://login-callback"
```

---

## Task 7: Plugar `handleDeeplinks` no MainActivity

**Files:**
- Modify: `app/src/main/java/com/example/MainActivity.kt`

- [ ] **Step 1: Importar `handleDeeplinks`**

Adicionar imports no topo de `MainActivity.kt`:

```kotlin
import android.content.Intent
import com.example.data.remote.Supabase
import io.github.jan.supabase.auth.handleDeeplinks
```

- [ ] **Step 2: Chamar `handleDeeplinks` em `onCreate`**

Dentro de `onCreate`, depois de `super.onCreate(savedInstanceState)`, adicionar:

```kotlin
        Supabase.client.handleDeeplinks(intent)
```

- [ ] **Step 3: Adicionar `onNewIntent`**

Logo após o `onCreate`, adicionar:

```kotlin
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Supabase.client.handleDeeplinks(intent)
    }
```

- [ ] **Step 4: Build**

Run: `./gradlew :app:assembleDebug --console=plain 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/MainActivity.kt
git commit -m "feat(fase9a): MainActivity propaga deep links pro Supabase Auth"
```

---

## Task 8: Criar `AuthRepository` thin wrapper

**Files:**
- Create: `app/src/main/java/com/example/data/remote/AuthRepository.kt`

- [ ] **Step 1: Escrever o arquivo**

Conteúdo de `app/src/main/java/com/example/data/remote/AuthRepository.kt`:

```kotlin
package com.example.data.remote

import io.github.jan.supabase.auth.SessionStatus
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * Wrapper fino sobre Supabase Auth. Cada método propaga exceções —
 * a UI trata via try/catch + Snackbar nesta fase (sem Result/sealed).
 */
class AuthRepository(private val supabase: io.github.jan.supabase.SupabaseClient = Supabase.client) {

    val sessionStatus: StateFlow<SessionStatus> = supabase.auth.sessionStatus

    /** UUID do usuário logado, ou null se não autenticado. */
    val currentUserIdFlow: Flow<String?> = sessionStatus.map { status ->
        (status as? SessionStatus.Authenticated)?.session?.user?.id
    }

    /** Snapshot do usuário logado (null se não autenticado). */
    val currentUser: UserInfo?
        get() = (sessionStatus.value as? SessionStatus.Authenticated)?.session?.user

    suspend fun signUpWithEmail(email: String, password: String, displayName: String) {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            // displayName vira user_metadata.full_name; o trigger handle_new_user
            // lê esse campo e popula profiles.nome.
            data = kotlinx.serialization.json.buildJsonObject {
                put("full_name", kotlinx.serialization.json.JsonPrimitive(displayName))
            }
        }
    }

    suspend fun signInWithEmail(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signInWithGoogleIdToken(idToken: String, rawNonce: String? = null) {
        supabase.auth.signInWith(IDToken) {
            this.idToken = idToken
            this.provider = Google
            this.nonce = rawNonce
        }
    }

    suspend fun sendPasswordResetEmail(email: String) {
        supabase.auth.resetPasswordForEmail(email)
    }

    /** Chamada depois que o deep link de reset trouxe a sessão temporária. */
    suspend fun updatePassword(newPassword: String) {
        supabase.auth.updateUser {
            password = newPassword
        }
    }

    suspend fun signOut() {
        supabase.auth.signOut()
    }
}
```

- [ ] **Step 2: Compilar**

Run: `./gradlew :app:compileDebugKotlin --console=plain 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL. (Se algum import falhar, verificar na doc supabase-kt 3.6 — nomes podem variar.)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/data/remote/AuthRepository.kt
git commit -m "feat(fase9a): AuthRepository sobre Supabase Auth (email/google/reset)"
```

---

## Task 9: Criar helper Google Sign-In via Credential Manager

**Files:**
- Create: `app/src/main/java/com/example/ui/auth/GoogleSignInHelper.kt`

- [ ] **Step 1: Criar diretório**

Run: `mkdir -p app/src/main/java/com/example/ui/auth`

- [ ] **Step 2: Escrever helper**

Conteúdo de `app/src/main/java/com/example/ui/auth/GoogleSignInHelper.kt`:

```kotlin
package com.example.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import java.security.MessageDigest
import java.util.UUID

/**
 * Encapsula o fluxo Credential Manager → GoogleIdToken.
 * Retorna o idToken bruto + o nonce raw (cliente passa ambos pro Supabase).
 */
class GoogleSignInHelper(private val context: Context) {

    data class Result(val idToken: String, val rawNonce: String)

    suspend fun getGoogleIdToken(): Result {
        val rawNonce = UUID.randomUUID().toString()
        val hashedNonce = hashSha256(rawNonce)

        val option = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(true)
            .setNonce(hashedNonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        val response = CredentialManager.create(context).getCredential(context, request)
        val credential = response.credential
        check(credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Credential retornada não é um GoogleIdTokenCredential"
        }
        val tokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        return Result(idToken = tokenCredential.idToken, rawNonce = rawNonce)
    }

    private fun hashSha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
```

- [ ] **Step 3: Compilar**

Run: `./gradlew :app:compileDebugKotlin --console=plain 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/ui/auth/GoogleSignInHelper.kt
git commit -m "feat(fase9a): helper Google Sign-In via Credential Manager"
```

---

## Task 10: Expor `sessionStatus` no MainViewModel (paralelo ao DataStore)

**Files:**
- Modify: `app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt`

> Não removemos nada antigo nesta task — só adicionamos os novos fluxos pra `MainActivity` poder usá-los. A remoção do código antigo de seed/DataStore acontece na sub-fase 9B.

- [ ] **Step 1: Importar AuthRepository**

Adicionar no topo de `MainViewModel.kt`:

```kotlin
import com.example.data.remote.AuthRepository
import io.github.jan.supabase.auth.SessionStatus
```

- [ ] **Step 2: Instanciar `authRepository` como propriedade**

Logo após `private val dataStoreManager = DataStoreManager(application)` (linha ~20):

```kotlin
    private val authRepository = AuthRepository()
```

- [ ] **Step 3: Expor `sessionStatus` como StateFlow**

Adicionar logo após a declaração de `authRepository`:

```kotlin
    val sessionStatus: StateFlow<SessionStatus> = authRepository.sessionStatus

    val supabaseUserId: StateFlow<String?> = authRepository.currentUserIdFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
```

> Nota: deixamos `currentUserId` antigo (DataStore-baseado) intacto pra não quebrar
> nada nesta sub-fase. `MainActivity` vai consumir `supabaseUserId` no Task 12.

- [ ] **Step 4: Adicionar wrapper de signOut que limpa DataStore também**

Logo após a função `logout(...)` existente (~linha 314), adicionar:

```kotlin
    fun signOutSupabase(onCompleted: () -> Unit) {
        viewModelScope.launch {
            try {
                authRepository.signOut()
            } catch (e: Exception) {
                // ignora — sessão já pode estar inválida
            }
            dataStoreManager.clearSession()
            onCompleted()
        }
    }
```

- [ ] **Step 5: Build**

Run: `./gradlew :app:assembleDebug --console=plain 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt
git commit -m "feat(fase9a): MainViewModel expõe sessionStatus + supabaseUserId em paralelo"
```

---

## Task 11: Reescrever `LoginScreen` — form real, sem botão demo, sem dica

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/WelcomeScreen.kt` (LoginScreen entre linhas 174-490)

> Reescreve o `LoginScreen` inteiro. Mantém o `onLoginSuccess(name, email)` apenas
> como bridge — o callback será invocado depois que `AuthRepository.signInWithEmail`
> retornar sucesso. Sub-fase 9B vai trocar essa callback por chamada direta na VM.

- [ ] **Step 1: Substituir o corpo do `LoginScreen`**

Em `WelcomeScreen.kt`, localizar `@Composable fun LoginScreen(` (linha ~174) e substituir todo o corpo até o fechamento da função (linha ~490) por:

```kotlin
@Composable
fun LoginScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onSignInWithEmail: suspend (email: String, password: String) -> Result<Unit>,
    onSignInWithGoogle: suspend () -> Result<Unit>,
    onSignedIn: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val isEmailValid = email.contains("@") && email.length >= 5
    val isPasswordValid = password.length >= 6
    val isFormValid = isEmailValid && isPasswordValid

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rodapé", style = MaterialTheme.typography.headlineLarge.copy(color = Terracota)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Terracota
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                RodapeCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)
                ) {
                    Text(
                        text = "Bem-vindo de volta",
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.trim(); errorMsg = null },
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMsg = null },
                        label = { Text("Senha") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onNavigateToForgotPassword,
                        modifier = Modifier.align(Alignment.End),
                        enabled = !isLoading,
                    ) {
                        Text("Esqueci minha senha", color = Terracota)
                    }

                    errorMsg?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            isLoading = true
                            errorMsg = null
                            scope.launch {
                                val result = onSignInWithEmail(email, password)
                                isLoading = false
                                result.fold(
                                    onSuccess = { onSignedIn() },
                                    onFailure = { errorMsg = it.message ?: "Falha ao entrar" },
                                )
                            }
                        },
                        enabled = isFormValid && !isLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Terracota),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp),
                            )
                        } else {
                            Text("Entrar", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = Color.White))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            isLoading = true
                            errorMsg = null
                            scope.launch {
                                val result = onSignInWithGoogle()
                                isLoading = false
                                result.fold(
                                    onSuccess = { onSignedIn() },
                                    onFailure = { errorMsg = it.message ?: "Falha no Google Sign-In" },
                                )
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        border = BorderStroke(1.dp, Divider),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = "Google",
                            tint = Ink,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Continuar com Google", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, color = Ink))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onNavigateToSignUp, enabled = !isLoading) {
                        Text("Ainda não tem conta? Cadastre-se", color = OlivaDark)
                    }
                }
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
```

- [ ] **Step 2: Garantir imports faltando**

Verificar que esses imports já estão no topo (são todos usados pelas telas existentes):

```kotlin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
```

Adicionar os que faltarem (a maioria já existe — o arquivo é grande).

- [ ] **Step 3: Compilar**

Run: `./gradlew :app:compileDebugKotlin --console=plain 2>&1 | tail -30`
Expected: erros nos call sites do `LoginScreen` em `MainActivity.kt` (assinatura mudou). Vamos arrumar no Task 12.

- [ ] **Step 4: Commit (parcial — build ainda quebrado, será resolvido em Task 12)**

```bash
git add app/src/main/java/com/example/ui/screens/WelcomeScreen.kt
git commit -m "feat(fase9a): LoginScreen reescrita — form real sem botão demo nem dica"
```

---

## Task 12: Criar `SignUpScreen`, `ForgotPasswordScreen`, `ResetPasswordScreen`

**Files:**
- Create: `app/src/main/java/com/example/ui/screens/SignUpScreen.kt`
- Create: `app/src/main/java/com/example/ui/screens/ForgotPasswordScreen.kt`
- Create: `app/src/main/java/com/example/ui/screens/ResetPasswordScreen.kt`

- [ ] **Step 1: `SignUpScreen.kt`**

Conteúdo completo:

```kotlin
package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.RodapeCard
import com.example.ui.theme.Divider
import com.example.ui.theme.Ink
import com.example.ui.theme.OlivaDark
import com.example.ui.theme.Terracota
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onNavigateBack: () -> Unit,
    onSignUp: suspend (email: String, password: String, name: String) -> Result<Unit>,
    onSignedUp: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showConfirmHint by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val nameValid = name.trim().length >= 2
    val emailValid = email.contains("@") && email.length >= 5
    val pwValid = password.length >= 6
    val formValid = nameValid && emailValid && pwValid

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Criar conta", style = MaterialTheme.typography.headlineLarge.copy(color = Terracota)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar", tint = Terracota)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Spacer(Modifier.height(24.dp))
                RodapeCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
                    if (showConfirmHint) {
                        Text(
                            "Conta criada! Confira seu email pra confirmar o cadastro antes de entrar.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onSignedUp,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Terracota),
                        ) {
                            Text("Voltar para login", color = Color.White)
                        }
                    } else {
                        Text("Bem-vindo ao Rodapé", style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(value = name, onValueChange = { name = it; errorMsg = null }, label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(value = email, onValueChange = { email = it.trim(); errorMsg = null }, label = { Text("Email") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(value = password, onValueChange = { password = it; errorMsg = null }, label = { Text("Senha (6+ caracteres)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), enabled = !isLoading)

                        errorMsg?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        }

                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                isLoading = true
                                errorMsg = null
                                scope.launch {
                                    val r = onSignUp(email, password, name.trim())
                                    isLoading = false
                                    r.fold(
                                        onSuccess = { showConfirmHint = true },
                                        onFailure = { errorMsg = it.message ?: "Falha ao criar conta" },
                                    )
                                }
                            },
                            enabled = formValid && !isLoading,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Terracota),
                        ) {
                            if (isLoading) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            else Text("Cadastrar", color = Color.White, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}
```

- [ ] **Step 2: `ForgotPasswordScreen.kt`**

```kotlin
package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.RodapeCard
import com.example.ui.theme.Terracota
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    onSendReset: suspend (email: String) -> Result<Unit>,
) {
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val emailValid = email.contains("@") && email.length >= 5

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recuperar senha", style = MaterialTheme.typography.headlineLarge.copy(color = Terracota)) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar", tint = Terracota) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            item {
                Spacer(Modifier.height(24.dp))
                RodapeCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
                    if (sent) {
                        Text("Se essa conta existir, enviamos um email com um link para redefinir a senha. Confira sua caixa.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    } else {
                        Text("Te mandamos um link por email", style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(value = email, onValueChange = { email = it.trim(); errorMsg = null }, label = { Text("Email") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
                        errorMsg?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                isLoading = true
                                scope.launch {
                                    val r = onSendReset(email)
                                    isLoading = false
                                    r.fold(
                                        onSuccess = { sent = true },
                                        onFailure = { errorMsg = it.message ?: "Falha ao enviar email" },
                                    )
                                }
                            },
                            enabled = emailValid && !isLoading,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Terracota),
                        ) {
                            if (isLoading) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            else Text("Enviar link de redefinição", color = Color.White, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}
```

- [ ] **Step 3: `ResetPasswordScreen.kt`**

```kotlin
package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.RodapeCard
import com.example.ui.theme.Terracota
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    onPasswordUpdated: () -> Unit,
    onUpdatePassword: suspend (newPassword: String) -> Result<Unit>,
) {
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val valid = password.length >= 6

    Scaffold(
        topBar = { TopAppBar(title = { Text("Nova senha", style = MaterialTheme.typography.headlineLarge.copy(color = Terracota)) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            item {
                Spacer(Modifier.height(24.dp))
                RodapeCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
                    Text("Defina uma nova senha", style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMsg = null },
                        label = { Text("Nova senha (6+ caracteres)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                    )
                    errorMsg?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            isLoading = true
                            scope.launch {
                                val r = onUpdatePassword(password)
                                isLoading = false
                                r.fold(
                                    onSuccess = { onPasswordUpdated() },
                                    onFailure = { errorMsg = it.message ?: "Falha ao redefinir senha" },
                                )
                            }
                        },
                        enabled = valid && !isLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Terracota),
                    ) {
                        if (isLoading) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        else Text("Redefinir senha", color = Color.White, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/ui/screens/SignUpScreen.kt app/src/main/java/com/example/ui/screens/ForgotPasswordScreen.kt app/src/main/java/com/example/ui/screens/ResetPasswordScreen.kt
git commit -m "feat(fase9a): telas SignUp / ForgotPassword / ResetPassword"
```

---

## Task 13: Ligar `MainActivity` nas novas telas + AuthRepository

**Files:**
- Modify: `app/src/main/java/com/example/MainActivity.kt`

> Aqui resolvemos a quebra de build deixada pelo Task 11 (assinatura do LoginScreen mudou) e plugamos as novas rotas.

- [ ] **Step 1: Adicionar imports**

```kotlin
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.data.remote.AuthRepository
import com.example.ui.auth.GoogleSignInHelper
import com.example.ui.screens.ForgotPasswordScreen
import com.example.ui.screens.ResetPasswordScreen
import com.example.ui.screens.SignUpScreen
import io.github.jan.supabase.auth.SessionStatus
```

- [ ] **Step 2: Trocar `startDestination` para depender de `sessionStatus`**

Localizar:

```kotlin
                    val currentUserId by viewModel.currentUserId.collectAsState()
                    val startDestination = if (currentUserId != null) {
                        "main_tabs"
                    } else {
                        "welcome"
                    }
```

Substituir por:

```kotlin
                    val supabaseUserId by viewModel.supabaseUserId.collectAsState()
                    val startDestination = if (supabaseUserId != null) "main_tabs" else "welcome"
```

> Mantemos `viewModel.currentUserId` em outras telas — só o `startDestination` muda nesta sub-fase.

- [ ] **Step 3: Reescrever a rota `login`**

Substituir o `composable("login") { ... }` inteiro por:

```kotlin
                    composable("login") {
                        val ctx = LocalContext.current
                        val authRepo = remember { AuthRepository() }
                        val google = remember { GoogleSignInHelper(ctx) }
                        LoginScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToSignUp = { navController.navigate("signup") },
                            onNavigateToForgotPassword = { navController.navigate("forgot_password") },
                            onSignInWithEmail = { email, password ->
                                runCatching { authRepo.signInWithEmail(email, password) }
                            },
                            onSignInWithGoogle = {
                                runCatching {
                                    val token = google.getGoogleIdToken()
                                    authRepo.signInWithGoogleIdToken(token.idToken, token.rawNonce)
                                }
                            },
                            onSignedIn = {
                                navController.navigate("main_tabs") {
                                    popUpTo("welcome") { inclusive = true }
                                }
                            },
                        )
                    }
```

- [ ] **Step 4: Adicionar rotas `signup`, `forgot_password`, `reset_password`**

Logo após `composable("login") { ... }`, adicionar:

```kotlin
                    composable("signup") {
                        val authRepo = remember { AuthRepository() }
                        SignUpScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onSignUp = { email, password, name ->
                                runCatching { authRepo.signUpWithEmail(email, password, name) }
                            },
                            onSignedUp = {
                                navController.popBackStack(route = "login", inclusive = false)
                            },
                        )
                    }

                    composable("forgot_password") {
                        val authRepo = remember { AuthRepository() }
                        ForgotPasswordScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onSendReset = { email -> runCatching { authRepo.sendPasswordResetEmail(email) } },
                        )
                    }

                    composable("reset_password") {
                        val authRepo = remember { AuthRepository() }
                        ResetPasswordScreen(
                            onUpdatePassword = { newPassword -> runCatching { authRepo.updatePassword(newPassword) } },
                            onPasswordUpdated = {
                                navController.navigate("main_tabs") {
                                    popUpTo("welcome") { inclusive = true }
                                }
                            },
                        )
                    }
```

- [ ] **Step 5: Observar `sessionStatus` para redirecionar quando deep link de reset cair**

Dentro do `setContent { ... CompositionLocalProvider { MyApplicationTheme { Surface { ... } } } }`, antes do `NavHost`, adicionar:

```kotlin
                    val sessionStatus by viewModel.sessionStatus.collectAsState()
                    LaunchedEffect(sessionStatus) {
                        val s = sessionStatus
                        if (s is SessionStatus.Authenticated && s.source is io.github.jan.supabase.auth.SessionSource.PasswordRecovery) {
                            navController.navigate("reset_password") {
                                popUpTo("welcome") { inclusive = false }
                            }
                        }
                    }
```

- [ ] **Step 6: Build e validar**

Run: `./gradlew :app:assembleDebug --console=plain 2>&1 | tail -30`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/MainActivity.kt
git commit -m "feat(fase9a): MainActivity pluga rotas auth + handler de password recovery"
```

---

## Task 14: Smoke test manual no emulador

**Files:** nenhum — só executar.

- [ ] **Step 1: Instalar APK debug em um emulador rodando**

Run: `./gradlew :app:installDebug`
Expected: BUILD SUCCESSFUL. APK instalado.

- [ ] **Step 2: Cadastrar conta nova**

1. Abrir app, tocar em "Entrar".
2. Tocar em "Ainda não tem conta? Cadastre-se".
3. Preencher nome, email real (que você consegue acessar), senha 6+ chars.
4. Tocar "Cadastrar".
5. Aparece mensagem "Conta criada! Confira seu email...".
6. Conferir caixa de email → clicar link de confirmação.
7. Voltar pro app → tocar "Voltar para login" → entrar com mesmo email+senha.

Expected: navega pra `main_tabs`. (Pode mostrar tela vazia/com dados antigos do Room — sub-fase 9B vai resolver.)

- [ ] **Step 3: Esqueci minha senha**

1. Logout (botão existente do app — chama o `logout` antigo do DataStore, ok).
2. Voltar pro Login → "Esqueci minha senha".
3. Digitar email cadastrado.
4. Email chega com link `app.rodape://login-callback?...`.
5. Tocar no link no celular → abre o app → cai em `reset_password`.
6. Digitar nova senha → "Redefinir senha" → navega pra main_tabs.

Expected: senha redefinida; conseguir relogar com a nova depois.

- [ ] **Step 4: Google Sign-In**

1. Logout.
2. Login → "Continuar com Google".
3. Sistema mostra picker do Credential Manager.
4. Selecionar conta Google.
5. App navega pra main_tabs.

Expected: sucesso. (Se falhar com "no credentials available", garantir que o emulador tem conta Google configurada em Settings → Accounts.)

- [ ] **Step 5: Commit do registro de validação**

Nenhum código mudou nesta task. Pular commit.

---

## Self-Review (executar após escrever tudo acima)

- [ ] **Cobertura do spec 9A**
  - Dependências (supabase-kt + credentials + ktor + serialization + desugaring) → Task 1, 2 ✓
  - SupabaseClient singleton + RodapeApp + BuildConfig → Task 3, 4, 5 ✓
  - Deep link OAuth no Manifest + handleDeeplinks → Task 6, 7 ✓
  - AuthRepository + GoogleSignInHelper → Task 8, 9 ✓
  - sessionStatus exposto na VM → Task 10 ✓
  - LoginScreen reescrita sem demo → Task 11 ✓
  - SignUp/Forgot/Reset screens → Task 12 ✓
  - MainActivity ligando tudo + password recovery handler → Task 13 ✓
  - Smoke test → Task 14 ✓

- [ ] **Sem placeholders**
  Reler cada Step — todos têm código completo. Sem TBD/TODO.

- [ ] **Consistência de tipos**
  - `onSignInWithEmail: suspend (...) -> Result<Unit>` em LoginScreen ↔ `runCatching { authRepo.signInWithEmail(...) }` em MainActivity → tipos batem.
  - `GoogleSignInHelper.Result` com `idToken` + `rawNonce` ↔ `authRepo.signInWithGoogleIdToken(token.idToken, token.rawNonce)` → bate.

---

## Anti-checklist (NÃO fazer nesta sub-fase)

- ❌ Apagar Room, DataStore, `RodapeRepository`, `seedDatabase`. (9C)
- ❌ Substituir queries Room por chamadas Supabase nos repos de dados. (9B)
- ❌ Upload de capas pro bucket. (9C)
- ❌ Refatorar MainViewModel em VMs por escopo. (fase futura)
- ❌ Esconder tab "Com link" do JoinClubScreen. (9B junto com ClubRepository)
- ❌ Migrar font_scale pra profiles. (9C)

A sub-fase 9A entrega: usuário consegue cadastrar / logar / resetar senha via Supabase Auth real. Resto do app continua funcionando com Room (que vamos arrancar na 9C).
