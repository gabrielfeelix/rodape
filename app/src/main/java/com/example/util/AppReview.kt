package com.example.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Play In-App Review — mostra o card nativo de avaliação SEM tirar o usuário do
 * app. Disparado num momento positivo (ex.: prompt após engajamento). O Google
 * controla a frequência real; se o fluxo não puder abrir, caímos pra página da
 * Play Store como fallback.
 */
fun requestInAppReview(context: Context, onDone: () -> Unit = {}) {
    val activity = context.findActivity()
    if (activity == null) {
        openPlayStorePage(context)
        onDone()
        return
    }
    val manager = ReviewManagerFactory.create(context)
    manager.requestReviewFlow().addOnCompleteListener { task ->
        if (task.isSuccessful) {
            manager.launchReviewFlow(activity, task.result)
                .addOnCompleteListener { onDone() }
        } else {
            // Quota/erro do fluxo in-app → fallback pra loja.
            openPlayStorePage(context)
            onDone()
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
