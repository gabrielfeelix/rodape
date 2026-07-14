package com.example.data.remote.repo

import com.example.data.model.User
import kotlinx.coroutines.flow.Flow

/** Perfis de usuário (tabela `profiles`). Fatiado da god-repo (F3c). */
interface UserRepository {
    fun getUserFlow(userId: String): Flow<User?>
    suspend fun getUser(userId: String): User?
    fun getAllUsersFlow(): Flow<List<User>>
    suspend fun insertUser(user: User)
    suspend fun updateFontScale(userId: String, scale: Float)
}
