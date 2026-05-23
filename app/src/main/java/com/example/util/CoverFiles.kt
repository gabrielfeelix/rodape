package com.example.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Helpers para persistir capas de livros cadastrados manualmente.
 *
 * Estratégia: tudo vai pra `context.filesDir/covers/{uuid}.jpg` (privado ao app).
 * O `Book.coverUrl` guarda o path absoluto prefixado com `file://` — Coil aceita
 * isso direto e renderiza igual a uma URL https://.
 */
object CoverFiles {

    private fun coversDir(context: Context): File {
        val dir = File(context.filesDir, "covers")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Copia bytes de um Uri (Photo Picker, galeria) pra dentro do filesDir.
     * Retorna o caminho `file://...` que pode ir direto pro Book.coverUrl.
     */
    fun saveFromUri(context: Context, uri: Uri): String? {
        return runCatching {
            val resolver: ContentResolver = context.contentResolver
            val target = File(coversDir(context), "${UUID.randomUUID()}.jpg")
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            "file://${target.absolutePath}"
        }.getOrNull()
    }

    /**
     * Salva um arquivo File diretamente (vindo do CameraX ImageCapture).
     * Move o arquivo origem (se for temporário) ou copia caso não dê pra mover.
     */
    fun saveFromFile(context: Context, source: File): String? {
        return runCatching {
            val target = File(coversDir(context), "${UUID.randomUUID()}.jpg")
            if (source.renameTo(target)) {
                "file://${target.absolutePath}"
            } else {
                source.inputStream().use { input ->
                    FileOutputStream(target).use { output ->
                        input.copyTo(output)
                    }
                }
                "file://${target.absolutePath}"
            }
        }.getOrNull()
    }
}
