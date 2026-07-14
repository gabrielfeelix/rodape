package com.example.data.remote.repo

import com.example.data.model.DbNotification
import kotlinx.coroutines.flow.Flow

/** Notificações in-app (tabela `notifications`). Fatiado da god-repo (F3c). */
interface NotificationRepository {
    suspend fun insertNotification(notification: DbNotification)
    suspend fun markAllNotificationsAsRead(userId: String)
    suspend fun markNotificationAsRead(id: String)
    fun getNotificationsFlow(userId: String): Flow<List<DbNotification>>
}
