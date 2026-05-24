# =============================================================================
# Rodape — Regras de R8/ProGuard pra release
# =============================================================================
# R8 esta ativo (isMinifyEnabled = true). Sem essas regras o app crasha em
# release por reflexao: Moshi codegen, kotlinx.serialization, Supabase-kt, Ktor.
# =============================================================================

# Mantem linhas de stack trace utilizaveis pos-obfuscacao
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Anotacoes que o R8 precisa preservar (Moshi/Serialization/Room)
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# -----------------------------------------------------------------------------
# Kotlinx Serialization (Supabase usa @Serializable em todos os DTOs)
# -----------------------------------------------------------------------------
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Mantem classes geradas (*$$serializer) e companion objects pra serializadores
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    static **$$serializer INSTANCE;
}

-if @kotlinx.serialization.Serializable class **
-keepclasseswithmembers class <1>$$serializer {
    static <1>$$serializer INSTANCE;
    *** serializer(...);
}

-keepclasseswithmembernames class * {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,allowobfuscation,allowshrinking class kotlinx.serialization.KSerializer

# -----------------------------------------------------------------------------
# Moshi (Retrofit usado pra Open Library)
# -----------------------------------------------------------------------------
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class *
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <fields>;
}
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}

# Moshi codegen — adaptadores gerados
-keep class **JsonAdapter { *; }
-keep class **JsonAdapter$* { *; }

# -----------------------------------------------------------------------------
# Retrofit + OkHttp
# -----------------------------------------------------------------------------
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# -----------------------------------------------------------------------------
# Ktor (Supabase HTTP)
# -----------------------------------------------------------------------------
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn io.ktor.**
-dontwarn kotlinx.atomicfu.**

# Engine CIO em particular usa reflexao pra carregar engine
-keep class io.ktor.client.engine.cio.** { *; }

# -----------------------------------------------------------------------------
# Supabase-kt
# -----------------------------------------------------------------------------
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**

# -----------------------------------------------------------------------------
# Room
# -----------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn androidx.room.paging.**

# -----------------------------------------------------------------------------
# WorkManager — Workers carregados por reflexao via classname
# -----------------------------------------------------------------------------
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker
-keep class * extends androidx.work.CoroutineWorker
-keep public class * extends androidx.work.RxWorker
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}

# -----------------------------------------------------------------------------
# Credential Manager (Google Sign-In)
# -----------------------------------------------------------------------------
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn com.google.android.libraries.identity.googleid.**

# -----------------------------------------------------------------------------
# Coil
# -----------------------------------------------------------------------------
-keep class coil.** { *; }
-dontwarn coil.**

# -----------------------------------------------------------------------------
# Compose
# -----------------------------------------------------------------------------
-keep,allowobfuscation,allowshrinking class androidx.compose.runtime.Composer
