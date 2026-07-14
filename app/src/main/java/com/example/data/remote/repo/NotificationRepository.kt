package com.example.data.remote.repo

import com.example.data.model.DbNotification
import com.example.data.remote.NotificationDto
import com.example.data.remote.NotificationInsertDto
import com.example.data.remote.SyncEngine
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

// F3c: notificações in-app (notifications). Corpos movidos VERBATIM do
// RemoteRepository — comportamento idêntico.
interface NotificationRepository {
    suspend fun insertNotification(notification: DbNotification)
    suspend fun markAllNotificationsAsRead(userId: String)
    suspend fun markNotificationAsRead(id: String)
    fun getNotificationsFlow(userId: String): Flow<List<DbNotification>>
}

internal class OfflineFirstNotificationRepository @Inject constructor(
    engine: SyncEngine,
) : OfflineFirstRepository(engine), NotificationRepository {

    override suspend fun insertNotification(notification: DbNotification) {
        runCatching {
            val payloadJson = runCatching { json.parseToJsonElement(notification.payloadJson) }
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
            notifyLocalMutation("notifications")
        }
    }

    override suspend fun markAllNotificationsAsRead(userId: String) {
        // Local-first: some o badge na hora mesmo offline. Antes era remoto-primeiro
        // e a notificacao ficava "nao lida" ate um round-trip bem-sucedido.
        dao.markAllNotificationsRead(userId)
        notifyLocalMutation("notifications")
        runCatching {
            supabase.from("notifications").update({ set("lida", true) }) {
                filter { eq("user_id", userId) }
            }
        }.onFailure { android.util.Log.w("Rodape/Repo", "markAll read remote falhou: ${it.message}") }
    }

    override suspend fun markNotificationAsRead(id: String) {
        dao.markNotificationRead(id)
        val payload = buildJsonObject { put("id", id) }.toString()
        tryRemoteOrEnqueue("mark_notification_read", payload, notifyTable = "notifications") {
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
        scope.launch { runCatching { reload() } }
        ensureRealtime("notifications", filterColumn = "user_id", filterValue = userId, reload = reload)
        return dao.notificationsForUserFlow(userId)
    }
}
