package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Compartilha um bitmap como PNG via FileProvider (3.8 — frase como imagem).
 * O arquivo vai pro cache (`cacheDir/shared_quotes/`) — o sistema pode limpar
 * quando quiser; o content URI concede leitura só ao app escolhido no chooser.
 */
suspend fun shareBitmapAsPng(
    context: Context,
    bitmap: Bitmap,
    fileKey: String,
    chooserTitle: String = "Compartilhar frase",
) {
    val uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "shared_quotes").apply { mkdirs() }
        // Nome estável por frase: recompartilhar sobrescreve em vez de acumular.
        val file = File(dir, "frase-${fileKey.filter { it.isLetterOrDigit() }}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}
