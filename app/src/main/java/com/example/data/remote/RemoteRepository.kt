package com.example.data.remote

import com.example.data.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
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

class RemoteRepository(private val supabase: SupabaseClient = Supabase.client) {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    // Scope interno do repo pra refreshes "fire-and-forget" das caches reativas.
    // SupervisorJob: falha de uma corotina nao cancela as outras.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ----------------------- Caches reativas -----------------------
    // Polling-based: cada Flow tem uma cache MutableStateFlow que e refreshada
    // sob demanda. Acoes de mutacao chamam refresh() depois pra UI atualizar.

    private fun <T> stateOf(initial: T): MutableStateFlow<T> = MutableStateFlow(initial)

    // ============================================================
    // USERS / PROFILES
    // ============================================================

    private val userCache = mutableMapOf<String, MutableStateFlow<User?>>()

    fun getUserFlow(userId: String): Flow<User?> {
        val flow = userCache.getOrPut(userId) { stateOf<User?>(null) }
        // Fire-and-forget refresh; coletor recebe quando voltar
        scope.launch {
            runCatching { flow.value = getUser(userId) }
        }
        return flow.asStateFlow()
    }

    suspend fun getUser(userId: String): User? = runCatching {
        supabase.from("profiles").select {
            filter { eq("id", userId) }
            limit(1)
        }.decodeSingleOrNull<ProfileDto>()?.toDomain()
    }.getOrNull()

    fun getAllUsersFlow(): Flow<List<User>> {
        val flow = stateOf<List<User>>(emptyList())
        scope.launch {
            flow.value = runCatching {
                supabase.from("profiles").select().decodeList<ProfileDto>().map { it.toDomain() }
            }.getOrDefault(emptyList())
        }
        return flow.asStateFlow()
    }

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

    private val clubCache = mutableMapOf<String, MutableStateFlow<Club?>>()

    fun getClubFlow(clubId: String): Flow<Club?> {
        val flow = clubCache.getOrPut(clubId) { stateOf<Club?>(null) }
        scope.launch {
            flow.value = getClub(clubId)
        }
        return flow.asStateFlow()
    }

    suspend fun getClub(clubId: String): Club? = runCatching {
        supabase.from("clubs").select {
            filter { eq("id", clubId) }
            limit(1)
        }.decodeSingleOrNull<ClubDto>()?.toDomain()
    }.getOrNull()

    suspend fun getClubByCodigo(codigo: String): Club? = runCatching {
        supabase.from("clubs").select {
            filter { eq("codigo", codigo) }
            limit(1)
        }.decodeSingleOrNull<ClubDto>()?.toDomain()
    }.getOrNull()

    private val clubsForUserCache = mutableMapOf<String, MutableStateFlow<List<Club>>>()

    fun getClubsForUser(userId: String): Flow<List<Club>> {
        val flow = clubsForUserCache.getOrPut(userId) { stateOf<List<Club>>(emptyList()) }
        scope.launch {
            flow.value = getClubsForUserList(userId)
        }
        return flow.asStateFlow()
    }

    suspend fun getClubsForUserList(userId: String): List<Club> = runCatching {
        // RLS: usuario so ve clubs em que e membro. SELECT direto retorna tudo a que ele
        // tem acesso. Filtra arquivados.
        supabase.from("clubs").select {
            filter { eq("arquivado", false) }
        }.decodeList<ClubDto>().map { it.toDomain() }
    }.getOrDefault(emptyList())

    suspend fun insertClub(club: Club) {
        // App nao deve mais inserir clube diretamente — usa RPC create_club.
        // Mantido como no-op pra preservar interface.
    }

    /** RPC: create_club. Retorna o UUID do novo clube. */
    suspend fun createClubViaRpc(
        nome: String,
        descricao: String?,
        cor: String,
        privacidade: String,
    ): String = runCatching {
        val response = supabase.postgrest.rpc(
            function = "create_club",
            parameters = buildJsonObject {
                put("p_nome", nome)
                put("p_descricao", descricao ?: "")
                put("p_cor", cor)
                put("p_privacidade", privacidade)
            },
        ).data
        // RPC retorna scalar uuid; vem como string JSON ou objeto
        response.trim('"', ' ', '\n')
    }.getOrElse { throw it }

    suspend fun joinClubWithCodeViaRpc(codigo: String): String? = runCatching {
        val resp = supabase.postgrest.rpc(
            function = "join_club_with_code",
            parameters = buildJsonObject { put("p_codigo", codigo.uppercase().trim()) }
        ).data
        resp.trim('"', ' ', '\n').takeIf { it.isNotBlank() && it != "null" }
    }.getOrNull()

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
        val flow = stateOf<List<User>>(emptyList())
        scope.launch {
            flow.value = runCatching {
                // JOIN: club_members -> profiles (postgrest embedding)
                supabase.from("club_members").select(Columns.raw("user_id, profiles!inner(*)")) {
                    filter { eq("club_id", clubId) }
                }.decodeList<JoinMemberProfile>().map { it.profile.toDomain() }
            }.getOrDefault(emptyList())
        }
        return flow.asStateFlow()
    }

    @Serializable
    private data class JoinMemberProfile(
        @SerialName("user_id") val userId: String,
        @SerialName("profiles") val profile: ProfileDto,
    )

    suspend fun getClubMember(clubId: String, userId: String): ClubMember? = runCatching {
        supabase.from("club_members").select {
            filter {
                eq("club_id", clubId)
                eq("user_id", userId)
            }
            limit(1)
        }.decodeSingleOrNull<ClubMemberDto>()?.toDomain()
    }.getOrNull()

    suspend fun getClubMembersListOrderedByJoin(clubId: String): List<ClubMember> = runCatching {
        supabase.from("club_members").select {
            filter { eq("club_id", clubId) }
            order("entrou_em", Order.ASCENDING)
        }.decodeList<ClubMemberDto>().map { it.toDomain() }
    }.getOrDefault(emptyList())

    fun getClubMembersRawFlow(clubId: String): Flow<List<ClubMember>> {
        val flow = stateOf<List<ClubMember>>(emptyList())
        scope.launch {
            flow.value = getClubMembersListOrderedByJoin(clubId)
        }
        return flow.asStateFlow()
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
        }
    }

    suspend fun insertMemberRemoval(removal: MemberRemoval) {
        // RPC remove_member ja cuida disso. Mantido no-op.
    }

    fun getMemberRemovalsForClubFlow(clubId: String): Flow<List<MemberRemoval>> {
        val flow = stateOf<List<MemberRemoval>>(emptyList())
        scope.launch {
            flow.value = runCatching {
                supabase.from("member_removals").select {
                    filter { eq("club_id", clubId) }
                    order("removed_at", Order.DESCENDING)
                }.decodeList<MemberRemovalDto>().map { it.toDomain() }
            }.getOrDefault(emptyList())
        }
        return flow.asStateFlow()
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
        val flow = stateOf<List<Club>>(emptyList())
        scope.launch {
            flow.value = runCatching {
                supabase.from("clubs").select {
                    filter { eq("arquivado", true) }
                }.decodeList<ClubDto>().map { it.toDomain() }
            }.getOrDefault(emptyList())
        }
        return flow.asStateFlow()
    }

    // ============================================================
    // BOOKS / CLUB_BOOKS
    // ============================================================

    suspend fun insertBook(book: Book) {
        runCatching {
            supabase.from("books").upsert(book.toInsertDto())
        }
    }

    suspend fun insertClubBook(clubBook: ClubBook) {
        runCatching {
            supabase.from("club_books").upsert(clubBook.toDto())
        }
    }

    fun getBookByStatusFlow(clubId: String, status: String): Flow<List<Book>> {
        val flow = stateOf<List<Book>>(emptyList())
        scope.launch {
            flow.value = runCatching {
                supabase.from("club_books").select(Columns.raw("book_id, status, books!inner(*)")) {
                    filter {
                        eq("club_id", clubId)
                        eq("status", status)
                    }
                }.decodeList<JoinClubBookBook>().map { it.book.toDomain() }
            }.getOrDefault(emptyList())
        }
        return flow.asStateFlow()
    }

    @Serializable
    private data class JoinClubBookBook(
        @SerialName("book_id") val bookId: String,
        val status: String,
        @SerialName("books") val book: BookDto,
    )

    fun getClubBooksFlow(clubId: String): Flow<List<Book>> {
        val flow = stateOf<List<Book>>(emptyList())
        scope.launch {
            flow.value = runCatching {
                supabase.from("club_books").select(Columns.raw("book_id, books!inner(*)")) {
                    filter { eq("club_id", clubId) }
                }.decodeList<JoinClubBookBookAll>().map { it.book.toDomain() }
            }.getOrDefault(emptyList())
        }
        return flow.asStateFlow()
    }

    @Serializable
    private data class JoinClubBookBookAll(
        @SerialName("book_id") val bookId: String,
        @SerialName("books") val book: BookDto,
    )

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

    suspend fun getBook(id: String): Book? = runCatching {
        supabase.from("books").select {
            filter { eq("id", id) }
            limit(1)
        }.decodeSingleOrNull<BookDto>()?.toDomain()
    }.getOrNull()

    suspend fun updateClubBookStatus(clubId: String, bookId: String, status: String) {
        runCatching {
            supabase.from("club_books").update({ set("status", status) }) {
                filter {
                    eq("club_id", clubId)
                    eq("book_id", bookId)
                }
            }
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
        }
    }

    fun getClubBooksByStatusFlow(clubId: String, status: String): Flow<List<ClubBook>> {
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
        }
    }

    // ============================================================
    // CHAPTERS
    // ============================================================

    suspend fun insertChapters(chapters: List<Chapter>) {
        if (chapters.isEmpty()) return
        runCatching {
            supabase.from("chapters").upsert(chapters.map { it.toDto() })
        }
    }

    fun getChaptersForBookFlow(bookId: String): Flow<List<Chapter>> {
        val flow = stateOf<List<Chapter>>(emptyList())
        scope.launch {
            flow.value = runCatching {
                supabase.from("chapters").select {
                    filter { eq("book_id", bookId) }
                    order("numero", Order.ASCENDING)
                }.decodeList<ChapterDto>().map { it.toDomain() }
            }.getOrDefault(emptyList())
        }
        return flow.asStateFlow()
    }

    suspend fun deleteChaptersForBook(bookId: String) {
        runCatching {
            supabase.from("chapters").delete {
                filter { eq("book_id", bookId) }
            }
        }
    }

    // ============================================================
    // USER PROGRESS
    // ============================================================

    suspend fun insertUserProgress(progress: UserProgress) {
        runCatching {
            supabase.from("user_progress").upsert(progress.toDto())
        }
    }

    fun getUserProgressFlow(userId: String, clubId: String, bookId: String): Flow<UserProgress?> {
        val flow = stateOf<UserProgress?>(null)
        scope.launch {
            flow.value = getUserProgress(userId, clubId, bookId)
        }
        return flow.asStateFlow()
    }

    suspend fun getUserProgress(userId: String, clubId: String, bookId: String): UserProgress? = runCatching {
        supabase.from("user_progress").select {
            filter {
                eq("user_id", userId)
                eq("club_id", clubId)
                eq("book_id", bookId)
            }
            limit(1)
        }.decodeSingleOrNull<UserProgressDto>()?.toDomain()
    }.getOrNull()

    fun getAllProgressForClubFlow(clubId: String): Flow<List<UserProgress>> {
        val flow = stateOf<List<UserProgress>>(emptyList())
        scope.launch {
            flow.value = runCatching {
                supabase.from("user_progress").select {
                    filter { eq("club_id", clubId) }
                }.decodeList<UserProgressDto>().map { it.toDomain() }
            }.getOrDefault(emptyList())
        }
        return flow.asStateFlow()
    }

    // ============================================================
    // COMMENTS / REACTIONS
    // ============================================================

    suspend fun insertComment(comment: Comment) {
        runCatching {
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

    fun getCommentsForChapterFlow(chapterId: String, clubId: String): Flow<List<Comment>> {
        val flow = stateOf<List<Comment>>(emptyList())
        scope.launch {
            flow.value = runCatching {
                supabase.from("comments").select {
                    filter {
                        eq("chapter_id", chapterId)
                        eq("club_id", clubId)
                    }
                    order("created_at", Order.ASCENDING)
                }.decodeList<CommentDto>().map { it.toDomain() }
            }.getOrDefault(emptyList())
        }
        return flow.asStateFlow()
    }

    fun getCommentsForBookFlow(bookId: String, clubId: String): Flow<List<Comment>> {
        val flow = stateOf<List<Comment>>(emptyList())
        scope.launch {
            flow.value = runCatching {
                // Join via embed pra trazer chapter.numero pra ordenacao
                supabase.from("comments").select(Columns.raw("*, chapters!inner(numero,book_id)")) {
                    filter {
                        eq("club_id", clubId)
                        eq("chapters.book_id", bookId)
                    }
                }.decodeList<CommentDto>().map { it.toDomain() }
            }.getOrDefault(emptyList())
        }
        return flow.asStateFlow()
    }

    suspend fun softRemoveComment(commentId: String, removidoPor: String, motivo: String) {
        runCatching {
            supabase.from("comments").update({
                set("removido", true)
                set("removido_por", removidoPor)
                set("motivo_remocao", motivo)
            }) { filter { eq("id", commentId) } }
        }
    }

    suspend fun restoreComment(commentId: String) {
        runCatching {
            supabase.from("comments").update({
                set("removido", false)
                set("removido_por", JsonNull)
                set("motivo_remocao", JsonNull)
            }) { filter { eq("id", commentId) } }
        }
    }

    fun getRemovedCommentsForClubFlow(clubId: String): Flow<List<Comment>> {
        val flow = stateOf<List<Comment>>(emptyList())
        scope.launch {
            flow.value = runCatching {
                supabase.from("comments").select {
                    filter {
                        eq("club_id", clubId)
                        eq("removido", true)
                    }
                    order("created_at", Order.DESCENDING)
                }.decodeList<CommentDto>().map { it.toDomain() }
            }.getOrDefault(emptyList())
        }
        return flow.asStateFlow()
    }

    // ---- reactions ----

    suspend fun insertReaction(reaction: Reaction) {
        runCatching {
            supabase.from("reactions").upsert(reaction.toDto())
        }
    }

    suspend fun deleteReaction(reaction: Reaction) {
        runCatching {
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
        val flow = stateOf<List<Reaction>>(emptyList())
        scope.launch {
            flow.value = runCatching {
                supabase.from("reactions").select {
                    filter { eq("comment_id", commentId) }
                }.decodeList<ReactionDto>().map { it.toDomain() }
            }.getOrDefault(emptyList())
        }
        return flow.asStateFlow()
    }

    fun getReactionsForChapterFlow(chapterId: String): Flow<List<Reaction>> {
        val flow = stateOf<List<Reaction>>(emptyList())
        scope.launch {
            flow.value = runCatching {
                // Pega comment_ids do capitulo, depois reactions desses comments.
                val commentIds = supabase.from("comments").select(Columns.raw("id")) {
                    filter { eq("chapter_id", chapterId) }
                }.decodeList<IdOnlyDto>().map { it.id }
                if (commentIds.isEmpty()) emptyList()
                else supabase.from("reactions").select {
                    filter { isIn("comment_id", commentIds) }
                }.decodeList<ReactionDto>().map { it.toDomain() }
            }.getOrDefault(emptyList())
        }
        return flow.asStateFlow()
    }

    @Serializable
    private data class IdOnlyDto(val id: String)

    // ============================================================
    // VOTES & VOTING ROUNDS
    // ============================================================

    suspend fun insertVote(vote: Vote) {
        val roundId = vote.votingRoundId ?: return
        runCatching {
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
            // Pega rounds desse clube
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
        }
    }

    fun getVotesForClubFlow(clubId: String): Flow<List<Vote>> {
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
        val flow = stateOf<List<Vote>>(emptyList())
        scope.launch {
            flow.value = getVotesForRound(roundId)
        }
        return flow.asStateFlow()
    }

    suspend fun getVotesForRound(roundId: String): List<Vote> = runCatching {
        supabase.from("votes").select {
            filter { eq("voting_round_id", roundId) }
        }.decodeList<VoteDto>().map { it.toDomain() }
    }.getOrDefault(emptyList())

    suspend fun removeUserVoteForBookInRound(userId: String, roundId: String, bookId: String) {
        runCatching {
            supabase.from("votes").delete {
                filter {
                    eq("user_id", userId)
                    eq("voting_round_id", roundId)
                    eq("book_id", bookId)
                }
            }
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
        }
    }

    fun getActiveVotingRoundFlow(clubId: String): Flow<VotingRound?> {
        val flow = stateOf<VotingRound?>(null)
        scope.launch {
            flow.value = getActiveVotingRound(clubId)
        }
        return flow.asStateFlow()
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
    }

    // ============================================================
    // BOOK SUGGESTIONS
    // ============================================================

    suspend fun insertBookSuggestion(suggestion: BookSuggestion) {
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
        }
    }

    fun getBookSuggestionFlow(bookId: String, clubId: String): Flow<BookSuggestion?> {
        val flow = stateOf<BookSuggestion?>(null)
        scope.launch {
            flow.value = runCatching {
                supabase.from("book_suggestions").select {
                    filter {
                        eq("book_id", bookId)
                        eq("club_id", clubId)
                    }
                    limit(1)
                }.decodeSingleOrNull<BookSuggestionDto>()?.toDomain()
            }.getOrNull()
        }
        return flow.asStateFlow()
    }

    fun getBookSuggestionsForClubFlow(clubId: String): Flow<List<BookSuggestion>> {
        val flow = stateOf<List<BookSuggestion>>(emptyList())
        scope.launch {
            flow.value = runCatching {
                supabase.from("book_suggestions").select {
                    filter { eq("club_id", clubId) }
                }.decodeList<BookSuggestionDto>().map { it.toDomain() }
            }.getOrDefault(emptyList())
        }
        return flow.asStateFlow()
    }

    suspend fun deleteBookSuggestion(bookId: String, clubId: String) {
        runCatching {
            supabase.from("book_suggestions").delete {
                filter {
                    eq("book_id", bookId)
                    eq("club_id", clubId)
                }
            }
        }
    }

    suspend fun deleteVotesForBook(bookId: String) {
        runCatching {
            supabase.from("votes").delete { filter { eq("book_id", bookId) } }
        }
    }

    // ============================================================
    // MEETINGS
    // ============================================================

    suspend fun insertMeeting(meeting: Meeting) {
        // Converte "data + hora" (legacy) pra timestamptz. Fallback: agora.
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

    fun getLatestMeetingFlow(clubId: String): Flow<Meeting?> {
        val flow = stateOf<Meeting?>(null)
        scope.launch {
            flow.value = runCatching {
                supabase.from("meetings").select {
                    filter { eq("club_id", clubId) }
                    order("data", Order.DESCENDING)
                    limit(1)
                }.decodeSingleOrNull<MeetingDto>()?.toDomain()
            }.getOrNull()
        }
        return flow.asStateFlow()
    }

    fun getAllMeetingsFlow(clubId: String): Flow<List<Meeting>> {
        val flow = stateOf<List<Meeting>>(emptyList())
        scope.launch {
            flow.value = runCatching {
                supabase.from("meetings").select {
                    filter { eq("club_id", clubId) }
                    order("data", Order.DESCENDING)
                }.decodeList<MeetingDto>().map { it.toDomain() }
            }.getOrDefault(emptyList())
        }
        return flow.asStateFlow()
    }

    fun getScheduledMeetingsForClubFlow(clubId: String): Flow<List<Meeting>> {
        val flow = stateOf<List<Meeting>>(emptyList())
        scope.launch {
            flow.value = runCatching {
                supabase.from("meetings").select {
                    filter {
                        eq("club_id", clubId)
                        eq("status", "agendado")
                    }
                    order("data", Order.ASCENDING)
                }.decodeList<MeetingDto>().map { it.toDomain() }
            }.getOrDefault(emptyList())
        }
        return flow.asStateFlow()
    }

    suspend fun getMeetingById(meetingId: String): Meeting? = runCatching {
        supabase.from("meetings").select {
            filter { eq("id", meetingId) }
            limit(1)
        }.decodeSingleOrNull<MeetingDto>()?.toDomain()
    }.getOrNull()

    fun getMeetingByIdFlow(meetingId: String): Flow<Meeting?> {
        val flow = stateOf<Meeting?>(null)
        scope.launch {
            flow.value = getMeetingById(meetingId)
        }
        return flow.asStateFlow()
    }

    fun getMeetingsForBookFlow(clubId: String, bookId: String): Flow<List<Meeting>> {
        val flow = stateOf<List<Meeting>>(emptyList())
        scope.launch {
            flow.value = getMeetingsForBookList(clubId, bookId)
        }
        return flow.asStateFlow()
    }

    suspend fun getMeetingsForBookList(clubId: String, bookId: String): List<Meeting> = runCatching {
        supabase.from("meetings").select {
            filter {
                eq("club_id", clubId)
                eq("book_id", bookId)
            }
            order("data", Order.ASCENDING)
        }.decodeList<MeetingDto>().map { it.toDomain() }
    }.getOrDefault(emptyList())

    suspend fun updateMeetingStatus(meetingId: String, status: String) {
        runCatching {
            supabase.from("meetings").update({ set("status", status) }) {
                filter { eq("id", meetingId) }
            }
        }
    }

    suspend fun deleteMeeting(meetingId: String) {
        runCatching {
            supabase.from("meetings").delete { filter { eq("id", meetingId) } }
        }
    }

    suspend fun deleteRsvpsForMeeting(meetingId: String) {
        runCatching {
            supabase.from("meeting_rsvps").delete { filter { eq("meeting_id", meetingId) } }
        }
    }

    // ---- meeting_rsvps ----

    suspend fun insertMeetingRsvp(rsvp: MeetingRsvp) {
        runCatching {
            supabase.from("meeting_rsvps").upsert(
                MeetingRsvpInsertDto(
                    meetingId = rsvp.meetingId,
                    userId = rsvp.userId,
                    status = rsvpToEnum(rsvp.status),
                )
            )
        }
    }

    fun getRsvpsForMeetingFlow(meetingId: String): Flow<List<MeetingRsvp>> {
        val flow = stateOf<List<MeetingRsvp>>(emptyList())
        scope.launch {
            flow.value = runCatching {
                supabase.from("meeting_rsvps").select {
                    filter { eq("meeting_id", meetingId) }
                }.decodeList<MeetingRsvpDto>().map { it.toDomain() }
            }.getOrDefault(emptyList())
        }
        return flow.asStateFlow()
    }

    fun getRsvpForMeetingOfUserFlow(meetingId: String, userId: String): Flow<MeetingRsvp?> {
        val flow = stateOf<MeetingRsvp?>(null)
        scope.launch {
            flow.value = runCatching {
                supabase.from("meeting_rsvps").select {
                    filter {
                        eq("meeting_id", meetingId)
                        eq("user_id", userId)
                    }
                    limit(1)
                }.decodeSingleOrNull<MeetingRsvpDto>()?.toDomain()
            }.getOrNull()
        }
        return flow.asStateFlow()
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
        }
    }

    fun getActiveMeetingPatternFlow(clubId: String): Flow<MeetingPattern?> {
        val flow = stateOf<MeetingPattern?>(null)
        scope.launch {
            flow.value = getActiveMeetingPattern(clubId)
        }
        return flow.asStateFlow()
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
        }
    }

    fun getMeetingMinutesFlow(meetingId: String): Flow<MeetingMinutes?> {
        val flow = stateOf<MeetingMinutes?>(null)
        scope.launch {
            flow.value = runCatching {
                supabase.from("meeting_minutes").select {
                    filter { eq("meeting_id", meetingId) }
                    limit(1)
                }.decodeSingleOrNull<MeetingMinutesDto>()?.toDomain()
            }.getOrNull()
        }
        return flow.asStateFlow()
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
        }
    }

    fun getMeetingNoteFlow(meetingId: String, userId: String): Flow<MeetingNote?> {
        val flow = stateOf<MeetingNote?>(null)
        scope.launch {
            flow.value = runCatching {
                supabase.from("meeting_notes").select {
                    filter {
                        eq("meeting_id", meetingId)
                        eq("user_id", userId)
                    }
                    limit(1)
                }.decodeSingleOrNull<MeetingNoteDto>()?.toDomain()
            }.getOrNull()
        }
        return flow.asStateFlow()
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
        }
    }

    suspend fun markAllNotificationsAsRead(userId: String) {
        runCatching {
            supabase.from("notifications").update({ set("lida", true) }) {
                filter { eq("user_id", userId) }
            }
        }
    }

    suspend fun markNotificationAsRead(id: String) {
        runCatching {
            supabase.from("notifications").update({ set("lida", true) }) {
                filter { eq("id", id) }
            }
        }
    }

    fun getNotificationsFlow(userId: String): Flow<List<DbNotification>> {
        val flow = stateOf<List<DbNotification>>(emptyList())
        scope.launch {
            flow.value = runCatching {
                supabase.from("notifications").select {
                    filter { eq("user_id", userId) }
                    order("created_at", Order.DESCENDING)
                }.decodeList<NotificationDto>().map { it.toDomain() }
            }.getOrDefault(emptyList())
        }
        return flow.asStateFlow()
    }

    // ============================================================
    // SAVED QUOTES
    // ============================================================

    suspend fun insertSavedQuote(quote: SavedQuote) {
        runCatching {
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
        }
    }

    fun getSavedQuotesForUserFlow(userId: String): Flow<List<SavedQuote>> {
        val flow = stateOf<List<SavedQuote>>(emptyList())
        scope.launch {
            flow.value = runCatching {
                supabase.from("saved_quotes").select {
                    filter { eq("user_id", userId) }
                    order("created_at", Order.DESCENDING)
                }.decodeList<SavedQuoteDto>().map { it.toDomain() }
            }.getOrDefault(emptyList())
        }
        return flow.asStateFlow()
    }

    fun getSavedQuotesForBookFlow(userId: String, bookId: String): Flow<List<SavedQuote>> {
        val flow = stateOf<List<SavedQuote>>(emptyList())
        scope.launch {
            flow.value = runCatching {
                supabase.from("saved_quotes").select {
                    filter {
                        eq("user_id", userId)
                        eq("book_id", bookId)
                    }
                    order("created_at", Order.DESCENDING)
                }.decodeList<SavedQuoteDto>().map { it.toDomain() }
            }.getOrDefault(emptyList())
        }
        return flow.asStateFlow()
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
        }
    }

    fun getBookSummaryFlow(bookId: String, clubId: String): Flow<BookSummary?> {
        val flow = stateOf<BookSummary?>(null)
        scope.launch {
            flow.value = runCatching {
                supabase.from("book_summaries").select {
                    filter {
                        eq("book_id", bookId)
                        eq("club_id", clubId)
                    }
                    limit(1)
                }.decodeSingleOrNull<BookSummaryDto>()?.toDomain()
            }.getOrNull()
        }
        return flow.asStateFlow()
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
        }
    }

    fun getBookRatingsFlow(bookId: String, clubId: String): Flow<List<BookRating>> {
        val flow = stateOf<List<BookRating>>(emptyList())
        scope.launch {
            flow.value = runCatching {
                supabase.from("book_ratings").select {
                    filter {
                        eq("book_id", bookId)
                        eq("club_id", clubId)
                    }
                }.decodeList<BookRatingDto>().map { it.toDomain() }
            }.getOrDefault(emptyList())
        }
        return flow.asStateFlow()
    }

    fun getBookRatingOfUserFlow(bookId: String, clubId: String, userId: String): Flow<BookRating?> {
        val flow = stateOf<BookRating?>(null)
        scope.launch {
            flow.value = runCatching {
                supabase.from("book_ratings").select {
                    filter {
                        eq("book_id", bookId)
                        eq("club_id", clubId)
                        eq("user_id", userId)
                    }
                    limit(1)
                }.decodeSingleOrNull<BookRatingDto>()?.toDomain()
            }.getOrNull()
        }
        return flow.asStateFlow()
    }

    // ============================================================
    // SEED — no-op (app nasce vazio em producao)
    // ============================================================

    suspend fun seedDatabase() {
        // 9B: app nasce vazio em producao. Nenhum seed.
    }
}
