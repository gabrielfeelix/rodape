package com.example.util

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Captura excecoes nao tratadas e persiste em arquivo local.
 *
 * Sem Crashlytics/Sentry/etc: nada sai do device sem autorizacao do usuario.
 * Se o usuario reportar bug, podemos pedir os arquivos via "Exportar logs"
 * em telas futuras.
 *
 * Politica:
 *  - Stack traces vao pra filesDir/crashes/crash-YYYYMMDD-HHmmss.txt
 *  - Mantem ate 10 arquivos (rotacao FIFO), depois apaga o mais antigo
 *  - Nao captura emails/userId/conteudo de usuario — so o stack trace e
 *    info do device (modelo, versao Android, versao do app)
 *  - Depois de logar, repassa pro handler default (deixa app crashar normalmente)
 */
object CrashLogger {

    private const val TAG = "Tramabook/CrashLogger"
    private const val CRASH_DIR = "crashes"
    private const val MAX_FILES = 10

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { writeCrash(appContext, thread, throwable) }
                .onFailure { Log.e(TAG, "Falhou ao persistir crash", it) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun writeCrash(context: Context, thread: Thread, throwable: Throwable) {
        val dir = File(context.filesDir, CRASH_DIR).apply { mkdirs() }
        rotate(dir)

        val ts = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val file = File(dir, "crash-$ts.txt")

        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))

        file.writeText(
            buildString {
                appendLine("=== Tramabook crash ===")
                appendLine("Quando: ${Date()}")
                appendLine("App: ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE}, ${BuildConfig.BUILD_TYPE})")
                appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE} / SDK ${Build.VERSION.SDK_INT})")
                appendLine("Thread: ${thread.name}")
                appendLine()
                appendLine("--- Stack trace ---")
                appendLine(sw.toString())
            }
        )
    }

    /** Mantem no maximo MAX_FILES arquivos, removendo os mais antigos. */
    private fun rotate(dir: File) {
        val files = dir.listFiles()?.sortedBy { it.lastModified() } ?: return
        if (files.size < MAX_FILES) return
        files.take(files.size - MAX_FILES + 1).forEach { it.delete() }
    }

    /** Lista os arquivos de crash existentes (pra futura tela "Exportar logs"). */
    fun crashFiles(context: Context): List<File> {
        val dir = File(context.filesDir, CRASH_DIR)
        if (!dir.exists()) return emptyList()
        return dir.listFiles()?.toList()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
}
