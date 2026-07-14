package com.example.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.remote.SyncEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Drena a fila de mutations pendentes em background com requisitos de rede.
 *
 * Triggers:
 *  - Ao iniciar o app (RodapeApp.onCreate enfileira UMA execucao oportunistica)
 *  - Quando o sistema detecta que rede ficou disponivel (Constraints.NetworkType.CONNECTED)
 *  - Apos cada mutation falhar (a SyncEngine chama schedule() pra retry)
 *
 * Estrategia simples nesta versao: cada execucao drena tudo da fila. Mutations
 * que falharem ficam la (markFailed incrementa attempts), e proxima execucao
 * tenta de novo. Sem backoff exponencial — WorkManager ja gerencia retry
 * automaticamente com BackoffPolicy padrao.
 *
 * F4a: @HiltWorker injeta a SyncEngine @Singleton — antes cada execucao
 * construia (e fechava) um RemoteRepository inteiro so pra drenar. A engine
 * do processo ja tem os 25 handlers registrados no init; NAO chamamos close()
 * porque ela e compartilhada com o resto do app.
 */
@HiltWorker
class DrainQueueWorker @AssistedInject internal constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val engine: SyncEngine,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            engine.tryDrainPendingQueue()
            Result.success()
        } catch (t: Throwable) {
            // Retry com backoff padrao do WorkManager (30s, 1min, 2min, ...)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "rodape-drain-queue"

        /** Enfileira UM trabalho oportunistico (so quando rede disponivel). */
        fun schedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<DrainQueueWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
