package com.example.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.remote.RemoteRepository

/**
 * Drena a fila de mutations pendentes em background com requisitos de rede.
 *
 * Triggers:
 *  - Ao iniciar o app (RodapeApp.onCreate enfileira UMA execucao oportunistica)
 *  - Quando o sistema detecta que rede ficou disponivel (Constraints.NetworkType.CONNECTED)
 *  - Apos cada mutation falhar (RemoteRepository pode chamar schedule() pra retry)
 *
 * Estrategia simples nesta versao: cada execucao drena tudo da fila. Mutations
 * que falharem ficam la (markFailed incrementa attempts), e proxima execucao
 * tenta de novo. Sem backoff exponencial — WorkManager ja gerencia retry
 * automaticamente com BackoffPolicy padrao.
 */
class DrainQueueWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            // RemoteRepository e pesado pra construir — instanciar so dentro do worker.
            val repo = RemoteRepository(applicationContext)
            repo.tryDrainPendingQueue()
            Result.success()
        }.getOrElse {
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
