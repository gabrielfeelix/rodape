package com.example.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.SyncEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// F5: estado de sync/offline da UI (badge de pendências + pull-to-refresh).
// Fala direto com a SyncEngine — superfície pública dela pra VMs (DESIGN-ALVO).
@HiltViewModel
class SyncViewModel @Inject internal constructor(
    private val engine: SyncEngine,
) : ViewModel() {

    /** Tamanho da fila offline — UI mostra "alterações aguardando conexão". */
    val pendingMutationsCount: StateFlow<Int> = engine.pendingMutationsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), 0)

    /** Pull-to-refresh: força re-sync de todas as caches ativas, ignorando TTL. */
    fun forceRefresh(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { engine.forceRefresh() }
            onDone()
        }
    }
}
