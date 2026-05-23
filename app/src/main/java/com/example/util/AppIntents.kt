package com.example.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Abre a página do app na Play Store. Se Play Store não estiver instalada,
 * abre no navegador. Como o app ainda usa applicationId `app.rodape`,
 * usamos esse pacote.
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
