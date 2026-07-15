package com.example.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DbNotification
import com.example.data.remote.repo.NotificationRepository
import com.example.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// F5: notificações in-app. Injeta SessionManager (userId) + repo de domínio —
// corpo dos flows/ações movido verbatim do MainViewModel.
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val session: SessionManager,
    private val notificationRepo: NotificationRepository,
) : ViewModel() {

    val notifications: StateFlow<List<DbNotification>> = session.currentUserId.flatMapLatest { userId ->
        if (userId != null) notificationRepo.getNotificationsFlow(userId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), emptyList())

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            val userId = session.currentUserId.value ?: return@launch
            notificationRepo.markAllNotificationsAsRead(userId)
        }
    }

    fun markNotificationAsRead(notifId: String) {
        viewModelScope.launch {
            notificationRepo.markNotificationAsRead(notifId)
        }
    }
}
