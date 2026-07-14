package com.example.data.remote.repo

import com.example.data.model.UserProgress
import kotlinx.coroutines.flow.Flow

/**
 * Progresso de leitura (tabela `user_progress`).
 *
 * Primeiro repo de domínio fatiado da god-repo RemoteRepository (F3c). A
 * fachada RemoteRepository delega estes 4 métodos pra cá; os VMs (F5) vão
 * injetar esta interface direto via Hilt. Impl offline-first em
 * [OfflineFirstProgressRepository], usando o kernel SyncEngine.
 */
interface ProgressRepository {
    suspend fun insertUserProgress(progress: UserProgress)
    fun getUserProgressFlow(userId: String, clubId: String, bookId: String): Flow<UserProgress?>
    suspend fun getUserProgress(userId: String, clubId: String, bookId: String): UserProgress?
    fun getAllProgressForClubFlow(clubId: String): Flow<List<UserProgress>>
}
