package com.example.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Mutation pendente — escrita que foi optimisticamente aplicada no Room mas
 * ainda nao confirmada pelo servidor (HTTP falhou, sem internet, etc).
 *
 * Background worker tenta reenviar quando rede volta.
 *
 * Cada mutation e identificada por:
 *  - kind: tipo da operacao (ex: "insert_comment", "delete_reaction")
 *  - payload: JSON com os params necessarios pra refazer a chamada
 *  - createdAt: pra retry ordering e expiracao
 *  - attempts: contador pra exponential backoff
 *  - lastError: ultima mensagem de erro (debug)
 *
 * Pra simplificar nesta primeira versao, payload e uma string JSON ad-hoc por
 * kind. Em producao maior, seria sealed class + serializer.
 */
@Entity(tableName = "pending_mutations")
data class PendingMutation(
    @PrimaryKey val id: String,
    val kind: String,
    val payload: String,
    val createdAt: Long,
    val attempts: Int = 0,
    val lastError: String? = null,
)

@Dao
interface PendingMutationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(m: PendingMutation)

    @Query("SELECT * FROM pending_mutations ORDER BY createdAt ASC")
    fun allFlow(): Flow<List<PendingMutation>>

    @Query("SELECT * FROM pending_mutations ORDER BY createdAt ASC")
    suspend fun all(): List<PendingMutation>

    @Query("SELECT COUNT(*) FROM pending_mutations")
    fun countFlow(): Flow<Int>

    @Query("DELETE FROM pending_mutations WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE pending_mutations SET attempts = attempts + 1, lastError = :err WHERE id = :id")
    suspend fun markFailed(id: String, err: String?)

    @Query("DELETE FROM pending_mutations")
    suspend fun clear()
}
