package com.example.data.remote.repo

import com.example.data.remote.SyncEngine
import kotlinx.coroutines.flow.MutableStateFlow

// ============================================================================
// Base dos repos de domínio (F3c).
//
// Açúcar de delegação pra SyncEngine: os corpos movidos do RemoteRepository
// compilam VERBATIM porque supabase/dao/scope/json/syncOnce/markSynced/
// tryRemoteOrEnqueue/notifyLocalMutation/ensureRealtime/stateOf continuam
// resolvendo pelos mesmos nomes. Nenhum repo guarda estado próprio de sync —
// tudo (fila/handlers/realtime/TTL) vive na engine.
// ============================================================================
internal abstract class OfflineFirstRepository(protected val engine: SyncEngine) {

    protected val supabase get() = engine.supabase
    protected val dao get() = engine.dao
    protected val scope get() = engine.scope
    protected val json get() = engine.json

    protected fun markSynced(key: String) = engine.markSynced(key)

    protected suspend fun syncOnce(key: String, ttlMs: Long, block: suspend () -> Unit) =
        engine.syncOnce(key, ttlMs, block)

    protected suspend fun tryRemoteOrEnqueue(
        kind: String,
        payload: String,
        notifyTable: String? = null,
        block: suspend () -> Unit,
    ) = engine.tryRemoteOrEnqueue(kind, payload, notifyTable, block)

    protected fun notifyLocalMutation(table: String) = engine.notifyLocalMutation(table)

    protected fun ensureRealtime(
        table: String,
        filterColumn: String? = null,
        filterValue: String? = null,
        reload: suspend () -> Unit,
    ) = engine.ensureRealtime(table, filterColumn, filterValue, reload)

    protected fun <T> stateOf(initial: T): MutableStateFlow<T> = MutableStateFlow(initial)
}
