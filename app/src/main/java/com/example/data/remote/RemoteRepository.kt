package com.example.data.remote

import com.example.BuildConfig
import com.example.data.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

// ============================================================================
// DTOs — mapeamento snake_case (Postgres) <-> camelCase (Kotlin domain)
//
// Cada DTO representa uma row do banco. Conversao bidirecional pra/de o
// `data class` antigo do Room (`com.example.data.model.*`), que a UI ja
// usa em toda parte. Isso evita reescrever as 22 telas Compose.
//
// Convencoes:
//  - Long epoch ms (domain) <-> ISO 8601 string (timestamptz no banco).
//  - "" no domain <-> null no banco em campos opcionais (e vice-versa).
//  - Enums Postgres viajam como String (postgrest manda o nome do label).
// ============================================================================

private val isoFmt: java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME

private fun Long.toIso(): String =
    java.time.Instant.ofEpochMilli(this)
        .atOffset(java.time.ZoneOffset.UTC)
        .format(isoFmt)

private fun String?.fromIso(): Long {
    if (this.isNullOrBlank()) return 0L
    return runCatching {
        java.time.OffsetDateTime.parse(this).toInstant().toEpochMilli()
    }.getOrElse { 0L }
}

// ---- profiles ----
@Serializable
private data class ProfileDto(
    val id: String,
    val nome: String,
    val sobrenome: String? = null,
    @SerialName("avatar_key") val avatarKey: String = "preset:leitor",
    @SerialName("font_scale") val fontScale: Double = 1.0,
) {
    fun toDomain(): User = User(
        id = id,
        nome = listOfNotNull(nome, sobrenome).joinToString(" ").trim().ifBlank { nome },
        email = "", // email mora em auth.users; UI le de supabaseEmail
        avatarUrl = avatarKey,
    )
}

// ---- clubs ----
@Serializable
private data class ClubDto(
    val id: String,
    val nome: String,
    val descricao: String? = null,
    val codigo: String,
    val cor: String = "0",
    val privacidade: String = "convidados",
    @SerialName("criador_id") val criadorId: String,
    val arquivado: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
) {
    fun toDomain(): Club = Club(
        id = id,
        nome = nome,
        descricao = descricao ?: "",
        codigo = codigo,
        cor = cor,
        privacidade = privacidade,
        criadorId = criadorId,
        criadoEm = createdAt.fromIso(),
        arquivado = arquivado,
    )
}

// ---- club_members ----
@Serializable
private data class ClubMemberDto(
    @SerialName("club_id") val clubId: String,
    @SerialName("user_id") val userId: String,
    val papel: String = "member",
    @SerialName("entrou_em") val entrouEm: String? = null,
) {
    fun toDomain(): ClubMember = ClubMember(
        clubId = clubId,
        userId = userId,
        papel = papel,
        entrouEm = entrouEm.fromIso(),
    )
}

// ---- books ----
@Serializable
private data class BookDto(
    val id: String,
    val title: String,
    val author: String,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("openlibrary_id") val openlibraryId: String? = null,
    val isbn: String? = null,
    @SerialName("is_manual") val isManual: Boolean = false,
    @SerialName("total_paginas") val totalPaginas: Int? = null,
    val editora: String? = null,
    @SerialName("ano_publicacao") val anoPublicacao: Int? = null,
    val idioma: String = "pt",
) {
    fun toDomain(): Book = Book(
        id = id,
        title = title,
        author = author,
        coverUrl = coverUrl ?: "",
        openlibraryId = openlibraryId ?: "",
        isbn = isbn ?: "",
        isManual = isManual,
        totalPaginas = totalPaginas,
        editora = editora,
        anoPublicacao = anoPublicacao,
        idioma = idioma,
    )
}

private fun Book.toInsertDto(): BookInsertDto = BookInsertDto(
    id = id, title = title, author = author,
    coverUrl = coverUrl.ifBlank { null },
    openlibraryId = openlibraryId.ifBlank { null },
    isbn = isbn.ifBlank { null },
    isManual = isManual,
    totalPaginas = totalPaginas,
    editora = editora,
    anoPublicacao = anoPublicacao,
    idioma = idioma ?: "pt",
)

/** Versao de insercao sem `created_at`/`created_by` (servidor preenche). */
@Serializable
private data class BookInsertDto(
    val id: String,
    val title: String,
    val author: String,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("openlibrary_id") val openlibraryId: String? = null,
    val isbn: String? = null,
    @SerialName("is_manual") val isManual: Boolean = false,
    @SerialName("total_paginas") val totalPaginas: Int? = null,
    val editora: String? = null,
    @SerialName("ano_publicacao") val anoPublicacao: Int? = null,
    val idioma: String = "pt",
)

// ---- club_books ----
@Serializable
private data class ClubBookDto(
    @SerialName("club_id") val clubId: String,
    @SerialName("book_id") val bookId: String,
    val status: String,
    val ordem: Int = 0,
    @SerialName("data_encontro") val dataEncontro: String? = null,
) {
    fun toDomain(): ClubBook = ClubBook(
        clubId = clubId,
        bookId = bookId,
        status = status,
        ordem = ordem,
        dataEncontro = if (dataEncontro != null) dataEncontro.fromIso() else null,
    )
}

private fun ClubBook.toDto() = ClubBookDto(
    clubId = clubId, bookId = bookId, status = status, ordem = ordem,
    dataEncontro = dataEncontro?.toIso(),
)

// ---- chapters ----
@Serializable
private data class ChapterDto(
    val id: String,
    @SerialName("book_id") val bookId: String,
    val numero: Int,
    val titulo: String,
) {
    fun toDomain(): Chapter = Chapter(id, bookId, numero, titulo)
}

private fun Chapter.toDto() = ChapterDto(id, bookId, numero, titulo)

// ---- user_progress ----
@Serializable
private data class UserProgressDto(
    @SerialName("user_id") val userId: String,
    @SerialName("club_id") val clubId: String,
    @SerialName("book_id") val bookId: String,
    @SerialName("current_chapter") val currentChapter: Int = 0,
) {
    fun toDomain(): UserProgress = UserProgress(userId, clubId, bookId, currentChapter)
}

private fun UserProgress.toDto() = UserProgressDto(userId, clubId, bookId, currentChapter)

// ---- comments ----
@Serializable
private data class CommentDto(
    val id: String,
    @SerialName("chapter_id") val chapterId: String,
    @SerialName("club_id") val clubId: String,
    @SerialName("user_id") val userId: String,
    val texto: String,
    val removido: Boolean = false,
    @SerialName("removido_por") val removidoPor: String? = null,
    @SerialName("motivo_remocao") val motivoRemocao: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
) {
    fun toDomain(): Comment = Comment(
        id = id, chapterId = chapterId, clubId = clubId, userId = userId,
        texto = texto, criadoEm = createdAt.fromIso(),
        removido = removido, removidoPor = removidoPor, motivoRemocao = motivoRemocao,
    )
}

@Serializable
private data class CommentInsertDto(
    val id: String,
    @SerialName("chapter_id") val chapterId: String,
    @SerialName("club_id") val clubId: String,
    @SerialName("user_id") val userId: String,
    val texto: String,
)

// ---- reactions ----
@Serializable
private data class ReactionDto(
    @SerialName("comment_id") val commentId: String,
    @SerialName("user_id") val userId: String,
    val emoji: String,
) {
    fun toDomain(): Reaction = Reaction(commentId, userId, emoji)
}

private fun Reaction.toDto() = ReactionDto(commentId, userId, emoji)

// ---- votes ----
@Serializable
private data class VoteDto(
    @SerialName("voting_round_id") val votingRoundId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("book_id") val bookId: String,
    @SerialName("voted_at") val votedAt: String? = null,
) {
    fun toDomain(): Vote = Vote(
        clubBookId = bookId,
        userId = userId,
        votedAt = votedAt.fromIso(),
        votingRoundId = votingRoundId,
    )
}

@Serializable
private data class VoteInsertDto(
    @SerialName("voting_round_id") val votingRoundId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("book_id") val bookId: String,
)

// ---- voting_rounds ----
@Serializable
private data class VotingRoundDto(
    val id: String,
    @SerialName("club_id") val clubId: String,
    @SerialName("criado_por") val criadoPor: String,
    @SerialName("aberta_em") val abertaEm: String? = null,
    @SerialName("fecha_em") val fechaEm: String,
    @SerialName("n_livros") val nLivros: Int = 1,
    val cadencia: String = "unica",
    val status: String = "aberta",
    val vencedores: JsonElement? = null,
) {
    fun toDomain(): VotingRound = VotingRound(
        id = id,
        clubId = clubId,
        criadoPor = criadoPor,
        abertaEm = abertaEm.fromIso(),
        fechaEm = fechaEm.fromIso(),
        nLivros = nLivros,
        cadencia = cadencia,
        status = status,
        vencedoresJson = vencedores?.toString() ?: "[]",
    )
}

@Serializable
private data class VotingRoundInsertDto(
    val id: String,
    @SerialName("club_id") val clubId: String,
    @SerialName("criado_por") val criadoPor: String,
    @SerialName("fecha_em") val fechaEm: String,
    @SerialName("n_livros") val nLivros: Int,
    val cadencia: String,
    val status: String = "aberta",
)

// ---- meetings ----
@Serializable
private data class MeetingDto(
    val id: String,
    @SerialName("club_id") val clubId: String,
    val data: String,
    val local: String? = null,
    val agenda: String? = null,
    @SerialName("book_id") val bookId: String? = null,
    @SerialName("chapter_start") val chapterStart: Int? = null,
    @SerialName("chapter_end") val chapterEnd: Int? = null,
    val status: String = "agendado",
) {
    fun toDomain(): Meeting {
        // Banco guarda 1 timestamptz; domain quebra em (data, hora) strings.
        val (dataStr, horaStr) = formatMeetingDateTime(data)
        return Meeting(
            id = id, clubId = clubId,
            data = dataStr, hora = horaStr, local = local ?: "",
            agenda = agenda ?: "", bookId = bookId,
            chapterStart = chapterStart, chapterEnd = chapterEnd, status = status,
        )
    }
}

@Serializable
private data class MeetingInsertDto(
    val id: String,
    @SerialName("club_id") val clubId: String,
    val data: String,
    val local: String? = null,
    val agenda: String? = null,
    @SerialName("book_id") val bookId: String? = null,
    @SerialName("chapter_start") val chapterStart: Int? = null,
    @SerialName("chapter_end") val chapterEnd: Int? = null,
    val status: String = "agendado",
)

/** "2026-05-25T19:00:00+00:00" -> ("DOMINGO, 25 DE MAIO DE 2026", "19:00"). */
private fun formatMeetingDateTime(iso: String): Pair<String, String> {
    return runCatching {
        val odt = java.time.OffsetDateTime.parse(iso)
        val dia = odt.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale("pt", "BR")).uppercase()
        val data = "${dia}, ${odt.dayOfMonth} DE ${odt.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale("pt", "BR")).uppercase()} DE ${odt.year}"
        val hora = "%02d:%02d".format(odt.hour, odt.minute)
        data to hora
    }.getOrElse { iso to "" }
}

// ---- meeting_rsvps ----
@Serializable
private data class MeetingRsvpDto(
    @SerialName("meeting_id") val meetingId: String,
    @SerialName("user_id") val userId: String,
    val status: String,
) {
    fun toDomain(): MeetingRsvp = MeetingRsvp(meetingId, userId, normalizeRsvp(status))
}

/** Banco usa enum `rsvp_status` ('vou'|'talvez'|'nao_vou'); UI usa "Vou"|"Talvez"|"Não vou". */
private fun normalizeRsvp(s: String): String = when (s.lowercase()) {
    "vou" -> "Vou"
    "talvez" -> "Talvez"
    "nao_vou", "não vou" -> "Não vou"
    else -> s
}

private fun rsvpToEnum(s: String): String = when (s.lowercase().replace("ã", "a")) {
    "vou" -> "vou"
    "talvez" -> "talvez"
    "nao vou", "naovou", "nao_vou" -> "nao_vou"
    else -> "talvez"
}

@Serializable
private data class MeetingRsvpInsertDto(
    @SerialName("meeting_id") val meetingId: String,
    @SerialName("user_id") val userId: String,
    val status: String,
)

// ---- notifications ----
@Serializable
private data class NotificationDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("club_id") val clubId: String? = null,
    val tipo: String,
    val payload: JsonElement = JsonObject(emptyMap()),
    val lida: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
) {
    fun toDomain(): DbNotification = DbNotification(
        id = id, userId = userId, clubId = clubId ?: "",
        tipo = tipo, payloadJson = payload.toString(),
        lida = lida, criadoEm = createdAt.fromIso(),
    )
}

@Serializable
private data class NotificationInsertDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("club_id") val clubId: String? = null,
    val tipo: String,
    val payload: JsonElement,
    val lida: Boolean = false,
)

// ---- saved_quotes ----
@Serializable
private data class SavedQuoteDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("club_id") val clubId: String,
    @SerialName("book_id") val bookId: String,
    val texto: String,
    @SerialName("capitulo_ref") val capituloRef: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
) {
    fun toDomain(): SavedQuote = SavedQuote(
        id = id, userId = userId, clubId = clubId, bookId = bookId,
        texto = texto, capituloRef = capituloRef ?: "",
        criadoEm = createdAt.fromIso(),
    )
}

@Serializable
private data class SavedQuoteInsertDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("club_id") val clubId: String,
    @SerialName("book_id") val bookId: String,
    val texto: String,
    @SerialName("capitulo_ref") val capituloRef: String? = null,
)

// ---- book_summaries ----
@Serializable
private data class BookSummaryDto(
    @SerialName("book_id") val bookId: String,
    @SerialName("club_id") val clubId: String,
    val texto: String,
    @SerialName("last_editor_id") val lastEditorId: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    fun toDomain(): BookSummary = BookSummary(
        bookId = bookId, clubId = clubId, texto = texto,
        lastEditorId = lastEditorId ?: "",
        updatedAt = updatedAt.fromIso(),
    )
}

@Serializable
private data class BookSummaryInsertDto(
    @SerialName("book_id") val bookId: String,
    @SerialName("club_id") val clubId: String,
    val texto: String,
    @SerialName("last_editor_id") val lastEditorId: String? = null,
)

// ---- book_ratings ----
@Serializable
private data class BookRatingDto(
    @SerialName("book_id") val bookId: String,
    @SerialName("club_id") val clubId: String,
    @SerialName("user_id") val userId: String,
    val stars: Int,
    val comment: String = "",
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    fun toDomain(): BookRating = BookRating(
        bookId = bookId, clubId = clubId, userId = userId,
        stars = stars, comment = comment, updatedAt = updatedAt.fromIso(),
    )
}

@Serializable
private data class BookRatingInsertDto(
    @SerialName("book_id") val bookId: String,
    @SerialName("club_id") val clubId: String,
    @SerialName("user_id") val userId: String,
    val stars: Int,
    val comment: String = "",
)

// ---- book_suggestions ----
@Serializable
private data class BookSuggestionDto(
    val id: String,
    @SerialName("club_id") val clubId: String,
    @SerialName("book_id") val bookId: String,
    @SerialName("voting_round_id") val votingRoundId: String? = null,
    @SerialName("sugerido_por") val sugeridoPor: String,
    val justificativa: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
) {
    fun toDomain(): BookSuggestion = BookSuggestion(
        id = id, clubId = clubId, bookId = bookId,
        suggestedByUserId = sugeridoPor,
        justificativa = justificativa ?: "",
        criadoEm = createdAt.fromIso(),
    )
}

@Serializable
private data class BookSuggestionInsertDto(
    val id: String,
    @SerialName("club_id") val clubId: String,
    @SerialName("book_id") val bookId: String,
    @SerialName("sugerido_por") val sugeridoPor: String,
    val justificativa: String? = null,
)

// ---- meeting_patterns ----
@Serializable
private data class MeetingPatternDto(
    val id: String,
    @SerialName("club_id") val clubId: String,
    @SerialName("dia_semana") val diaSemana: Int,
    val hora: String,
    val local: String? = null,
    @SerialName("agenda_template") val agendaTemplate: String? = null,
    val ativo: Boolean = true,
    @SerialName("tipo_recorrencia") val tipoRecorrencia: String = "semanal",
    @SerialName("valor_recorrencia") val valorRecorrencia: Int = 0,
) {
    fun toDomain(): MeetingPattern = MeetingPattern(
        id = id, clubId = clubId, diaSemana = diaSemana, hora = hora,
        local = local ?: "", agendaTemplate = agendaTemplate ?: "",
        ativo = ativo, tipoRecorrencia = tipoRecorrencia,
        valorRecorrencia = valorRecorrencia,
    )
}

@Serializable
private data class MeetingPatternInsertDto(
    val id: String,
    @SerialName("club_id") val clubId: String,
    @SerialName("dia_semana") val diaSemana: Int,
    val hora: String,
    val local: String? = null,
    @SerialName("agenda_template") val agendaTemplate: String? = null,
    val ativo: Boolean = true,
    @SerialName("tipo_recorrencia") val tipoRecorrencia: String = "semanal",
    @SerialName("valor_recorrencia") val valorRecorrencia: Int = 0,
)

// ---- member_removals ----
@Serializable
private data class MemberRemovalDto(
    val id: String,
    @SerialName("club_id") val clubId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("removed_by") val removedBy: String,
    val motivo: String? = null,
    @SerialName("removed_at") val removedAt: String? = null,
) {
    fun toDomain(): MemberRemoval = MemberRemoval(
        id = id, clubId = clubId, userId = userId,
        removedByUserId = removedBy,
        motivo = motivo ?: "",
        removedAt = removedAt.fromIso(),
    )
}

// ---- meeting_minutes ----
@Serializable
private data class MeetingMinutesDto(
    @SerialName("meeting_id") val meetingId: String,
    val texto: String,
    @SerialName("last_editor_id") val lastEditorId: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    fun toDomain(): MeetingMinutes = MeetingMinutes(
        meetingId = meetingId, texto = texto,
        lastEditorId = lastEditorId ?: "",
        updatedAt = updatedAt.fromIso(),
    )
}

@Serializable
private data class MeetingMinutesInsertDto(
    @SerialName("meeting_id") val meetingId: String,
    val texto: String,
    @SerialName("last_editor_id") val lastEditorId: String? = null,
)

// ---- meeting_notes ----
@Serializable
private data class MeetingNoteDto(
    @SerialName("meeting_id") val meetingId: String,
    @SerialName("user_id") val userId: String,
    val texto: String,
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    fun toDomain(): MeetingNote = MeetingNote(
        meetingId = meetingId, userId = userId, texto = texto,
        updatedAt = updatedAt.fromIso(),
    )
}

@Serializable
private data class MeetingNoteInsertDto(
    @SerialName("meeting_id") val meetingId: String,
    @SerialName("user_id") val userId: String,
    val texto: String,
)

// ============================================================================
// RemoteRepository
//
// Substitui RodapeRepository (Room). Mantem EXATAMENTE as mesmas assinaturas
// publicas pra reduzir o impacto no MainViewModel a uma so troca de instancia.
//
// Estrategia:
//  - Leituras one-shot: `suspend fun ... = supabase.from("X").select { filter { ... } }`.
//  - Leituras reativas (Flow): polling via MutableStateFlow inicializada com select.
//    Pra MVP nao usamos Realtime WebSocket — UI re-le ao chamar a acao.
//    Trabalho de Realtime fica pra fase futura (decisao: simplicidade > vivacidade).
//  - Mutacoes: `supabase.from("X").upsert(...)` ou `.delete { filter { ... } }`.
//  - Operacoes privilegiadas: RPC SECURITY DEFINER (`create_club`, `join_club_with_code`,
//    `promote_member`, etc.).
//
// Erros: propagam excecao. UI ja tem fallback com `?: emptyList()` em quase tudo;
// onde nao tem, a tela fica vazia momentaneamente — sem crash.
// ============================================================================

class RemoteRepository(
    private val appContext: android.content.Context,
    private val supabase: SupabaseClient = Supabase.client,
) {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    // Single source of truth = Room. UI le do Room (instantaneo, offline ok),
    // background sync popula Room a partir do Supabase.
    private val dao = com.example.data.db.AppDatabase.get(appContext).rodapeDao()
    private val pendingDao = com.example.data.db.AppDatabase.get(appContext).pendingMutationDao()

    // Scope interno do repo pra refreshes "fire-and-forget" das caches reativas.
    // SupervisorJob: falha de uma corotina nao cancela as outras.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Apaga o cache local — chamar no logout pra nao vazar dados entre contas. */
    suspend fun clearLocalCache() {
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
    private fun markSynced(key: String) {
        lastSyncAt[key] = System.currentTimeMillis()
    }

    /** Helper que envolve um sync: pula se TTL nao expirou, marca apos sucesso. */
    private suspend fun syncOnce(key: String, ttlMs: Long, block: suspend () -> Unit) {
        if (recentlySynced(key, ttlMs)) return
        runCatching { block() }.onSuccess { markSynced(key) }
    }

    // TTLs canonicos: balanceiam frescor vs custo.
    private object Ttl {
        const val FAST = 5_000L      // 5s — chat ativo, votos em rodada aberta
        const val MED = 30_000L      // 30s — listas de clube, livros
        const val SLOW = 300_000L    // 5min — perfis, catalogo de books
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

    private val mutationHandlers = mutableMapOf<String, suspend (String) -> Unit>()

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
        // Pede pro WorkManager tentar drenar assim que houver rede.
        com.example.data.sync.DrainQueueWorker.schedule(appContext)
    }

    /**
     * Envelope idiomatico: tenta executar [block] remoto; se lancar, grava na
     * queue. Use isso em todos os mutations criticos.
     */
    private suspend fun tryRemoteOrEnqueue(
        kind: String,
        payload: String,
        block: suspend () -> Unit,
    ) {
        runCatching { block() }.onFailure { enqueueMutation(kind, payload) }
    }

    /** Drena a fila — chamada quando rede volta ou em sync periodico. */
    suspend fun tryDrainPendingQueue() {
        val all = pendingDao.all()
        for (m in all) {
            val handler = mutationHandlers[m.kind] ?: continue
            runCatching { handler(m.payload) }
                .onSuccess { pendingDao.delete(m.id) }
                .onFailure { pendingDao.markFailed(m.id, it.message) }
        }
    }

    /** StateFlow do tamanho da fila pra UI mostrar badge "X pendentes". */
    val pendingMutationsCount: Flow<Int> = pendingDao.countFlow()

    init {
        // Registra handlers conhecidos. Payload e JSON ad-hoc por kind.
        registerHandler("insert_comment") { json ->
            val obj = this.json.parseToJsonElement(json) as JsonObject
            supabase.from("comments").upsert(
                CommentInsertDto(
                    id = obj["id"]!!.toString().trim('"'),
                    chapterId = obj["chapterId"]!!.toString().trim('"'),
                    clubId = obj["clubId"]!!.toString().trim('"'),
                    userId = obj["userId"]!!.toString().trim('"'),
                    texto = obj["texto"]!!.toString().trim('"'),
                )
            )
        }
        registerHandler("insert_reaction") { json ->
            val obj = this.json.parseToJsonElement(json) as JsonObject
            supabase.from("reactions").upsert(
                ReactionDto(
                    commentId = obj["commentId"]!!.toString().trim('"'),
                    userId = obj["userId"]!!.toString().trim('"'),
                    emoji = obj["emoji"]!!.toString().trim('"'),
                )
            )
        }
        registerHandler("delete_reaction") { json ->
            val obj = this.json.parseToJsonElement(json) as JsonObject
            supabase.from("reactions").delete {
                filter {
                    eq("comment_id", obj["commentId"]!!.toString().trim('"'))
                    eq("user_id", obj["userId"]!!.toString().trim('"'))
                    eq("emoji", obj["emoji"]!!.toString().trim('"'))
                }
            }
        }
        registerHandler("insert_meeting_rsvp") { json ->
            val obj = this.json.parseToJsonElement(json) as JsonObject
            supabase.from("meeting_rsvps").upsert(
                MeetingRsvpInsertDto(
                    meetingId = obj["meetingId"]!!.toString().trim('"'),
                    userId = obj["userId"]!!.toString().trim('"'),
                    status = obj["status"]!!.toString().trim('"'),
                )
            )
        }
        registerHandler("insert_vote") { json ->
            val obj = this.json.parseToJsonElement(json) as JsonObject
            supabase.from("votes").upsert(
                VoteInsertDto(
                    votingRoundId = obj["votingRoundId"]!!.toString().trim('"'),
                    userId = obj["userId"]!!.toString().trim('"'),
                    bookId = obj["bookId"]!!.toString().trim('"'),
                )
            )
        }
        registerHandler("insert_saved_quote") { json ->
            val obj = this.json.parseToJsonElement(json) as JsonObject
            supabase.from("saved_quotes").upsert(
                SavedQuoteInsertDto(
                    id = obj["id"]!!.toString().trim('"'),
                    userId = obj["userId"]!!.toString().trim('"'),
                    clubId = obj["clubId"]!!.toString().trim('"'),
                    bookId = obj["bookId"]!!.toString().trim('"'),
                    texto = obj["texto"]!!.toString().trim('"'),
                    capituloRef = obj["capituloRef"]?.toString()?.trim('"'),
                )
            )
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
    private val realtimeJobs = mutableMapOf<String, Job>()

    // Reload registry: chave (table) -> set de reload() ativos.
    // Apos uma mutation local (insert/update/delete), chamamos
    // notifyLocalMutation(table) que dispara TODOS os reloads dessa tabela
    // imediatamente — sem esperar Realtime WebSocket. Optimistic-lite:
    // a mudanca aparece em ~200ms (RTT) em vez de ~500ms (websocket roundtrip).
    private val tableReloaders = mutableMapOf<String, MutableSet<suspend () -> Unit>>()

    private fun registerReloader(table: String, reload: suspend () -> Unit) {
        tableReloaders.getOrPut(table) { mutableSetOf() }.add(reload)
    }

    /** Chamar APOS uma mutation bem-sucedida (insert/update/delete) na tabela.
     *  Dispara reload de todas as caches que escutam essa tabela.
     *  Realtime tambem vai disparar, mas com latencia maior — este e o fast path. */
    private fun notifyLocalMutation(table: String) {
        tableReloaders[table]?.forEach { reload ->
            scope.launch { runCatching { reload() } }
        }
    }

    /**
     * Subscribe na tabela [table]. Quando receber qualquer mudanca que case
     * com [filterColumn]=[filterValue] (se fornecidos), chama [reload].
     * Idempotente: chamar 2x com mesma chave reusa a subscription existente.
     */
    private fun ensureRealtime(
        table: String,
        filterColumn: String? = null,
        filterValue: String? = null,
        reload: suspend () -> Unit,
    ) {
        // Sempre registra o reloader pra fast-path local (mutations imediatas).
        registerReloader(table, reload)

        val key = "$table:${filterColumn ?: "*"}=${filterValue ?: "*"}"
        if (realtimeJobs[key]?.isActive == true) return

        val job = scope.launch {
            runCatching {
                val ch = supabase.channel("rodape-rt-$key")
                val flow = ch.postgresChangeFlow<PostgresAction>(schema = "public") {
                    this.table = table
                    if (filterColumn != null && filterValue != null) {
                        filter(filterColumn, io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, filterValue)
                    }
                }
                flow.onEach { _ ->
                    // Qualquer evento dispara reload — simples e robusto.
                    runCatching { reload() }
                }.launchIn(scope)
                ch.subscribe()
            }
        }
        realtimeJobs[key] = job
    }

    // ----------------------- Caches reativas -----------------------
    // Polling-based: cada Flow tem uma cache MutableStateFlow que e refreshada
    // sob demanda. Acoes de mutacao chamam refresh() depois pra UI atualizar.

    private fun <T> stateOf(initial: T): MutableStateFlow<T> = MutableStateFlow(initial)

    // ============================================================
    // USERS / PROFILES
    // ============================================================

    fun getUserFlow(userId: String): Flow<User?> {
        // Reload manual (via realtime/mutation) ignora TTL — sempre re-busca.
        val reload: suspend () -> Unit = { syncUser(userId); markSynced("user:$userId") }
        // Trigger on view: respeita TTL.
        scope.launch { syncOnce("user:$userId", Ttl.SLOW) { syncUser(userId) } }
        ensureRealtime("profiles", filterColumn = "id", filterValue = userId, reload = reload)
        return dao.userFlow(userId)
    }

    private suspend fun syncUser(userId: String) {
        val u = runCatching {
            supabase.from("profiles").select {
                filter { eq("id", userId) }
                limit(1)
            }.decodeSingleOrNull<ProfileDto>()?.toDomain()
        }.getOrNull()
        if (u != null) dao.upsertUser(u)
    }

    suspend fun getUser(userId: String): User? {
        // Snapshot: prefere Room (rapido), busca remoto se nao tem.
        dao.user(userId)?.let { return it }
        syncUser(userId)
        return dao.user(userId)
    }

    /** Nao usado hoje (RLS limita visibilidade); mantido como stub vazio. */
    fun getAllUsersFlow(): Flow<List<User>> = kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun insertUser(user: User) {
        // Apenas atualiza profile do USUARIO LOGADO (RLS bloqueia outros).
        // Para criar profile de outro usuario o trigger handle_new_user e quem faz.
        runCatching {
            val partes = user.nome.trim().split(" ", limit = 2)
            val nome = partes.firstOrNull()?.ifBlank { user.nome } ?: user.nome
            val sobrenome = if (partes.size > 1) partes[1].trim().ifBlank { null } else null
            supabase.from("profiles").upsert(
                ProfileUpdateDto(
                    id = user.id,
                    nome = nome,
                    sobrenome = sobrenome,
                    avatarKey = user.avatarUrl.ifBlank { "preset:leitor" },
                )
            )
            notifyLocalMutation("profiles")
        }
    }

    @Serializable
    private data class ProfileUpdateDto(
        val id: String,
        val nome: String,
        val sobrenome: String? = null,
        @SerialName("avatar_key") val avatarKey: String,
    )

    suspend fun updateFontScale(userId: String, scale: Float) {
        runCatching {
            supabase.from("profiles").update({ set("font_scale", scale) }) {
                filter { eq("id", userId) }
            }
        }
    }

    // ============================================================
    // CLUBS
    // ============================================================

    fun getClubFlow(clubId: String): Flow<Club?> {
        val reload: suspend () -> Unit = { syncClub(clubId); markSynced("club:$clubId") }
        scope.launch { syncOnce("club:$clubId", Ttl.MED) { syncClub(clubId) } }
        ensureRealtime("clubs", filterColumn = "id", filterValue = clubId, reload = reload)
        return dao.clubFlow(clubId)
    }

    private suspend fun syncClub(clubId: String) {
        val c = runCatching {
            supabase.from("clubs").select {
                filter { eq("id", clubId) }
                limit(1)
            }.decodeSingleOrNull<ClubDto>()?.toDomain()
        }.getOrNull()
        if (c != null) dao.upsertClub(c)
    }

    suspend fun getClub(clubId: String): Club? {
        dao.club(clubId)?.let { return it }
        syncClub(clubId)
        return dao.club(clubId)
    }

    suspend fun getClubByCodigo(codigo: String): Club? = runCatching {
        // Pra entrar em clube por codigo, busca direto no servidor (clube pode
        // nao estar no cache local porque user nao e membro ainda).
        val club = supabase.from("clubs").select {
            filter { eq("codigo", codigo) }
            limit(1)
        }.decodeSingleOrNull<ClubDto>()?.toDomain()
        // Nao cacheamos — RLS deve impedir acesso se nao for membro.
        club
    }.getOrNull()

    fun getClubsForUser(userId: String): Flow<List<Club>> {
        val reload: suspend () -> Unit = { syncClubsForUser(); markSynced("clubs:user:$userId") }
        scope.launch { syncOnce("clubs:user:$userId", Ttl.MED) { syncClubsForUser() } }
        ensureRealtime("club_members", filterColumn = "user_id", filterValue = userId, reload = reload)
        ensureRealtime("clubs", reload = reload)
        return dao.clubsActiveFlow()
    }

    private suspend fun syncClubsForUser() {
        runCatching {
            val list = supabase.from("clubs").select().decodeList<ClubDto>().map { it.toDomain() }
            // Substitui: o que sumiu do servidor (saiu do clube) some local tambem.
            dao.upsertClubs(list)
            dao.pruneClubsExcept(list.map { it.id })
        }
    }

    suspend fun getClubsForUserList(userId: String): List<Club> = runCatching {
        supabase.from("clubs").select {
            filter { eq("arquivado", false) }
        }.decodeList<ClubDto>().map { it.toDomain() }
    }.getOrDefault(emptyList())

    suspend fun insertClub(club: Club) {
        // App nao deve mais inserir clube diretamente — usa RPC create_club.
        // Mantido como no-op pra preservar interface.
    }

    /** RPC: create_club. Retorna o UUID do novo clube.
     *
     *  A funcao Postgres `create_club` esta declarada como `RETURNS clubs`, ou
     *  seja, devolve a ROW INTEIRA do novo clube como JSON (nao so o UUID).
     *  Precisamos decodificar como JsonObject e extrair o campo `id`. Bug
     *  anterior tratava o JSON inteiro como string -> filtros HTTP montavam
     *  URL invalida (?id=eq.{json...}) -> 400 Bad Request silencioso e clube
     *  ficava "perdido" pro cliente apesar de criado no banco. */
    suspend fun createClubViaRpc(
        nome: String,
        descricao: String?,
        cor: String,
        privacidade: String,
    ): String {
        val resp = supabase.postgrest.rpc(
            function = "create_club",
            parameters = buildJsonObject {
                put("p_nome", nome)
                put("p_descricao", descricao ?: "")
                put("p_cor", cor)
                put("p_privacidade", privacidade)
            },
        ).data
        return extractIdFromRpcRow(resp)
            ?: error("create_club: resposta sem campo id: $resp")
    }

    /** RPC: join_club_with_code. Retorna ROW de club_members (campo `club_id`).
     *  Mesmo padrao do create_club — extrai o id apos parsear. */
    suspend fun joinClubWithCodeViaRpc(codigo: String): String? = runCatching {
        val resp = supabase.postgrest.rpc(
            function = "join_club_with_code",
            parameters = buildJsonObject { put("p_codigo", codigo.uppercase().trim()) }
        ).data
        extractFieldFromRpcRow(resp, "club_id")
    }.getOrNull()

    /** Parse helper: pega `id` da row retornada por uma RPC `RETURNS tabela`.
     *  Resposta vem como JSON: '{"id":"uuid","nome":"...",...}'. Em casos
     *  raros vem como array de 1 elemento: '[{"id":"uuid",...}]'. Tolera ambos. */
    private fun extractIdFromRpcRow(raw: String): String? =
        extractFieldFromRpcRow(raw, "id")

    private fun extractFieldFromRpcRow(raw: String, field: String): String? {
        if (raw.isBlank() || raw == "null") return null
        return runCatching {
            val element = json.parseToJsonElement(raw)
            val obj = when (element) {
                is JsonObject -> element
                is kotlinx.serialization.json.JsonArray -> element.firstOrNull() as? JsonObject
                else -> null
            }
            obj?.get(field)?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull }
        }.getOrNull()
    }

    suspend fun leaveClubViaRpc(clubId: String) {
        runCatching {
            supabase.postgrest.rpc("leave_club", buildJsonObject { put("p_club_id", clubId) })
        }
    }

    suspend fun regenerateInviteCodeViaRpc(clubId: String): String = runCatching {
        val resp = supabase.postgrest.rpc(
            "regenerate_invite_code",
            buildJsonObject { put("p_club_id", clubId) }
        ).data
        resp.trim('"', ' ', '\n')
    }.getOrElse { "" }

    suspend fun promoteMemberViaRpc(clubId: String, targetUserId: String) {
        runCatching {
            supabase.postgrest.rpc("promote_member", buildJsonObject {
                put("p_club_id", clubId); put("p_target_user_id", targetUserId)
            })
        }
    }

    suspend fun demoteAdminViaRpc(clubId: String, targetUserId: String) {
        runCatching {
            supabase.postgrest.rpc("demote_admin", buildJsonObject {
                put("p_club_id", clubId); put("p_target_user_id", targetUserId)
            })
        }
    }

    suspend fun transferSuperAdminViaRpc(clubId: String, toUserId: String) {
        runCatching {
            supabase.postgrest.rpc("transfer_super_admin", buildJsonObject {
                put("p_club_id", clubId); put("p_target_user_id", toUserId)
            })
        }
    }

    suspend fun removeMemberViaRpc(clubId: String, targetUserId: String, motivo: String?) {
        runCatching {
            supabase.postgrest.rpc("remove_member", buildJsonObject {
                put("p_club_id", clubId)
                put("p_target_user_id", targetUserId)
                put("p_motivo", motivo ?: "")
            })
        }
    }

    suspend fun closeVotingRoundViaRpc(roundId: String) {
        runCatching {
            supabase.postgrest.rpc("close_voting_round", buildJsonObject {
                put("p_round_id", roundId)
            })
        }
    }

    // ============================================================
    // CLUB MEMBERS
    // ============================================================

    suspend fun insertClubMember(member: ClubMember) {
        // Nao inserimos membro diretamente — RPCs (create_club / join_club_with_code)
        // ja cuidam. Manter no-op preserva interface.
    }

    fun getClubMembersFlow(clubId: String): Flow<List<User>> {
        val key = "members:$clubId"
        val reload: suspend () -> Unit = { syncClubMembers(clubId); markSynced(key) }
        scope.launch { syncOnce(key, Ttl.MED) { syncClubMembers(clubId) } }
        ensureRealtime("club_members", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.memberUsersInClubFlow(clubId)
    }

    private suspend fun syncClubMembers(clubId: String) {
        runCatching {
            val rows = supabase.from("club_members").select(Columns.raw("user_id, papel, entrou_em, profiles!inner(*)")) {
                filter { eq("club_id", clubId) }
            }.decodeList<JoinMemberProfile>()
            val users = rows.map { it.profile.toDomain() }
            val members = rows.map { row ->
                ClubMember(
                    clubId = clubId,
                    userId = row.userId,
                    papel = row.papel ?: "member",
                    entrouEm = row.entrouEm.fromIso(),
                )
            }
            dao.replaceMembersInClub(clubId, members, users)
        }
    }

    @Serializable
    private data class JoinMemberProfile(
        @SerialName("user_id") val userId: String,
        val papel: String? = null,
        @SerialName("entrou_em") val entrouEm: String? = null,
        @SerialName("profiles") val profile: ProfileDto,
    )

    suspend fun getClubMember(clubId: String, userId: String): ClubMember? {
        dao.member(clubId, userId)?.let { return it }
        return runCatching {
            supabase.from("club_members").select {
                filter {
                    eq("club_id", clubId)
                    eq("user_id", userId)
                }
                limit(1)
            }.decodeSingleOrNull<ClubMemberDto>()?.toDomain()
                ?.also { dao.upsertMembers(listOf(it)) }
        }.getOrNull()
    }

    suspend fun getClubMembersListOrderedByJoin(clubId: String): List<ClubMember> = runCatching {
        supabase.from("club_members").select {
            filter { eq("club_id", clubId) }
            order("entrou_em", Order.ASCENDING)
        }.decodeList<ClubMemberDto>().map { it.toDomain() }
    }.getOrDefault(emptyList())

    fun getClubMembersRawFlow(clubId: String): Flow<List<ClubMember>> {
        val reload: suspend () -> Unit = { syncClubMembers(clubId) }
        scope.launch { runCatching { reload() } }
        ensureRealtime("club_members", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.membersInClubFlow(clubId)
    }

    suspend fun updateMemberPapel(clubId: String, userId: String, papel: String) {
        // RLS bloqueia update direto de papel — use as RPCs *ViaRpc.
        // Caminho legado: o MainViewModel chama updateMemberPapel diretamente em alguns
        // fluxos (transferir super_admin). Mantemos um update direto que so funciona se
        // o usuario tiver permissao via RLS (caller_role super_admin).
        runCatching {
            supabase.from("club_members").update({ set("papel", papel) }) {
                filter {
                    eq("club_id", clubId)
                    eq("user_id", userId)
                }
            }
            notifyLocalMutation("club_members")
        }
    }

    suspend fun deleteClubMember(clubId: String, userId: String) {
        runCatching {
            supabase.from("club_members").delete {
                filter {
                    eq("club_id", clubId)
                    eq("user_id", userId)
                }
            }
            notifyLocalMutation("club_members")
        }
    }

    suspend fun insertMemberRemoval(removal: MemberRemoval) {
        // RPC remove_member ja cuida disso. Mantido no-op.
    }

    fun getMemberRemovalsForClubFlow(clubId: String): Flow<List<MemberRemoval>> {
        scope.launch {
            runCatching {
                val list = supabase.from("member_removals").select {
                    filter { eq("club_id", clubId) }
                    order("removed_at", Order.DESCENDING)
                }.decodeList<MemberRemovalDto>().map { it.toDomain() }
                dao.upsertMemberRemovals(list)
            }
        }
        return dao.memberRemovalsForClubFlow(clubId)
    }

    // ============================================================
    // CLUB ADMIN
    // ============================================================

    suspend fun updateClubInfo(clubId: String, nome: String, descricao: String, cor: String, privacidade: String) {
        runCatching {
            supabase.from("clubs").update({
                set("nome", nome)
                set("descricao", descricao)
                set("cor", cor)
                set("privacidade", privacidade)
            }) { filter { eq("id", clubId) } }
        }
    }

    suspend fun updateClubCodigo(clubId: String, codigo: String) {
        // Use a RPC regenerateInviteCodeViaRpc em vez deste path.
        runCatching {
            supabase.from("clubs").update({ set("codigo", codigo) }) {
                filter { eq("id", clubId) }
            }
        }
    }

    suspend fun updateClubArquivado(clubId: String, arquivado: Boolean) {
        runCatching {
            supabase.from("clubs").update({ set("arquivado", arquivado) }) {
                filter { eq("id", clubId) }
            }
        }
    }

    fun getArchivedClubsForUserFlow(userId: String): Flow<List<Club>> {
        scope.launch {
            runCatching {
                val list = supabase.from("clubs").select {
                    filter { eq("arquivado", true) }
                }.decodeList<ClubDto>().map { it.toDomain() }
                dao.upsertClubs(list)
            }
        }
        return dao.clubsArchivedFlow()
    }

    // ============================================================
    // BOOKS / CLUB_BOOKS
    // ============================================================

    suspend fun insertBook(book: Book) {
        // Optimistic local-first: escreve no Room ANTES de tentar Supabase pra
        // UI nunca ficar "fantasma" (livro sumiu so porque rede falhou ou rate
        // limit do Supabase respondeu 429). Se o remoto falhar, loga e segue —
        // a proxima sync vai reconciliar.
        dao.upsertBook(book)
        notifyLocalMutation("books")
        runCatching { supabase.from("books").upsert(book.toInsertDto()) }
            .onFailure { android.util.Log.w("Rodape/Repo", "insertBook remote falhou (livro existe local): ${it.message}") }
    }

    /**
     * Sobe bytes da capa pro bucket `book-covers` no path `<clubId>/<bookId>.jpg`.
     * Retorna URL pra usar em `books.cover_url`.
     *
     * Bucket e privado — geramos signed URL com expiracao longa (1 ano) e a guardamos
     * no banco. Quando expirar, regeneramos. Pra clubes ativos isso significa que
     * a URL sempre esta valida na pratica.
     *
     * Path por clube garante isolamento via RLS: so members do clube podem
     * ler/escrever ali (policy book_covers_*_members).
     */
    suspend fun uploadBookCover(clubId: String, bookId: String, bytes: ByteArray): String? = runCatching {
        val path = "$clubId/$bookId.jpg"
        val bucket = supabase.storage.from("book-covers")
        // Upload sobreescreve se ja existir (caso usuario troque a capa).
        bucket.upload(path, bytes) {
            upsert = true
            contentType = io.ktor.http.ContentType.Image.JPEG
        }
        // Signed URL valida por 1 ano (max permitido pelo Supabase Storage).
        val signedUrl = bucket.createSignedUrl(path, kotlin.time.Duration.parse("365d"))
        // signed URL ja vem com prefixo do servidor
        "${BuildConfig.SUPABASE_URL}$signedUrl"
    }.getOrNull()

    suspend fun insertClubBook(clubBook: ClubBook) {
        // Optimistic local-first (mesmo motivo de insertBook).
        dao.upsertClubBook(clubBook)
        notifyLocalMutation("club_books")
        runCatching { supabase.from("club_books").upsert(clubBook.toDto()) }
            .onFailure { android.util.Log.w("Rodape/Repo", "insertClubBook remote falhou: ${it.message}") }
    }

    fun getBookByStatusFlow(clubId: String, status: String): Flow<List<Book>> {
        val key = "clubBooks:$clubId"
        val reload: suspend () -> Unit = { syncClubBooks(clubId); markSynced(key) }
        scope.launch { syncOnce(key, Ttl.MED) { syncClubBooks(clubId) } }
        ensureRealtime("club_books", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.booksByStatusFlow(clubId, status)
    }

    private suspend fun syncClubBooks(clubId: String) {
        runCatching {
            val rows = supabase.from("club_books").select(Columns.raw("book_id, status, ordem, data_encontro, books!inner(*)")) {
                filter { eq("club_id", clubId) }
            }.decodeList<JoinClubBookFull>()
            val books = rows.map { it.book.toDomain() }
            val clubBooks = rows.map { row ->
                ClubBook(
                    clubId = clubId,
                    bookId = row.bookId,
                    status = row.status,
                    ordem = row.ordem,
                    dataEncontro = row.dataEncontro?.fromIso(),
                )
            }
            dao.replaceClubBooksInClub(clubId, clubBooks, books)
        }
    }

    @Serializable
    private data class JoinClubBookFull(
        @SerialName("book_id") val bookId: String,
        val status: String,
        val ordem: Int = 0,
        @SerialName("data_encontro") val dataEncontro: String? = null,
        @SerialName("books") val book: BookDto,
    )

    fun getClubBooksFlow(clubId: String): Flow<List<Book>> {
        val key = "clubBooks:$clubId"
        val reload: suspend () -> Unit = { syncClubBooks(clubId); markSynced(key) }
        scope.launch { syncOnce(key, Ttl.MED) { syncClubBooks(clubId) } }
        ensureRealtime("club_books", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.clubBooksFlow(clubId)
    }

    suspend fun getClubBookStatus(clubId: String, bookId: String): String? = runCatching {
        supabase.from("club_books").select(Columns.raw("status")) {
            filter {
                eq("club_id", clubId)
                eq("book_id", bookId)
            }
            limit(1)
        }.decodeSingleOrNull<StatusOnlyDto>()?.status
    }.getOrNull()

    @Serializable
    private data class StatusOnlyDto(val status: String)

    suspend fun getBook(id: String): Book? {
        dao.book(id)?.let { return it }
        return runCatching {
            supabase.from("books").select {
                filter { eq("id", id) }
                limit(1)
            }.decodeSingleOrNull<BookDto>()?.toDomain()
                ?.also { dao.upsertBook(it) }
        }.getOrNull()
    }

    suspend fun updateClubBookStatus(clubId: String, bookId: String, status: String) {
        runCatching {
            supabase.from("club_books").update({ set("status", status) }) {
                filter {
                    eq("club_id", clubId)
                    eq("book_id", bookId)
                }
            }
            // Cache local sera atualizado via notifyLocalMutation -> syncClubBooks.
            notifyLocalMutation("club_books")
        }
    }

    suspend fun updateClubBookMeetingDate(clubId: String, bookId: String, dataEncontro: Long?) {
        runCatching {
            supabase.from("club_books").update({
                set("data_encontro", dataEncontro?.toIso())
            }) {
                filter {
                    eq("club_id", clubId)
                    eq("book_id", bookId)
                }
            }
            notifyLocalMutation("club_books")
        }
    }

    fun getClubBooksByStatusFlow(clubId: String, status: String): Flow<List<ClubBook>> {
        val reload: suspend () -> Unit = { syncClubBooks(clubId) }
        scope.launch { runCatching { reload() } }
        ensureRealtime("club_books", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.clubBooksByStatusFlow(clubId, status)
    }

    // Helper antigo abaixo removido — `syncClubBooks` cuida de tudo via Room.
    @Suppress("UNUSED")
    private fun _oldClubBooksByStatusFlow(clubId: String, status: String): Flow<List<ClubBook>> {
        val flow = stateOf<List<ClubBook>>(emptyList())
        scope.launch {
            flow.value = runCatching {
                supabase.from("club_books").select {
                    filter {
                        eq("club_id", clubId)
                        eq("status", status)
                    }
                }.decodeList<ClubBookDto>().map { it.toDomain() }
            }.getOrDefault(emptyList())
        }
        return flow.asStateFlow()
    }

    suspend fun deleteClubBook(clubId: String, bookId: String) {
        runCatching {
            supabase.from("club_books").delete {
                filter {
                    eq("club_id", clubId)
                    eq("book_id", bookId)
                }
            }
            dao.deleteClubBook(clubId, bookId)
            notifyLocalMutation("club_books")
        }
    }

    // ============================================================
    // CHAPTERS
    // ============================================================

    suspend fun insertChapters(chapters: List<Chapter>) {
        if (chapters.isEmpty()) return
        runCatching {
            supabase.from("chapters").upsert(chapters.map { it.toDto() })
            dao.upsertChapters(chapters)
        }
    }

    fun getChaptersForBookFlow(bookId: String): Flow<List<Chapter>> {
        scope.launch {
            runCatching {
                val list = supabase.from("chapters").select {
                    filter { eq("book_id", bookId) }
                    order("numero", Order.ASCENDING)
                }.decodeList<ChapterDto>().map { it.toDomain() }
                dao.upsertChapters(list)
            }
        }
        return dao.chaptersForBookFlow(bookId)
    }

    suspend fun deleteChaptersForBook(bookId: String) {
        runCatching {
            supabase.from("chapters").delete {
                filter { eq("book_id", bookId) }
            }
            dao.deleteChaptersForBook(bookId)
        }
    }

    // ============================================================
    // USER PROGRESS
    // ============================================================

    suspend fun insertUserProgress(progress: UserProgress) {
        runCatching {
            supabase.from("user_progress").upsert(progress.toDto())
            dao.upsertProgress(progress)
            notifyLocalMutation("user_progress")
        }
    }

    fun getUserProgressFlow(userId: String, clubId: String, bookId: String): Flow<UserProgress?> {
        val reload: suspend () -> Unit = {
            val p = runCatching {
                supabase.from("user_progress").select {
                    filter {
                        eq("user_id", userId)
                        eq("club_id", clubId)
                        eq("book_id", bookId)
                    }
                    limit(1)
                }.decodeSingleOrNull<UserProgressDto>()?.toDomain()
            }.getOrNull()
            if (p != null) dao.upsertProgress(p)
        }
        scope.launch { runCatching { reload() } }
        ensureRealtime("user_progress", filterColumn = "user_id", filterValue = userId, reload = reload)
        return dao.progressFlow(userId, clubId, bookId)
    }

    suspend fun getUserProgress(userId: String, clubId: String, bookId: String): UserProgress? {
        dao.progress(userId, clubId, bookId)?.let { return it }
        return runCatching {
            supabase.from("user_progress").select {
                filter {
                    eq("user_id", userId)
                    eq("club_id", clubId)
                    eq("book_id", bookId)
                }
                limit(1)
            }.decodeSingleOrNull<UserProgressDto>()?.toDomain()
                ?.also { dao.upsertProgress(it) }
        }.getOrNull()
    }

    fun getAllProgressForClubFlow(clubId: String): Flow<List<UserProgress>> {
        val reload: suspend () -> Unit = {
            runCatching {
                val list = supabase.from("user_progress").select {
                    filter { eq("club_id", clubId) }
                }.decodeList<UserProgressDto>().map { it.toDomain() }
                dao.upsertProgresses(list)
            }
        }
        scope.launch { runCatching { reload() } }
        ensureRealtime("user_progress", reload = reload)
        return dao.allProgressForClubFlow(clubId)
    }

    // ============================================================
    // COMMENTS / REACTIONS
    // ============================================================

    suspend fun insertComment(comment: Comment) {
        // 1. Optimistic local PRIMEIRO — UI ja mostra
        dao.upsertComment(comment)
        notifyLocalMutation("comments")
        // 2. Tenta HTTP — se falhar, queue
        val payload = """{"id":"${comment.id}","chapterId":"${comment.chapterId}","clubId":"${comment.clubId}","userId":"${comment.userId}","texto":"${comment.texto.escapeJson()}"}"""
        tryRemoteOrEnqueue("insert_comment", payload) {
            supabase.from("comments").upsert(
                CommentInsertDto(
                    id = comment.id,
                    chapterId = comment.chapterId,
                    clubId = comment.clubId,
                    userId = comment.userId,
                    texto = comment.texto,
                )
            )
        }
    }

    /** Escapa caracteres especiais pra string JSON. */
    private fun String.escapeJson(): String = this
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

    fun getCommentsForChapterFlow(chapterId: String, clubId: String): Flow<List<Comment>> {
        val key = "comments:ch:$chapterId"
        val reload: suspend () -> Unit = { syncCommentsForChapter(chapterId, clubId); markSynced(key) }
        scope.launch { syncOnce(key, Ttl.FAST) { syncCommentsForChapter(chapterId, clubId) } }
        ensureRealtime("comments", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.commentsForChapterFlow(chapterId, clubId)
    }

    private suspend fun syncCommentsForChapter(chapterId: String, clubId: String) {
        runCatching {
            val list = supabase.from("comments").select {
                filter {
                    eq("chapter_id", chapterId)
                    eq("club_id", clubId)
                }
                order("created_at", Order.ASCENDING)
            }.decodeList<CommentDto>().map { it.toDomain() }
            dao.replaceCommentsInChapter(chapterId, clubId, list)
        }
    }

    fun getCommentsForBookFlow(bookId: String, clubId: String): Flow<List<Comment>> {
        scope.launch {
            runCatching {
                // Join via embed pra trazer chapter.numero pra ordenacao
                val list = supabase.from("comments").select(Columns.raw("*, chapters!inner(numero,book_id)")) {
                    filter {
                        eq("club_id", clubId)
                        eq("chapters.book_id", bookId)
                    }
                }.decodeList<CommentDto>().map { it.toDomain() }
                dao.upsertComments(list)
            }
        }
        // Reactive flow do Room ja faz JOIN com chapters via DAO query
        return dao.commentsForBookFlow(bookId, clubId)
    }

    suspend fun softRemoveComment(commentId: String, removidoPor: String, motivo: String) {
        runCatching {
            supabase.from("comments").update({
                set("removido", true)
                set("removido_por", removidoPor)
                set("motivo_remocao", motivo)
            }) { filter { eq("id", commentId) } }
            notifyLocalMutation("comments")
        }
    }

    suspend fun restoreComment(commentId: String) {
        runCatching {
            supabase.from("comments").update({
                set("removido", false)
                set("removido_por", JsonNull)
                set("motivo_remocao", JsonNull)
            }) { filter { eq("id", commentId) } }
            notifyLocalMutation("comments")
        }
    }

    fun getRemovedCommentsForClubFlow(clubId: String): Flow<List<Comment>> {
        scope.launch {
            runCatching {
                val list = supabase.from("comments").select {
                    filter {
                        eq("club_id", clubId)
                        eq("removido", true)
                    }
                    order("created_at", Order.DESCENDING)
                }.decodeList<CommentDto>().map { it.toDomain() }
                dao.upsertComments(list)
            }
        }
        return dao.removedCommentsForClubFlow(clubId)
    }

    // ---- reactions ----

    suspend fun insertReaction(reaction: Reaction) {
        dao.upsertReaction(reaction)
        notifyLocalMutation("reactions")
        val payload = """{"commentId":"${reaction.commentId}","userId":"${reaction.userId}","emoji":"${reaction.emoji}"}"""
        tryRemoteOrEnqueue("insert_reaction", payload) {
            supabase.from("reactions").upsert(reaction.toDto())
        }
    }

    suspend fun deleteReaction(reaction: Reaction) {
        dao.deleteReaction(reaction.commentId, reaction.userId, reaction.emoji)
        notifyLocalMutation("reactions")
        val payload = """{"commentId":"${reaction.commentId}","userId":"${reaction.userId}","emoji":"${reaction.emoji}"}"""
        tryRemoteOrEnqueue("delete_reaction", payload) {
            supabase.from("reactions").delete {
                filter {
                    eq("comment_id", reaction.commentId)
                    eq("user_id", reaction.userId)
                    eq("emoji", reaction.emoji)
                }
            }
        }
    }

    fun getReactionsForCommentFlow(commentId: String): Flow<List<Reaction>> {
        val reload: suspend () -> Unit = {
            runCatching {
                val list = supabase.from("reactions").select {
                    filter { eq("comment_id", commentId) }
                }.decodeList<ReactionDto>().map { it.toDomain() }
                dao.replaceReactionsForComment(commentId, list)
            }
        }
        scope.launch { runCatching { reload() } }
        ensureRealtime("reactions", filterColumn = "comment_id", filterValue = commentId, reload = reload)
        return dao.reactionsForCommentFlow(commentId)
    }

    fun getReactionsForChapterFlow(chapterId: String): Flow<List<Reaction>> {
        val reload: suspend () -> Unit = {
            runCatching {
                val commentIds = supabase.from("comments").select(Columns.raw("id")) {
                    filter { eq("chapter_id", chapterId) }
                }.decodeList<IdOnlyDto>().map { it.id }
                if (commentIds.isNotEmpty()) {
                    val list = supabase.from("reactions").select {
                        filter { isIn("comment_id", commentIds) }
                    }.decodeList<ReactionDto>().map { it.toDomain() }
                    dao.upsertReactions(list)
                }
            }
        }
        scope.launch { runCatching { reload() } }
        ensureRealtime("reactions", reload = reload)
        return dao.reactionsForChapterFlow(chapterId)
    }

    @Serializable
    private data class IdOnlyDto(val id: String)

    // ============================================================
    // VOTES & VOTING ROUNDS
    // ============================================================

    suspend fun insertVote(vote: Vote) {
        val roundId = vote.votingRoundId ?: return
        dao.upsertVotes(listOf(vote))
        notifyLocalMutation("votes")
        val payload = """{"votingRoundId":"$roundId","userId":"${vote.userId}","bookId":"${vote.clubBookId}"}"""
        tryRemoteOrEnqueue("insert_vote", payload) {
            supabase.from("votes").upsert(
                VoteInsertDto(
                    votingRoundId = roundId,
                    userId = vote.userId,
                    bookId = vote.clubBookId,
                )
            )
        }
    }

    suspend fun clearVotesForUserInClub(userId: String, clubId: String) {
        runCatching {
            val roundIds = supabase.from("voting_rounds").select(Columns.raw("id")) {
                filter { eq("club_id", clubId) }
            }.decodeList<IdOnlyDto>().map { it.id }
            if (roundIds.isEmpty()) return@runCatching
            supabase.from("votes").delete {
                filter {
                    eq("user_id", userId)
                    isIn("voting_round_id", roundIds)
                }
            }
            notifyLocalMutation("votes")
        }
    }

    fun getVotesForClubFlow(clubId: String): Flow<List<Vote>> {
        // Sem cache local especifico — esse flow nao e critico (UI tem flow por round).
        val flow = stateOf<List<Vote>>(emptyList())
        scope.launch {
            flow.value = runCatching {
                val roundIds = supabase.from("voting_rounds").select(Columns.raw("id")) {
                    filter { eq("club_id", clubId) }
                }.decodeList<IdOnlyDto>().map { it.id }
                if (roundIds.isEmpty()) emptyList()
                else supabase.from("votes").select {
                    filter { isIn("voting_round_id", roundIds) }
                }.decodeList<VoteDto>().map { it.toDomain() }
            }.getOrDefault(emptyList())
        }
        return flow.asStateFlow()
    }

    fun getVotesForRoundFlow(roundId: String): Flow<List<Vote>> {
        val reload: suspend () -> Unit = {
            runCatching {
                val list = supabase.from("votes").select {
                    filter { eq("voting_round_id", roundId) }
                }.decodeList<VoteDto>().map { it.toDomain() }
                dao.replaceVotesInRound(roundId, list)
            }
        }
        scope.launch { runCatching { reload() } }
        ensureRealtime("votes", filterColumn = "voting_round_id", filterValue = roundId, reload = reload)
        return dao.votesForRoundFlow(roundId)
    }

    suspend fun getVotesForRound(roundId: String): List<Vote> {
        // Prefere Room; se vazio, busca remoto
        val cached = dao.votesForRound(roundId)
        if (cached.isNotEmpty()) return cached
        return runCatching {
            supabase.from("votes").select {
                filter { eq("voting_round_id", roundId) }
            }.decodeList<VoteDto>().map { it.toDomain() }
                .also { dao.upsertVotes(it) }
        }.getOrDefault(emptyList())
    }

    suspend fun removeUserVoteForBookInRound(userId: String, roundId: String, bookId: String) {
        runCatching {
            supabase.from("votes").delete {
                filter {
                    eq("user_id", userId)
                    eq("voting_round_id", roundId)
                    eq("book_id", bookId)
                }
            }
            // O sync via Realtime/notifyLocalMutation vai limpar via replace.
            notifyLocalMutation("votes")
        }
    }

    suspend fun countUserVotesInRound(userId: String, roundId: String): Int = runCatching {
        supabase.from("votes").select {
            filter {
                eq("user_id", userId)
                eq("voting_round_id", roundId)
            }
        }.decodeList<VoteDto>().size
    }.getOrDefault(0)

    // ---- voting_rounds ----

    suspend fun insertVotingRound(round: VotingRound) {
        runCatching {
            supabase.from("voting_rounds").upsert(
                VotingRoundInsertDto(
                    id = round.id,
                    clubId = round.clubId,
                    criadoPor = round.criadoPor,
                    fechaEm = round.fechaEm.toIso(),
                    nLivros = round.nLivros,
                    cadencia = round.cadencia,
                    status = round.status,
                )
            )
            dao.upsertVotingRound(round)
            notifyLocalMutation("voting_rounds")
        }
    }

    fun getActiveVotingRoundFlow(clubId: String): Flow<VotingRound?> {
        val reload: suspend () -> Unit = {
            runCatching {
                val r = getActiveVotingRound(clubId)
                if (r != null) dao.upsertVotingRound(r)
            }
        }
        scope.launch { runCatching { reload() } }
        ensureRealtime("voting_rounds", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.activeRoundForClubFlow(clubId)
    }

    suspend fun getActiveVotingRound(clubId: String): VotingRound? = runCatching {
        supabase.from("voting_rounds").select {
            filter {
                eq("club_id", clubId)
                eq("status", "aberta")
            }
            order("aberta_em", Order.DESCENDING)
            limit(1)
        }.decodeSingleOrNull<VotingRoundDto>()?.toDomain()
    }.getOrNull()

    suspend fun closeVotingRound(id: String, vencedoresJson: String) {
        // Preferir RPC close_voting_round (que faz tudo: marca finished, promove vencedor, notifica).
        // Mas o MainViewModel hoje ja faz parte desse trabalho manualmente, entao usamos
        // a RPC + ignoramos o vencedoresJson legado.
        closeVotingRoundViaRpc(id)
        notifyLocalMutation("voting_rounds")
        notifyLocalMutation("club_books") // RPC promoveu vencedor (next/current)
        notifyLocalMutation("notifications") // RPC criou notifs pros membros
    }

    // ============================================================
    // BOOK SUGGESTIONS
    // ============================================================

    suspend fun insertBookSuggestion(suggestion: BookSuggestion) {
        // Optimistic local-first (mesmo motivo de insertBook/insertClubBook).
        dao.upsertBookSuggestions(listOf(suggestion))
        notifyLocalMutation("book_suggestions")
        runCatching {
            supabase.from("book_suggestions").upsert(
                BookSuggestionInsertDto(
                    id = suggestion.id,
                    clubId = suggestion.clubId,
                    bookId = suggestion.bookId,
                    sugeridoPor = suggestion.suggestedByUserId,
                    justificativa = suggestion.justificativa.ifBlank { null },
                )
            )
        }.onFailure { android.util.Log.w("Rodape/Repo", "insertBookSuggestion remote falhou: ${it.message}") }
    }

    fun getBookSuggestionFlow(bookId: String, clubId: String): Flow<BookSuggestion?> {
        scope.launch {
            runCatching {
                val s = supabase.from("book_suggestions").select {
                    filter {
                        eq("book_id", bookId)
                        eq("club_id", clubId)
                    }
                    limit(1)
                }.decodeSingleOrNull<BookSuggestionDto>()?.toDomain()
                if (s != null) dao.upsertBookSuggestions(listOf(s))
            }
        }
        return dao.bookSuggestionFlow(clubId, bookId)
    }

    fun getBookSuggestionsForClubFlow(clubId: String): Flow<List<BookSuggestion>> {
        val reload: suspend () -> Unit = {
            runCatching {
                val list = supabase.from("book_suggestions").select {
                    filter { eq("club_id", clubId) }
                }.decodeList<BookSuggestionDto>().map { it.toDomain() }
                dao.replaceBookSuggestionsInClub(clubId, list)
            }
        }
        scope.launch { runCatching { reload() } }
        ensureRealtime("book_suggestions", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.bookSuggestionsForClubFlow(clubId)
    }

    suspend fun deleteBookSuggestion(bookId: String, clubId: String) {
        runCatching {
            supabase.from("book_suggestions").delete {
                filter {
                    eq("book_id", bookId)
                    eq("club_id", clubId)
                }
            }
            dao.deleteBookSuggestion(clubId, bookId)
            notifyLocalMutation("book_suggestions")
        }
    }

    suspend fun deleteVotesForBook(bookId: String) {
        runCatching {
            supabase.from("votes").delete { filter { eq("book_id", bookId) } }
            notifyLocalMutation("votes")
        }
    }

    // ============================================================
    // MEETINGS
    // ============================================================

    suspend fun insertMeeting(meeting: Meeting) {
        val dataIso = parseMeetingDateTime(meeting.data, meeting.hora) ?: System.currentTimeMillis().toIso()
        runCatching {
            supabase.from("meetings").upsert(
                MeetingInsertDto(
                    id = meeting.id,
                    clubId = meeting.clubId,
                    data = dataIso,
                    local = meeting.local.ifBlank { null },
                    agenda = meeting.agenda.ifBlank { null },
                    bookId = meeting.bookId,
                    chapterStart = meeting.chapterStart,
                    chapterEnd = meeting.chapterEnd,
                    status = meeting.status,
                )
            )
            dao.upsertMeetings(listOf(meeting))
            notifyLocalMutation("meetings")
        }
    }

    private fun parseMeetingDateTime(data: String, hora: String): String? {
        // Tenta formatos comuns: "DD/MM/YYYY", "DD/MM", livre.
        // Se falhar, retorna null e o caller usa now().
        return runCatching {
            val partes = data.split("/")
            if (partes.size != 3) return@runCatching null
            val dia = partes[0].toInt()
            val mes = partes[1].toInt()
            val ano = partes[2].toInt()
            val (h, m) = hora.split(":").let {
                if (it.size >= 2) it[0].toInt() to it[1].toInt() else 19 to 0
            }
            java.time.OffsetDateTime.of(ano, mes, dia, h, m, 0, 0, java.time.ZoneOffset.UTC).toString()
        }.getOrNull()
    }

    private suspend fun syncMeetingsForClub(clubId: String) {
        runCatching {
            val list = supabase.from("meetings").select {
                filter { eq("club_id", clubId) }
                order("data", Order.DESCENDING)
            }.decodeList<MeetingDto>().map { it.toDomain() }
            dao.replaceMeetingsInClub(clubId, list)
        }
    }

    fun getLatestMeetingFlow(clubId: String): Flow<Meeting?> {
        val reload: suspend () -> Unit = { syncMeetingsForClub(clubId) }
        scope.launch { runCatching { reload() } }
        ensureRealtime("meetings", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.latestMeetingForClubFlow(clubId)
    }

    fun getAllMeetingsFlow(clubId: String): Flow<List<Meeting>> {
        val reload: suspend () -> Unit = { syncMeetingsForClub(clubId) }
        scope.launch { runCatching { reload() } }
        ensureRealtime("meetings", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.meetingsForClubFlow(clubId)
    }

    fun getScheduledMeetingsForClubFlow(clubId: String): Flow<List<Meeting>> {
        val reload: suspend () -> Unit = { syncMeetingsForClub(clubId) }
        scope.launch { runCatching { reload() } }
        ensureRealtime("meetings", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.scheduledMeetingsForClubFlow(clubId)
    }

    suspend fun getMeetingById(meetingId: String): Meeting? {
        dao.meetingById(meetingId)?.let { return it }
        return runCatching {
            supabase.from("meetings").select {
                filter { eq("id", meetingId) }
                limit(1)
            }.decodeSingleOrNull<MeetingDto>()?.toDomain()
                ?.also { dao.upsertMeetings(listOf(it)) }
        }.getOrNull()
    }

    fun getMeetingByIdFlow(meetingId: String): Flow<Meeting?> {
        scope.launch { runCatching { getMeetingById(meetingId) } }
        return dao.meetingByIdFlow(meetingId)
    }

    fun getMeetingsForBookFlow(clubId: String, bookId: String): Flow<List<Meeting>> {
        scope.launch { runCatching { syncMeetingsForClub(clubId) } }
        return dao.meetingsForBookFlow(clubId, bookId)
    }

    suspend fun getMeetingsForBookList(clubId: String, bookId: String): List<Meeting> = runCatching {
        supabase.from("meetings").select {
            filter {
                eq("club_id", clubId)
                eq("book_id", bookId)
            }
            order("data", Order.ASCENDING)
        }.decodeList<MeetingDto>().map { it.toDomain() }
            .also { dao.upsertMeetings(it) }
    }.getOrDefault(emptyList())

    suspend fun updateMeetingStatus(meetingId: String, status: String) {
        runCatching {
            supabase.from("meetings").update({ set("status", status) }) {
                filter { eq("id", meetingId) }
            }
            notifyLocalMutation("meetings")
        }
    }

    suspend fun deleteMeeting(meetingId: String) {
        runCatching {
            supabase.from("meetings").delete { filter { eq("id", meetingId) } }
            dao.deleteMeeting(meetingId)
            notifyLocalMutation("meetings")
        }
    }

    suspend fun deleteRsvpsForMeeting(meetingId: String) {
        runCatching {
            supabase.from("meeting_rsvps").delete { filter { eq("meeting_id", meetingId) } }
            dao.deleteAllRsvpsForMeeting(meetingId)
            notifyLocalMutation("meeting_rsvps")
        }
    }

    // ---- meeting_rsvps ----

    suspend fun insertMeetingRsvp(rsvp: MeetingRsvp) {
        dao.upsertMeetingRsvp(rsvp)
        notifyLocalMutation("meeting_rsvps")
        val statusEnum = rsvpToEnum(rsvp.status)
        val payload = """{"meetingId":"${rsvp.meetingId}","userId":"${rsvp.userId}","status":"$statusEnum"}"""
        tryRemoteOrEnqueue("insert_meeting_rsvp", payload) {
            supabase.from("meeting_rsvps").upsert(
                MeetingRsvpInsertDto(
                    meetingId = rsvp.meetingId,
                    userId = rsvp.userId,
                    status = statusEnum,
                )
            )
        }
    }

    fun getRsvpsForMeetingFlow(meetingId: String): Flow<List<MeetingRsvp>> {
        val reload: suspend () -> Unit = {
            runCatching {
                val list = supabase.from("meeting_rsvps").select {
                    filter { eq("meeting_id", meetingId) }
                }.decodeList<MeetingRsvpDto>().map { it.toDomain() }
                dao.replaceRsvpsForMeeting(meetingId, list)
            }
        }
        scope.launch { runCatching { reload() } }
        ensureRealtime("meeting_rsvps", filterColumn = "meeting_id", filterValue = meetingId, reload = reload)
        return dao.rsvpsForMeetingFlow(meetingId)
    }

    fun getRsvpForMeetingOfUserFlow(meetingId: String, userId: String): Flow<MeetingRsvp?> {
        scope.launch {
            runCatching {
                val r = supabase.from("meeting_rsvps").select {
                    filter {
                        eq("meeting_id", meetingId)
                        eq("user_id", userId)
                    }
                    limit(1)
                }.decodeSingleOrNull<MeetingRsvpDto>()?.toDomain()
                if (r != null) dao.upsertMeetingRsvp(r)
            }
        }
        return dao.rsvpOfUserForMeetingFlow(meetingId, userId)
    }

    // ---- meeting_patterns ----

    suspend fun insertMeetingPattern(pattern: MeetingPattern) {
        runCatching {
            supabase.from("meeting_patterns").upsert(
                MeetingPatternInsertDto(
                    id = pattern.id,
                    clubId = pattern.clubId,
                    diaSemana = pattern.diaSemana,
                    hora = pattern.hora,
                    local = pattern.local.ifBlank { null },
                    agendaTemplate = pattern.agendaTemplate.ifBlank { null },
                    ativo = pattern.ativo,
                    tipoRecorrencia = pattern.tipoRecorrencia,
                    valorRecorrencia = pattern.valorRecorrencia,
                )
            )
            dao.upsertMeetingPattern(pattern)
            notifyLocalMutation("meeting_patterns")
        }
    }

    fun getActiveMeetingPatternFlow(clubId: String): Flow<MeetingPattern?> {
        scope.launch {
            runCatching {
                val p = getActiveMeetingPattern(clubId)
                if (p != null) dao.upsertMeetingPattern(p)
            }
        }
        return dao.activeMeetingPatternForClubFlow(clubId)
    }

    suspend fun getActiveMeetingPattern(clubId: String): MeetingPattern? = runCatching {
        supabase.from("meeting_patterns").select {
            filter {
                eq("club_id", clubId)
                eq("ativo", true)
            }
            limit(1)
        }.decodeSingleOrNull<MeetingPatternDto>()?.toDomain()
    }.getOrNull()

    suspend fun deactivateMeetingPatterns(clubId: String) {
        runCatching {
            supabase.from("meeting_patterns").update({ set("ativo", false) }) {
                filter { eq("club_id", clubId) }
            }
            notifyLocalMutation("meeting_patterns")
        }
    }

    // ---- meeting_minutes / meeting_notes ----

    suspend fun insertMeetingMinutes(minutes: MeetingMinutes) {
        runCatching {
            supabase.from("meeting_minutes").upsert(
                MeetingMinutesInsertDto(
                    meetingId = minutes.meetingId,
                    texto = minutes.texto,
                    lastEditorId = minutes.lastEditorId.ifBlank { null },
                )
            )
            notifyLocalMutation("meeting_minutes")
        }
    }

    fun getMeetingMinutesFlow(meetingId: String): Flow<MeetingMinutes?> {
        val reload: suspend () -> Unit = {
            runCatching {
                val m = supabase.from("meeting_minutes").select {
                    filter { eq("meeting_id", meetingId) }
                    limit(1)
                }.decodeSingleOrNull<MeetingMinutesDto>()?.toDomain()
                if (m != null) dao.upsertMeetingMinutes(m)
            }
        }
        scope.launch { runCatching { reload() } }
        ensureRealtime("meeting_minutes", filterColumn = "meeting_id", filterValue = meetingId, reload = reload)
        return dao.meetingMinutesFlow(meetingId)
    }

    suspend fun insertMeetingNote(note: MeetingNote) {
        runCatching {
            supabase.from("meeting_notes").upsert(
                MeetingNoteInsertDto(
                    meetingId = note.meetingId,
                    userId = note.userId,
                    texto = note.texto,
                )
            )
            dao.upsertMeetingNote(note)
            notifyLocalMutation("meeting_notes")
        }
    }

    fun getMeetingNoteFlow(meetingId: String, userId: String): Flow<MeetingNote?> {
        scope.launch {
            runCatching {
                val n = supabase.from("meeting_notes").select {
                    filter {
                        eq("meeting_id", meetingId)
                        eq("user_id", userId)
                    }
                    limit(1)
                }.decodeSingleOrNull<MeetingNoteDto>()?.toDomain()
                if (n != null) dao.upsertMeetingNote(n)
            }
        }
        return dao.meetingNoteOfUserFlow(meetingId, userId)
    }

    // ============================================================
    // NOTIFICATIONS
    // ============================================================

    suspend fun insertNotification(notification: DbNotification) {
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

    suspend fun markAllNotificationsAsRead(userId: String) {
        runCatching {
            supabase.from("notifications").update({ set("lida", true) }) {
                filter { eq("user_id", userId) }
            }
            notifyLocalMutation("notifications")
        }
    }

    suspend fun markNotificationAsRead(id: String) {
        runCatching {
            supabase.from("notifications").update({ set("lida", true) }) {
                filter { eq("id", id) }
            }
            notifyLocalMutation("notifications")
        }
    }

    fun getNotificationsFlow(userId: String): Flow<List<DbNotification>> {
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

    // ============================================================
    // SAVED QUOTES
    // ============================================================

    suspend fun insertSavedQuote(quote: SavedQuote) {
        dao.upsertSavedQuotes(listOf(quote))
        notifyLocalMutation("saved_quotes")
        val cap = quote.capituloRef.escapeJson()
        val payload = """{"id":"${quote.id}","userId":"${quote.userId}","clubId":"${quote.clubId}","bookId":"${quote.bookId}","texto":"${quote.texto.escapeJson()}","capituloRef":"$cap"}"""
        tryRemoteOrEnqueue("insert_saved_quote", payload) {
            supabase.from("saved_quotes").upsert(
                SavedQuoteInsertDto(
                    id = quote.id,
                    userId = quote.userId,
                    clubId = quote.clubId,
                    bookId = quote.bookId,
                    texto = quote.texto,
                    capituloRef = quote.capituloRef.ifBlank { null },
                )
            )
        }
    }

    suspend fun deleteSavedQuote(quote: SavedQuote) {
        runCatching {
            supabase.from("saved_quotes").delete { filter { eq("id", quote.id) } }
            dao.deleteSavedQuote(quote.id)
            notifyLocalMutation("saved_quotes")
        }
    }

    fun getSavedQuotesForUserFlow(userId: String): Flow<List<SavedQuote>> {
        val reload: suspend () -> Unit = {
            runCatching {
                val list = supabase.from("saved_quotes").select {
                    filter { eq("user_id", userId) }
                    order("created_at", Order.DESCENDING)
                }.decodeList<SavedQuoteDto>().map { it.toDomain() }
                dao.replaceSavedQuotesForUser(userId, list)
            }
        }
        scope.launch { runCatching { reload() } }
        ensureRealtime("saved_quotes", filterColumn = "user_id", filterValue = userId, reload = reload)
        return dao.savedQuotesForUserFlow(userId)
    }

    fun getSavedQuotesForBookFlow(userId: String, bookId: String): Flow<List<SavedQuote>> {
        scope.launch {
            runCatching {
                val list = supabase.from("saved_quotes").select {
                    filter {
                        eq("user_id", userId)
                        eq("book_id", bookId)
                    }
                    order("created_at", Order.DESCENDING)
                }.decodeList<SavedQuoteDto>().map { it.toDomain() }
                dao.upsertSavedQuotes(list)
            }
        }
        return dao.savedQuotesForBookFlow(userId, bookId)
    }

    // ============================================================
    // BOOK SUMMARIES / RATINGS
    // ============================================================

    suspend fun insertBookSummary(summary: BookSummary) {
        runCatching {
            supabase.from("book_summaries").upsert(
                BookSummaryInsertDto(
                    bookId = summary.bookId,
                    clubId = summary.clubId,
                    texto = summary.texto,
                    lastEditorId = summary.lastEditorId.ifBlank { null },
                )
            )
            dao.upsertBookSummary(summary)
            notifyLocalMutation("book_summaries")
        }
    }

    fun getBookSummaryFlow(bookId: String, clubId: String): Flow<BookSummary?> {
        val reload: suspend () -> Unit = {
            runCatching {
                val s = supabase.from("book_summaries").select {
                    filter {
                        eq("book_id", bookId)
                        eq("club_id", clubId)
                    }
                    limit(1)
                }.decodeSingleOrNull<BookSummaryDto>()?.toDomain()
                if (s != null) dao.upsertBookSummary(s)
            }
        }
        scope.launch { runCatching { reload() } }
        ensureRealtime("book_summaries", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.bookSummaryFlow(bookId, clubId)
    }

    suspend fun insertBookRating(rating: BookRating) {
        runCatching {
            supabase.from("book_ratings").upsert(
                BookRatingInsertDto(
                    bookId = rating.bookId,
                    clubId = rating.clubId,
                    userId = rating.userId,
                    stars = rating.stars,
                    comment = rating.comment,
                )
            )
            dao.upsertBookRatings(listOf(rating))
            notifyLocalMutation("book_ratings")
        }
    }

    fun getBookRatingsFlow(bookId: String, clubId: String): Flow<List<BookRating>> {
        val reload: suspend () -> Unit = {
            runCatching {
                val list = supabase.from("book_ratings").select {
                    filter {
                        eq("book_id", bookId)
                        eq("club_id", clubId)
                    }
                }.decodeList<BookRatingDto>().map { it.toDomain() }
                dao.upsertBookRatings(list)
                dao.pruneBookRatingsExcept(bookId, clubId, list.map { it.userId })
            }
        }
        scope.launch { runCatching { reload() } }
        ensureRealtime("book_ratings", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.bookRatingsFlow(bookId, clubId)
    }

    fun getBookRatingOfUserFlow(bookId: String, clubId: String, userId: String): Flow<BookRating?> {
        scope.launch {
            runCatching {
                val r = supabase.from("book_ratings").select {
                    filter {
                        eq("book_id", bookId)
                        eq("club_id", clubId)
                        eq("user_id", userId)
                    }
                    limit(1)
                }.decodeSingleOrNull<BookRatingDto>()?.toDomain()
                if (r != null) dao.upsertBookRatings(listOf(r))
            }
        }
        return dao.bookRatingOfUserFlow(bookId, clubId, userId)
    }

    // ============================================================
    // SEED — no-op (app nasce vazio em producao)
    // ============================================================

    suspend fun seedDatabase() {
        // 9B: app nasce vazio em producao. Nenhum seed.
    }
}
