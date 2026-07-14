package com.example.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// ============================================================================
// SyncEngine — kernel offline do app (F3b)
//
// Extraído de dentro do RemoteRepository SEM mudança de lógica. Concentra a
// infra que TODOS os domínios compartilham:
//  - SWR com TTL (syncOnce/markSynced)
//  - fila de mutações offline (tryRemoteOrEnqueue + drain + os 25 handlers)
//  - realtime + reload registry (ensureRealtime/notifyLocalMutation)
//
// TRAVA DE DRAIN-SAFETY: os handlers de TODOS os kinds ficam registrados aqui,
// num único mutationHandlers, ANTES de qualquer drain (init). Um kind sem
// handler é descartado como "unknown" = perda de dado do usuário. Por isso
// handlers + drainMutex + isPermanentError + enqueue/kickDrain vivem NESTA
// classe, nunca espalhados por repositórios lazy.
// ============================================================================

/** Escapa caracteres especiais pra string JSON (payloads ad-hoc da fila). */
internal fun String.escapeJson(): String = this
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\n", "\\n")
    .replace("\r", "\\r")
    .replace("\t", "\\t")

// TTLs canonicos: balanceiam frescor vs custo.
internal object Ttl {
    const val FAST = 5_000L      // 5s — chat ativo, votos em rodada aberta
    const val MED = 30_000L      // 30s — listas de clube, livros
    const val SLOW = 300_000L    // 5min — perfis, catalogo de books
}

internal class SyncEngine(
    private val appContext: android.content.Context,
    // Exposto: os repos de domínio (F3c) fazem os SELECTs/mutations com o mesmo
    // client, via engine — superfície documentada no DESIGN-ALVO §SyncEngine.
    val supabase: SupabaseClient,
) {

    val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    // Single source of truth = Room. UI le do Room (instantaneo, offline ok),
    // background sync popula Room a partir do Supabase.
    val dao = com.example.data.db.AppDatabase.get(appContext).rodapeDao()
    private val pendingDao = com.example.data.db.AppDatabase.get(appContext).pendingMutationDao()

    // Scope interno pra refreshes "fire-and-forget" das caches reativas.
    // SupervisorJob: falha de uma corotina nao cancela as outras.
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Apaga o cache local — chamar no logout pra nao vazar dados entre contas.
     *  Antes de limpar, tenta drenar a fila offline uma ultima vez pra nao
     *  descartar silenciosamente mutations que o usuario achou que salvou.
     *  IMPORTANTE: chamar ENQUANTO a sessao ainda existe (antes do signOut),
     *  senao o drain roda deslogado -> 401 -> descarta as mutations. */
    suspend fun clearLocalCache() {
        runCatching { tryDrainPendingQueue() }
        clearLocalCacheNoDrain()
    }

    /** Limpa o cache SEM drenar. Usar na troca de conta (nao drenar as mutations
     *  do usuario A sob a sessao do usuario B) e depois de ja ter drenado. */
    suspend fun clearLocalCacheNoDrain() {
        dao.clearAll()
        pendingDao.clear()
        lastSyncAt.clear()
    }

    // ============================================================
    // SYNC INTELIGENTE (Nivel 2B — SWR com TTL)
    // ============================================================
    //
    // Cada par (resource, key) tem um timestamp de ultimo sync. Quando UI pede
    // um flow, em vez de SEMPRE bater no servidor, checamos se passou o TTL.
    // Se nao passou, deixa Realtime cuidar — economiza HTTP, bateria e bytes.
    //
    // TTL pequeno pra coisas que mudam rapido (comentarios = 10s), grande pra
    // coisas estaticas (perfis, books catalogo = 5min). notifyLocalMutation e
    // eventos Realtime sempre fazem refresh — TTL so afeta o "trigger on view".

    private val lastSyncAt = java.util.concurrent.ConcurrentHashMap<String, Long>()

    /** True se ja sincronizou ha menos de [ttlMs] — caller deve pular. */
    private fun recentlySynced(key: String, ttlMs: Long): Boolean {
        val last = lastSyncAt[key] ?: return false
        return System.currentTimeMillis() - last < ttlMs
    }

    /** Marca um sync como concluido. */
    fun markSynced(key: String) {
        lastSyncAt[key] = System.currentTimeMillis()
    }

    /** Helper que envolve um sync: pula se TTL nao expirou, marca apos sucesso. */
    suspend fun syncOnce(key: String, ttlMs: Long, block: suspend () -> Unit) {
        if (recentlySynced(key, ttlMs)) return
        runCatching { block() }.onSuccess { markSynced(key) }
    }

    // ============================================================
    // WRITE QUEUE OFFLINE (Nivel 3A)
    // ============================================================
    //
    // Quando uma mutation HTTP falha (sem internet, 5xx), gravamos em
    // pending_mutations no Room. O optimistic update local ja foi aplicado,
    // entao UI mostra a mudanca como se fosse permanente. Quando rede volta,
    // tryDrainQueue() reenvia tudo em ordem cronologica.
    //
    // Padrao de uso nos mutations:
    //   runRemote { supabase.from("X").upsert(...) } returningOn(failure) {
    //       enqueue("kind", payload)
    //   }

    private val mutationHandlers =
        java.util.concurrent.ConcurrentHashMap<String, suspend (String) -> Unit>()

    /**
     * Registra um handler que sabe re-executar uma mutation de [kind] a partir
     * do payload serializado. Chamado durante init pra cada tipo de mutation
     * que queremos retentar offline.
     */
    private fun registerHandler(kind: String, handler: suspend (String) -> Unit) {
        mutationHandlers[kind] = handler
    }

    /** Grava uma mutation pendente pra retry futuro. */
    private suspend fun enqueueMutation(kind: String, payload: String) {
        pendingDao.insert(
            com.example.data.db.PendingMutation(
                id = java.util.UUID.randomUUID().toString(),
                kind = kind,
                payload = payload,
                createdAt = System.currentTimeMillis(),
            )
        )
        // RECUPERAÇÃO IMEDIATA em processo: a falha que joga pra fila costuma ser
        // TRANSITÓRIA (rádio frio/conexão fria no 1º toque) e a rede volta em 1-2s.
        // Antes o único gatilho era o WorkManager (não-expedited) — que só drena
        // MINUTOS depois, deixando o "1 alteração aguardando conexão" preso. Agora
        // drenamos já no scope do app, com retries curtos, e o worker vira fallback
        // só pra quando o app está em background/morto.
        kickImmediateDrain()
        com.example.data.sync.DrainQueueWorker.schedule(appContext)
    }

    // Evita spawnar vários loops de drain concorrentes quando o usuário faz várias
    // ações seguidas — um loop só drena TODA a fila a cada passada.
    private val immediateDrainInFlight = java.util.concurrent.atomic.AtomicBoolean(false)

    /** Drena a fila JÁ, em processo, com backoff curto até esvaziar (ou desistir
     *  pro fallback do WorkManager). Não bloqueia a ação: roda no scope do repo. */
    private fun kickImmediateDrain() {
        if (!immediateDrainInFlight.compareAndSet(false, true)) return
        scope.launch {
            try {
                // 0.25s, 0.75s, 1.5s, 3s, 5s — cobre a janela típica de rádio frio.
                val delaysMs = longArrayOf(250, 750, 1500, 3000, 5000)
                for (d in delaysMs) {
                    delay(d)
                    val remaining = runCatching { tryDrainPendingQueue() }.getOrDefault(1)
                    if (remaining == 0) break
                }
            } finally {
                immediateDrainInFlight.set(false)
            }
        }
    }

    /**
     * Envelope idiomatico: tenta executar [block] remoto; se lancar, grava na
     * queue. Use isso em todos os mutations criticos.
     */
    suspend fun tryRemoteOrEnqueue(
        kind: String,
        payload: String,
        notifyTable: String? = null,
        block: suspend () -> Unit,
    ) {
        // notifyTable é notificado SÓ no sucesso: o reload dispara um replace
        // (upsert+prune) e, se rodasse antes do remoto confirmar, podaria a linha
        // otimista ainda-não-sincronizada (item "pisca e some"). O Room já emitiu
        // pro Flow no upsert local, então a UI otimista aparece na hora de qualquer
        // jeito; o notify só reconcilia com o servidor.
        //
        // Retry INLINE curto antes de desistir pra fila: a falha comum é TRANSITÓRIA
        // (rádio frio/conexão fria no 1º toque) e a rede volta em <1s. Com a rede
        // sadia (round-trip ~200ms) a ação "resolve" na hora em vez de mostrar
        // "aguardando conexão". Todos os blocks são idempotentes (upsert/delete),
        // então repetir é seguro. 4xx real (payload/permissão) não retenta inline.
        repeat(3) { attempt ->
            val res = runCatching { block() }
            if (res.isSuccess) {
                if (notifyTable != null) notifyLocalMutation(notifyTable)
                return
            }
            val err = res.exceptionOrNull()
            if (err != null && isPermanentError(err)) {
                enqueueMutation(kind, payload)
                return
            }
            if (attempt < 2) kotlinx.coroutines.delay(350L * (attempt + 1)) // 350ms, 700ms
        }
        enqueueMutation(kind, payload)
    }

    // Depois de MAX_DRAIN_ATTEMPTS a mutation vira "dead-letter" e e descartada,
    // pra nao ficar eternamente presa na fila (poison message) bloqueando o
    // badge de "pendentes" e reprocessando a cada rede.
    private val maxDrainAttempts = 5

    /** Heuristica: erro permanente (4xx = payload/permissao invalida) nao adianta
     *  retentar; erro transitorio (timeout, 5xx, sem rede) sim. Sem depender de
     *  tipos concretos do SDK, inspeciona a mensagem por status 4xx. */
    private fun isPermanentError(t: Throwable): Boolean {
        val msg = (t.message ?: "") + " " + t.toString()
        // 408 (timeout) e 429 (rate limit) sao TRANSITORIOS apesar de 4xx — retenta.
        if (Regex("""\b(408|429)\b""").containsMatchIn(msg)) return false
        // Demais 4xx = payload/permissao invalida: nao adianta retentar.
        return Regex("""\b4\d\d\b""").containsMatchIn(msg)
    }

    // Serializa drains concorrentes (worker + init + forceRefresh + logout) pra
    // nao processar o mesmo item 2x (double markFailed -> dead-letter prematuro).
    private val drainMutex = Mutex()

    /** Drena a fila — chamada quando rede volta ou em sync periodico.
     *  Para na PRIMEIRA falha transitoria pra preservar ordem/dependencias
     *  (ex: insert_reaction depende de insert_comment ainda na fila). */
    suspend fun tryDrainPendingQueue(): Int = drainMutex.withLock {
        val all = pendingDao.all() // ja vem ordenado por createdAt ASC
        for (m in all) {
            val handler = mutationHandlers[m.kind]
            if (handler == null) {
                // kind desconhecido (versao antiga do app): nunca sera processado.
                android.util.Log.w("Rodape", "Descartando mutation de kind desconhecido: ${m.kind}")
                pendingDao.delete(m.id)
                continue
            }
            val result = runCatching { handler(m.payload) }
            if (result.isSuccess) {
                pendingDao.delete(m.id)
                continue
            }
            val err = result.exceptionOrNull()!!
            val permanent = isPermanentError(err)
            val exhausted = m.attempts + 1 >= maxDrainAttempts
            if (permanent || exhausted) {
                android.util.Log.w(
                    "Rodape",
                    "Dead-letter mutation ${m.kind} (permanent=$permanent, attempts=${m.attempts + 1}): ${err.message}"
                )
                pendingDao.delete(m.id)
            } else {
                // Falha transitoria (sem rede / 5xx): marca e PARA. Retentar os
                // itens seguintes agora arriscaria aplicar um dependente antes do
                // seu pai (FK/permissao 4xx -> dead-letter injusto). Proxima drenagem
                // recomeca do inicio quando a rede voltar.
                pendingDao.markFailed(m.id, err.message)
                break
            }
        }
        // Quantos ainda sobraram na fila (0 = tudo sincronizado). Usado pelo drain
        // imediato em processo pra parar assim que a fila esvazia.
        pendingDao.all().size
    }

    /** StateFlow do tamanho da fila pra UI mostrar badge "X pendentes". */
    val pendingMutationsCount: Flow<Int> = pendingDao.countFlow()

    /**
     * Pull-to-refresh: zera os TTLs, tenta drenar a fila offline e refaz o
     * SELECT de todas as caches ativas registradas. Suspende até terminar,
     * pra UI poder segurar o spinner honestamente.
     */
    suspend fun forceRefresh() {
        lastSyncAt.clear()
        runCatching { tryDrainPendingQueue() }
        // Snapshot evita ConcurrentModification se um flow registrar reloader
        // durante o refresh.
        val reloads = tableReloaders.values.toList().flatMap { it.values }
        kotlinx.coroutines.coroutineScope {
            reloads.map { reload ->
                async { runCatching { reload() } }
            }.awaitAll()
        }
    }

    // Helpers de parse do payload da fila. Antes usava-se
    // `element.toString().trim('"')`, que devolve a forma JSON-ENCODADA: um texto
    // com quebra de linha ou aspas era reenviado com `\n`/`\"` literais no banco.
    // jsonPrimitive.content decodifica corretamente os escapes.
    private fun JsonObject.str(key: String): String =
        (this[key] as JsonPrimitive).content
    private fun JsonObject.strOrNull(key: String): String? =
        (this[key] as? JsonPrimitive)?.content

    init {
        // Registra handlers conhecidos. Payload e JSON ad-hoc por kind.
        registerHandler("insert_comment") { json ->
            val obj = this.json.parseToJsonElement(json) as JsonObject
            supabase.from("comments").upsert(
                CommentInsertDto(
                    id = obj.str("id"),
                    chapterId = obj.str("chapterId"),
                    clubId = obj.str("clubId"),
                    userId = obj.str("userId"),
                    texto = obj.str("texto"),
                )
            )
        }
        registerHandler("insert_reaction") { json ->
            val obj = this.json.parseToJsonElement(json) as JsonObject
            supabase.from("reactions").upsert(
                ReactionDto(
                    commentId = obj.str("commentId"),
                    userId = obj.str("userId"),
                    emoji = obj.str("emoji"),
                )
            )
        }
        registerHandler("delete_reaction") { json ->
            val obj = this.json.parseToJsonElement(json) as JsonObject
            supabase.from("reactions").delete {
                filter {
                    eq("comment_id", obj.str("commentId"))
                    eq("user_id", obj.str("userId"))
                    eq("emoji", obj.str("emoji"))
                }
            }
        }
        registerHandler("insert_meeting_rsvp") { json ->
            val obj = this.json.parseToJsonElement(json) as JsonObject
            supabase.from("meeting_rsvps").upsert(
                MeetingRsvpInsertDto(
                    meetingId = obj.str("meetingId"),
                    userId = obj.str("userId"),
                    status = obj.str("status"),
                )
            )
        }
        registerHandler("insert_vote") { json ->
            val obj = this.json.parseToJsonElement(json) as JsonObject
            supabase.from("votes").upsert(
                VoteInsertDto(
                    votingRoundId = obj.str("votingRoundId"),
                    userId = obj.str("userId"),
                    bookId = obj.str("bookId"),
                )
            ) { onConflict = "voting_round_id,user_id" }
        }
        registerHandler("insert_saved_quote") { json ->
            val obj = this.json.parseToJsonElement(json) as JsonObject
            supabase.from("saved_quotes").upsert(
                SavedQuoteInsertDto(
                    id = obj.str("id"),
                    userId = obj.str("userId"),
                    clubId = obj.str("clubId"),
                    bookId = obj.str("bookId"),
                    texto = obj.str("texto"),
                    capituloRef = obj.strOrNull("capituloRef"),
                )
            )
        }
        // P0-2: replay das CRIAÇÕES feitas offline (payload = DTO serializado).
        registerHandler("insert_book") { j ->
            supabase.from("books").upsert(json.decodeFromString<BookInsertDto>(j))
        }
        registerHandler("insert_club_book") { j ->
            supabase.from("club_books").upsert(json.decodeFromString<ClubBookDto>(j))
        }
        registerHandler("insert_book_suggestion") { j ->
            supabase.from("book_suggestions").upsert(json.decodeFromString<BookSuggestionInsertDto>(j))
        }
        registerHandler("insert_meeting") { j ->
            val dto = json.decodeFromString<MeetingInsertDto>(j)
            // FK: garante o livro no servidor antes do encontro (idem caminho online).
            dto.bookId?.let { bid ->
                dao.book(bid)?.let { b -> runCatching { supabase.from("books").upsert(b.toInsertDto()) } }
            }
            supabase.from("meetings").upsert(dto)
        }
        registerHandler("upsert_user_progress") { json ->
            val obj = this.json.parseToJsonElement(json) as JsonObject
            supabase.from("user_progress").upsert(
                UserProgressDto(
                    userId = obj.str("userId"),
                    clubId = obj.str("clubId"),
                    bookId = obj.str("bookId"),
                    currentChapter = obj.str("currentChapter").toInt(),
                )
            )
        }
        registerHandler("upsert_book_rating") { json ->
            val obj = this.json.parseToJsonElement(json) as JsonObject
            supabase.from("book_ratings").upsert(
                BookRatingInsertDto(
                    bookId = obj.str("bookId"),
                    clubId = obj.str("clubId"),
                    userId = obj.str("userId"),
                    stars = obj.str("stars").toInt(),
                    comment = obj.str("comment"),
                )
            )
        }
        registerHandler("insert_book_favorite") { j ->
            val obj = this.json.parseToJsonElement(j) as JsonObject
            supabase.from("book_favorites").upsert(
                BookFavoriteInsertDto(userId = obj.str("userId"), bookId = obj.str("bookId"))
            )
        }
        registerHandler("delete_book_favorite") { j ->
            val obj = this.json.parseToJsonElement(j) as JsonObject
            supabase.from("book_favorites").delete {
                filter {
                    eq("user_id", obj.str("userId"))
                    eq("book_id", obj.str("bookId"))
                }
            }
        }
        registerHandler("upsert_profile") { json ->
            val obj = this.json.parseToJsonElement(json) as JsonObject
            supabase.from("profiles").upsert(
                ProfileUpdateDto(
                    id = obj.str("id"),
                    nome = obj.str("nome"),
                    sobrenome = obj.strOrNull("sobrenome"),
                    avatarKey = obj.str("avatarKey"),
                    pronome = obj.strOrNull("pronome"),
                )
            )
        }
        registerHandler("delete_vote") { json ->
            val obj = this.json.parseToJsonElement(json) as JsonObject
            supabase.from("votes").delete {
                filter {
                    eq("voting_round_id", obj.str("votingRoundId"))
                    eq("user_id", obj.str("userId"))
                    eq("book_id", obj.str("bookId"))
                }
            }
        }
        registerHandler("edit_comment") { json ->
            val obj = this.json.parseToJsonElement(json) as JsonObject
            supabase.from("comments").update({ set("texto", obj.str("texto")) }) {
                filter { eq("id", obj.str("id")) }
            }
        }
        registerHandler("delete_comment") { json ->
            val obj = this.json.parseToJsonElement(json) as JsonObject
            supabase.from("comments").delete { filter { eq("id", obj.str("id")) } }
        }
        registerHandler("remove_comment") { json ->
            val obj = this.json.parseToJsonElement(json) as JsonObject
            supabase.from("comments").update({
                set("removido", true)
                set("removido_por", obj.str("by"))
                set("motivo_remocao", obj.strOrNull("motivo"))
            }) { filter { eq("id", obj.str("id")) } }
        }
        registerHandler("restore_comment") { json ->
            val obj = this.json.parseToJsonElement(json) as JsonObject
            supabase.from("comments").update({
                set("removido", false)
                set("removido_por", JsonNull)
                set("motivo_remocao", JsonNull)
            }) { filter { eq("id", obj.str("id")) } }
        }
        registerHandler("mark_notification_read") { json ->
            val obj = this.json.parseToJsonElement(json) as JsonObject
            supabase.from("notifications").update({ set("lida", true) }) {
                filter { eq("id", obj.str("id")) }
            }
        }
        // ---- moderação (0010) ----
        registerHandler("insert_report") { j ->
            val obj = this.json.parseToJsonElement(j) as JsonObject
            supabase.from("content_reports").upsert(
                ContentReportInsertDto(
                    reporterId = obj.str("reporterId"),
                    clubId = obj.str("clubId"),
                    targetType = obj.str("targetType"),
                    targetId = obj.str("targetId"),
                    targetUserId = obj.str("targetUserId"),
                    motivo = obj.str("motivo"),
                    detalhe = obj.strOrNull("detalhe"),
                )
            ) { onConflict = "reporter_id,target_type,target_id"; ignoreDuplicates = true }
        }
        registerHandler("insert_user_block") { j ->
            val obj = this.json.parseToJsonElement(j) as JsonObject
            supabase.from("user_blocks").upsert(
                UserBlockInsertDto(blockerId = obj.str("blockerId"), blockedId = obj.str("blockedId"))
            ) { ignoreDuplicates = true }
        }
        registerHandler("delete_user_block") { j ->
            val obj = this.json.parseToJsonElement(j) as JsonObject
            supabase.from("user_blocks").delete {
                filter { eq("blocker_id", obj.str("blockerId")); eq("blocked_id", obj.str("blockedId")) }
            }
        }
        registerHandler("moderate_remove") { j ->
            val obj = this.json.parseToJsonElement(j) as JsonObject
            supabase.postgrest.rpc("moderate_remove_content", buildJsonObject {
                put("p_type", obj.str("type"))
                put("p_target_id", obj.str("targetId"))
                put("p_target_user_id", obj.str("targetUserId"))
                put("p_club_id", obj.str("clubId"))
                put("p_motivo", obj.strOrNull("motivo"))
            })
        }

        // Tenta drenar logo no init (caso tenha sobrado fila da sessao anterior).
        scope.launch { runCatching { tryDrainPendingQueue() } }
    }

    // ============================================================
    // REALTIME HELPER
    // ============================================================
    //
    // Estrategia: pra cada (tabela, filtro) registramos UMA subscription
    // postgres_changes que invalida a cache ao receber INSERT/UPDATE/DELETE.
    // Quando invalida, o caller passa um `reload` suspend que refaz o SELECT
    // e atualiza o StateFlow.
    //
    // Por que reload em vez de aplicar o diff? Mais robusto:
    //  - ordenacao server-side e preservada
    //  - tolera mensagens perdidas/duplicadas
    //  - tolera filtros complexos (JOIN via FK) que Realtime nao expressa
    //  - schema-agnostic — funciona pra todas as 22 tabelas igual
    //
    // Custo: 1 GET extra por evento. Aceitavel em volume baixo (clube tem
    // dezenas de membros, nao milhoes). Pra escala maior, futura otimizacao
    // pode aplicar diff diretamente.

    // Tracking: chave (table + filtro) -> Job da subscription ativa.
    // Permite reuso entre coletores e desligamento limpo.
    // ConcurrentHashMap: registrado da main (getters de Flow) e lido de corrotinas
    // em Dispatchers.IO — mapa comum daria ConcurrentModificationException.
    private val realtimeJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()
    // Canais Realtime rastreados por key pra poder desregistrar no close() — antes
    // o close() so cancelava as corotinas, mas os canais ficavam registrados no
    // SupabaseClient singleton e acumulavam a cada recriacao de ViewModel (rotacao).
    private val realtimeChannels =
        java.util.concurrent.ConcurrentHashMap<String, io.github.jan.supabase.realtime.RealtimeChannel>()

    // Reload registry: chave (table) -> mapa (key -> reload()).
    // Antes era um CopyOnWriteArraySet que CRESCIA SEM LIMITE — cada revisita de
    // tela/re-emissao de flatMapLatest adicionava um novo lambda (nunca iguais),
    // e um unico write local virava centenas de reloads (RemoteRepository.kt:1003).
    // Agora indexamos por (table,filter) key: re-registrar SUBSTITUI em vez de acumular.
    private val tableReloaders =
        java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentHashMap<String, suspend () -> Unit>>()

    private fun registerReloader(table: String, key: String, reload: suspend () -> Unit) {
        tableReloaders.getOrPut(table) { java.util.concurrent.ConcurrentHashMap() }[key] = reload
    }

    /** Chamar APOS uma mutation bem-sucedida (insert/update/delete) na tabela.
     *  Dispara reload de todas as caches que escutam essa tabela.
     *  Realtime tambem vai disparar, mas com latencia maior — este e o fast path. */
    fun notifyLocalMutation(table: String) {
        tableReloaders[table]?.values?.forEach { reload ->
            scope.launch { runCatching { reload() } }
        }
    }

    /**
     * Subscribe na tabela [table]. Quando receber qualquer mudanca que case
     * com [filterColumn]=[filterValue] (se fornecidos), chama [reload].
     * Idempotente: chamar 2x com mesma chave reusa a subscription existente.
     *
     * O Job rastreado agora COLETA o flow (via collector.join()), entao seu
     * isActive reflete a subscription viva de verdade. Antes o Job so fazia o
     * setup e morria apos subscribe(), enquanto o coletor real corria detached —
     * o guard `isActive` nunca dedupava e VAZAVA um canal novo a cada chamada.
     */
    fun ensureRealtime(
        table: String,
        filterColumn: String? = null,
        filterValue: String? = null,
        reload: suspend () -> Unit,
    ) {
        val key = "$table:${filterColumn ?: "*"}=${filterValue ?: "*"}"
        // Registra o reloader pro fast-path local (substitui o de mesma key).
        registerReloader(table, key, reload)

        // compute() atomico fecha a race check-then-act: sem ele, dois coletores
        // da mesma key podiam ambos passar o guard e lancar jobs concorrentes,
        // deixando um canal subscrito porem untracked (leak).
        realtimeJobs.compute(key) { _, current ->
            if (current?.isActive == true) return@compute current
            val ch = supabase.channel("rodape-rt-$key")
            realtimeChannels[key] = ch
            scope.launch {
                runCatching {
                    val flow = ch.postgresChangeFlow<PostgresAction>(schema = "public") {
                        this.table = table
                        if (filterColumn != null && filterValue != null) {
                            filter(filterColumn, io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, filterValue)
                        }
                    }
                    val collector = launch {
                        flow.collect { runCatching { reload() } }
                    }
                    ch.subscribe()
                    // Mantem o Job vivo enquanto coletar — liveness = subscription real.
                    collector.join()
                }
            }
        }
    }

    /** Encerra a engine: cancela subscriptions/reloads pendentes. Chamar em
     *  MainViewModel.onCleared() e no fim do worker de drain. Sem isso, os
     *  coletores Realtime e loops de reload rodavam pra sempre (leak). */
    fun close() {
        // Captura os canais ANTES de cancelar o scope, depois desregistra num scope
        // detached (unsubscribe e suspend e o scope proprio ja morreu). Sem isso os
        // canais ficavam registrados no SupabaseClient singleton acumulando.
        val channelsToRemove = realtimeChannels.values.toList()
        realtimeChannels.clear()
        realtimeJobs.clear()
        tableReloaders.clear()
        scope.cancel()
        if (channelsToRemove.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                channelsToRemove.forEach { ch -> runCatching { ch.unsubscribe() } }
            }
        }
    }
}
