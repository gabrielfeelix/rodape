package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Chapter
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

/**
 * VM da tela de Notificações — primeira tela fatiada do MainViewModel (F5.2).
 *
 * Padrão do F5: `@HiltViewModel` injeta o SessionManager (@Singleton) + a interface de repo
 * fatiada (F3c). Expõe os MESMOS nomes que a tela já lia do god-VM (`notifications`,
 * `currentChapters`, `markAll...`, `markNotificationAsRead`), então o corpo da NotificationsScreen
 * não muda — só o tipo do parâmetro. Bodies verbatim do MainViewModel.
 *
 * `internal` porque injeta SessionManager (internal, por causa do SyncEngine internal).
 */
@HiltViewModel
internal class NotificationsViewModel @Inject constructor(
    private val session: SessionManager,
    private val notifRepo: NotificationRepository,
) : ViewModel() {

    val notifications: StateFlow<List<DbNotification>> = session.currentUserId.flatMapLatest { userId ->
        if (userId != null) notifRepo.getNotificationsFlow(userId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), emptyList())

    /** Capítulos do livro atual — vêm do grafo de sessão (usado p/ resolver deep-link de notif). */
    val currentChapters: StateFlow<List<Chapter>> = session.currentChapters

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            val userId = session.currentUserId.value ?: return@launch
            notifRepo.markAllNotificationsAsRead(userId)
        }
    }

    fun markNotificationAsRead(notifId: String) {
        viewModelScope.launch {
            notifRepo.markNotificationAsRead(notifId)
        }
    }
}
