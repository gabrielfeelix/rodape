package com.example

import android.app.Application
import com.example.data.remote.Supabase
import com.example.data.sync.DrainQueueWorker
import com.example.util.CrashLogger
import com.google.firebase.crashlytics.FirebaseCrashlytics

class RodapeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Crashlytics: captura crashes remotamente pra podermos consertar
        // (Android Vitals só mostra crashes que o usuário reporta na Play).
        // Desligado em DEBUG pra não poluir o painel com crashes de desenvolvimento.
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG

        // Captura uncaught exceptions e persiste em filesDir/crashes/ antes
        // de o app morrer (complementa o Crashlytics; serve pro "Exportar logs"
        // offline). Encadeia com o handler do Crashlytics, então ambos capturam.
        CrashLogger.install(this)

        // Toca no singleton pra forcar warm-up (lazy thread-safe).
        Supabase.client

        // Tenta drenar fila de mutations pendentes na primeira oportunidade
        // com rede. Se nao tiver mutations pendentes, e no-op.
        DrainQueueWorker.schedule(this)
    }
}
