package com.example.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

/**
 * Abre a página do app na Play Store. Se Play Store não estiver instalada,
 * abre no navegador. Usa o `packageName` em runtime (resolve pra `app.rodape`
 * em release), sem hardcode.
 */
fun openPlayStorePage(context: Context, packageName: String = context.packageName) {
    val marketUri = Uri.parse("market://details?id=$packageName")
    val intent = Intent(Intent.ACTION_VIEW, marketUri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        val webUri = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}

/** URLs públicas dos documentos legais (hospedados no repositório). */
const val URL_TERMOS = "https://github.com/gabrielfeelix/rodape/blob/master/docs/legal/termos-de-uso.md"
const val URL_PRIVACIDADE = "https://github.com/gabrielfeelix/rodape/blob/master/docs/privacy/privacy-policy.md"

/** Abre uma URL no navegador. Fallback silencioso se não houver navegador. */
fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Sem navegador — fallback silencioso.
    }
}

/**
 * Abre o cliente de email padrão com destino, assunto e corpo pré-preenchidos.
 */
fun openEmailFeedback(
    context: Context,
    to: String = "feedback@rodape.app",
    subject: String = "Feedback do Rodapé",
    body: String = ""
) {
    val uri = Uri.parse(
        "mailto:$to?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}"
    )
    val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Sem app de email — fallback é silencioso
    }
}

/**
 * Abre um email de "reportar bug" pré-preenchido com info do device e o trecho
 * inicial do último crash local (se houver). Assim o usuário nos manda um relato
 * útil sem esforço. Corpo truncado pra caber num mailto.
 */
fun openBugReport(context: Context) {
    val device = "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
    val ultimoCrash = CrashLogger.crashFiles(context).firstOrNull()
        ?.let { runCatching { it.readText().take(1500) }.getOrNull() }
    val body = buildString {
        appendLine("Descreva o que aconteceu:")
        appendLine()
        appendLine()
        appendLine("————————")
        appendLine("Device: $device")
        if (ultimoCrash != null) {
            appendLine()
            appendLine("Último erro registrado:")
            appendLine(ultimoCrash)
        }
    }
    openEmailFeedback(context, subject = "Bug no Rodapé", body = body)
}

/**
 * Compartilha texto livre via share sheet do Android (WhatsApp, email, SMS,
 * Telegram, Drive, Notes, etc). Usado pra exportar frases salvas.
 */
fun shareTextContent(context: Context, subject: String, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val chooser = Intent.createChooser(intent, subject).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}

/**
 * Compartilha o código de convite do clube via share sheet do Android
 * (WhatsApp, SMS, Telegram, etc).
 */
fun shareClubInvite(context: Context, clubName: String, codigo: String) {
    val text = buildString {
        append("📚 Quer ler comigo no Rodapé?\n\n")
        append("Entra no clube \"$clubName\" com o código:\n\n")
        append("$codigo\n\n")
        append("(É só baixar o Rodapé e tocar em \"Entrar num clube\".)")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Convite pro clube $clubName")
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val chooser = Intent.createChooser(intent, "Compartilhar convite").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}
