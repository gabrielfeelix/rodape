import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

// Le segredos de assinatura do .env raiz (gitignored) com fallback pra
// System.getenv (CI/servidor de build). Senhas NUNCA vao pro BuildConfig
// — sao usadas apenas no SigningConfig em tempo de build do Gradle.
val signingEnv: Properties = Properties().apply {
  val envFile = rootProject.file(".env")
  if (envFile.exists()) {
    envFile.inputStream().use { load(it) }
  }
}

fun signingSecret(key: String): String? =
  signingEnv.getProperty(key)?.takeIf { it.isNotBlank() }
    ?: System.getenv(key)?.takeIf { it.isNotBlank() }

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "app.rodape"
    minSdk = 24
    targetSdk = 36
    versionCode = 2
    versionName = "1.0.1"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = signingSecret("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = signingSecret("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = signingSecret("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      // R8 ativo: encolhe codigo, otimiza, ofusca, e remove recursos nao usados.
      // Regras em proguard-rules.pro mantem o que e carregado por reflexao
      // (Supabase/Ktor/Moshi/Room/WorkManager).
      isCrunchPngs = true
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      // Debug fica sem minify pra iteracao rapida e stack traces limpos.
      isMinifyEnabled = false
      isShrinkResources = false
      applicationIdSuffix = ".debug"
      versionNameSuffix = "-debug"
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }

  // Room schemas exportados pra arquivos versionados — facilita futuras migrations.
  ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
//
// IMPORTANTE: o plugin injeta TODAS as chaves do .env como campos BuildConfig.X,
// e BuildConfig fica embutido no APK (decompilável). Por isso a ignoreList
// abaixo barra explicitamente segredos que jamais podem ir pro cliente.
// Só chaves "publicas/anon" e identificadores são permitidos.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("SUPABASE_SERVICE_ROLE")
  ignoreList.add("SUPABASE_SECRET_KEY")
  ignoreList.add("GOOGLE_WEB_CLIENT_SECRET")
  // Chave do Gemini (AI Studio) NAO e usada por nenhum codigo do app — barrar
  // pra nao vazar no BuildConfig/APK. Rotacionar a chave no console (ela ja
  // esteve embutida em builds anteriores).
  ignoreList.add("GEMINI_API_KEY")
  // Credenciais de administração do Supabase (só usadas em ferramentas locais /
  // migrações, NUNCA no app) — barradas pra jamais irem pro BuildConfig/APK.
  ignoreList.add("SUPABASE_ACCESS_TOKEN")
  ignoreList.add("SUPABASE_DB_PASSWORD")
  ignoreList.add("SUPABASE_DB_URL")
  // Senhas do keystore: jamais devem ir pro BuildConfig (vazariam no APK).
  // O signingConfig le essas direto via System.getenv() em runtime de build.
  ignoreList.add("KEYSTORE_PATH")
  ignoreList.add("STORE_PASSWORD")
  ignoreList.add("KEY_PASSWORD")
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  // Extended traz Book, MenuBook, HowToVote, EventNote, etc. R8 strippa o que
  // nao e usado, entao o impacto no APK final e ~200KB (vale a pena).
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui.google.fonts)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.work.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.moshi.kotlin.codegen)
  "ksp"(libs.androidx.room.compiler)

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

  // --- Material Components (View XML) — necessario pro tema base
  // Theme.Material3.DayNight.NoActionBar usado por dialogs nativos
  // (Credential Manager / Google Sign-In bottom sheet). UI Compose nao usa.
  implementation(libs.material)

  // --- Core library desugaring (java.time em minSdk 24) ---
  coreLibraryDesugaring(libs.desugar.jdk.libs)
}
