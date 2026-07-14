package com.example.data.remote.repo

import com.example.data.db.RodapeDao
import com.example.data.model.DbNotification
import com.example.data.remote.NotificationDto
import com.example.data.remote.NotificationInsertDto
import com.example.data.remote.SyncEngine
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class OfflineFirstNotificationRepository @Inject constructor(
    private val engine: SyncEngine,
    private val supabase: SupabaseClient,
    private val dao: RodapeDao,
) : NotificationRepository {

    override suspend fun insertNotification(notification: DbNotification) {
        runCatching {
            val payloadJson = runCatching { engine.json.parseToJsonElement(notification.payloadJson) }
                .getOrElse { JsonObject(emptyMap()) }
            supabase.from("notifications").upsert(
                NotificationInsertDto(
                    id = notification.id,
                    userId = notification.userId,
                    clubId = notification.clubId.ifBlank { null },
                    tipo = notification.tipo,
                    payload = payloadJson,
                    lida = notification.lida,
                )
            )
            dao.upsertNotifications(listOf(notification))
            engine.notifyLocalMutation("notifications")
        }
    }

    override suspend fun markAllNotificationsAsRead(userId: String) {
        // Local-first: some o badge na hora mesmo offline. Antes era remoto-primeiro
        // e a notificacao ficava "nao lida" ate um round-trip bem-sucedido.
        dao.markAllNotificationsRead(userId)
        engine.notifyLocalMutation("notifications")
        runCatching {
            supabase.from("notifications").update({ set("lida", true) }) {
                filter { eq("user_id", userId) }
            }
        }.onFailure { android.util.Log.w("Rodape/Repo", "markAll read remote falhou: ${it.message}") }
    }

    override suspend fun markNotificationAsRead(id: String) {
        dao.markNotificationRead(id)
        val payload = buildJsonObject { put("id", id) }.toString()
        engine.tryRemoteOrEnqueue("mark_notification_read", payload, notifyTable = "notifications") {
            supabase.from("notifications").update({ set("lida", true) }) {
                filter { eq("id", id) }
            }
        }
    }

    override fun getNotificationsFlow(userId: String): Flow<List<DbNotification>> {
        val reload: suspend () -> Unit = {
            runCatching {
                val list = supabase.from("notifications").select {
                    filter { eq("user_id", userId) }
                    order("created_at", Order.DESCENDING)
                }.decodeList<NotificationDto>().map { it.toDomain() }
                dao.replaceNotificationsForUser(userId, list)
            }
        }
        engine.scope.launch { runCatching { reload() } }
        engine.ensureRealtime("notifications", filterColumn = "user_id", filterValue = userId, reload = reload)
        return dao.notificationsForUserFlow(userId)
    }
}
