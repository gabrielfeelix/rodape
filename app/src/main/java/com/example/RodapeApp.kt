package com.example

import android.app.Application
import com.example.data.remote.Supabase
import com.example.data.sync.DrainQueueWorker
import com.example.util.CrashLogger

class RodapeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Captura uncaught exceptions e persiste em filesDir/crashes/ antes
        // de o app morrer. Sem rede, sem terceiros — usuario decide depois
        // se quer exportar/compartilhar.
        CrashLogger.install(this)

        // Toca no singleton pra forcar warm-up (lazy thread-safe).
        Supabase.client

        // Tenta drenar fila de mutations pendentes na primeira oportunidade
        // com rede. Se nao tiver mutations pendentes, e no-op.
        DrainQueueWorker.schedule(this)
    }
}
