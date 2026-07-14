package com.example.data.remote

import com.example.data.model.*
import com.example.util.MeetingTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

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

internal val isoFmt: java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME

internal fun Long.toIso(): String =
    java.time.Instant.ofEpochMilli(this)
        .atOffset(java.time.ZoneOffset.UTC)
        .format(isoFmt)

internal fun String?.fromIso(): Long {
    if (this.isNullOrBlank()) return 0L
    return runCatching {
        java.time.OffsetDateTime.parse(this).toInstant().toEpochMilli()
    }.getOrElse { 0L }
}

// ---- profiles ----
@Serializable
internal data class ProfileDto(
    val id: String,
    val nome: String,
    val sobrenome: String? = null,
    @SerialName("avatar_key") val avatarKey: String = "preset:leitor",
    @SerialName("font_scale") val fontScale: Double = 1.0,
    val pronome: String? = null,
) {
    fun toDomain(): User = User(
        id = id,
        nome = listOfNotNull(nome, sobrenome).joinToString(" ").trim().ifBlank { nome },
        email = "", // email mora em auth.users; UI le de supabaseEmail
        avatarUrl = avatarKey,
        pronome = pronome?.trim()?.ifBlank { null },
    )
}

// Update parcial do profile do usuário logado (insertUser + handler upsert_profile).
// Era nested privado no RemoteRepository; virou internal no F3b porque o handler
// mora na SyncEngine e o caminho online continua no repository.
@Serializable
internal data class ProfileUpdateDto(
    val id: String,
    val nome: String,
    val sobrenome: String? = null,
    @SerialName("avatar_key") val avatarKey: String,
    val pronome: String? = null,
)

// ---- clubs ----
@Serializable
internal data class ClubDto(
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
internal data class ClubMemberDto(
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
internal data class BookDto(
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

internal fun Book.toInsertDto(): BookInsertDto = BookInsertDto(
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
internal data class BookInsertDto(
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
internal data class ClubBookDto(
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

internal fun ClubBook.toDto() = ClubBookDto(
    clubId = clubId, bookId = bookId, status = status, ordem = ordem,
    dataEncontro = dataEncontro?.toIso(),
)

// ---- moderation (0010) ----
@Serializable
internal data class UserBlockInsertDto(
    @SerialName("blocker_id") val blockerId: String,
    @SerialName("blocked_id") val blockedId: String,
)

@Serializable
internal data class UserBlockDto(
    @SerialName("blocker_id") val blockerId: String,
    @SerialName("blocked_id") val blockedId: String,
    @SerialName("created_at") val createdAt: String? = null,
) {
    fun toDomain(): UserBlock = UserBlock(blockerId, blockedId, createdAt.fromIso())
}

@Serializable
internal data class ContentReportInsertDto(
    @SerialName("reporter_id") val reporterId: String,
    @SerialName("club_id") val clubId: String,
    @SerialName("target_type") val targetType: String,
    @SerialName("target_id") val targetId: String,
    @SerialName("target_user_id") val targetUserId: String,
    val motivo: String,
    val detalhe: String? = null,
)

@Serializable
internal data class ContentReportDto(
    val id: String,
    @SerialName("club_id") val clubId: String,
    @SerialName("target_type") val targetType: String,
    @SerialName("target_id") val targetId: String,
    @SerialName("target_user_id") val targetUserId: String,
    @SerialName("reporter_id") val reporterId: String,
    val motivo: String,
    val detalhe: String? = null,
    val status: String = "pendente",
    @SerialName("created_at") val createdAt: String? = null,
) {
    fun toDomain(): ContentReport = ContentReport(
        id = id,
        clubId = clubId,
        targetType = ReportTargetType.entries.firstOrNull { it.wire == targetType } ?: ReportTargetType.COMMENT,
        targetId = targetId,
        targetUserId = targetUserId,
        reporterId = reporterId,
        motivo = ReportReason.entries.firstOrNull { it.wire == motivo } ?: ReportReason.OUTRO,
        detalhe = detalhe,
        status = status,
        criadoEm = createdAt.fromIso(),
    )
}

// ---- chapters ----
@Serializable
internal data class ChapterDto(
    val id: String,
    @SerialName("book_id") val bookId: String,
    val numero: Int,
    val titulo: String,
) {
    fun toDomain(): Chapter = Chapter(id, bookId, numero, titulo)
}

internal fun Chapter.toDto() = ChapterDto(id, bookId, numero, titulo)

// ---- chapter_templates (índice compartilhado por ISBN — crowdsourcing) ----
@Serializable
internal data class ChapterTemplateEntryDto(val numero: Int, val titulo: String = "")

@Serializable
internal data class ChapterTemplateDto(
    val isbn: String,
    @SerialName("titulo_livro") val tituloLivro: String? = null,
    val chapters: List<ChapterTemplateEntryDto> = emptyList(),
    @SerialName("contributed_by") val contributedBy: String? = null,
)

// ---- user_progress ----
@Serializable
internal data class UserProgressDto(
    @SerialName("user_id") val userId: String,
    @SerialName("club_id") val clubId: String,
    @SerialName("book_id") val bookId: String,
    @SerialName("current_chapter") val currentChapter: Int = 0,
) {
    fun toDomain(): UserProgress = UserProgress(userId, clubId, bookId, currentChapter)
}

internal fun UserProgress.toDto() = UserProgressDto(userId, clubId, bookId, currentChapter)

// ---- comments ----
@Serializable
internal data class CommentDto(
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
internal data class CommentInsertDto(
    val id: String,
    @SerialName("chapter_id") val chapterId: String,
    @SerialName("club_id") val clubId: String,
    @SerialName("user_id") val userId: String,
    val texto: String,
)

// ---- reactions ----
@Serializable
internal data class ReactionDto(
    @SerialName("comment_id") val commentId: String,
    @SerialName("user_id") val userId: String,
    val emoji: String,
) {
    fun toDomain(): Reaction = Reaction(commentId, userId, emoji)
}

internal fun Reaction.toDto() = ReactionDto(commentId, userId, emoji)

// ---- votes ----
@Serializable
internal data class VoteDto(
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
internal data class VoteInsertDto(
    @SerialName("voting_round_id") val votingRoundId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("book_id") val bookId: String,
)

// ---- voting_rounds ----
@Serializable
internal data class VotingRoundDto(
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
internal data class VotingRoundInsertDto(
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
internal data class MeetingDto(
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
        // Banco guarda 1 timestamptz (instante). Deriva (data, hora) NO FUSO LOCAL
        // e guarda o epoch pra ordenação cronológica. Antes formatava o offset UTC
        // cru sem converter — o encontro aparecia na hora errada.
        val epoch = MeetingTime.isoToEpoch(data) ?: 0L
        val (dataStr, horaStr) = if (epoch != 0L) MeetingTime.epochToLabel(epoch) else (data to "")
        return Meeting(
            id = id, clubId = clubId,
            data = dataStr, hora = horaStr, local = local ?: "",
            agenda = agenda ?: "", bookId = bookId,
            chapterStart = chapterStart, chapterEnd = chapterEnd, status = status,
            dataEpoch = epoch,
        )
    }
}

@Serializable
internal data class MeetingInsertDto(
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

// ---- meeting_rsvps ----
@Serializable
internal data class MeetingRsvpDto(
    @SerialName("meeting_id") val meetingId: String,
    @SerialName("user_id") val userId: String,
    val status: String,
) {
    fun toDomain(): MeetingRsvp = MeetingRsvp(meetingId, userId, normalizeRsvp(status))
}

/** Banco usa enum `rsvp_status` ('vou'|'talvez'|'nao_vou'); UI usa "Vou"|"Talvez"|"Não vou". */
internal fun normalizeRsvp(s: String): String = when (s.lowercase()) {
    "vou" -> "Vou"
    "talvez" -> "Talvez"
    "nao_vou", "não vou" -> "Não vou"
    else -> s
}

internal fun rsvpToEnum(s: String): String = when (s.lowercase().replace("ã", "a")) {
    "vou" -> "vou"
    "talvez" -> "talvez"
    "nao vou", "naovou", "nao_vou" -> "nao_vou"
    else -> "talvez"
}

@Serializable
internal data class MeetingRsvpInsertDto(
    @SerialName("meeting_id") val meetingId: String,
    @SerialName("user_id") val userId: String,
    val status: String,
)

// ---- notifications ----
@Serializable
internal data class NotificationDto(
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
internal data class NotificationInsertDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("club_id") val clubId: String? = null,
    val tipo: String,
    val payload: JsonElement,
    val lida: Boolean = false,
)

// ---- saved_quotes ----
@Serializable
internal data class SavedQuoteDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("club_id") val clubId: String,
    @SerialName("book_id") val bookId: String,
    val texto: String,
    @SerialName("capitulo_ref") val capituloRef: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val removido: Boolean = false,
    @SerialName("removido_por") val removidoPor: String? = null,
    @SerialName("motivo_remocao") val motivoRemocao: String? = null,
) {
    fun toDomain(): SavedQuote = SavedQuote(
        id = id, userId = userId, clubId = clubId, bookId = bookId,
        texto = texto, capituloRef = capituloRef ?: "",
        criadoEm = createdAt.fromIso(),
        removido = removido, removidoPor = removidoPor, motivoRemocao = motivoRemocao,
    )
}

@Serializable
internal data class SavedQuoteInsertDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("club_id") val clubId: String,
    @SerialName("book_id") val bookId: String,
    val texto: String,
    @SerialName("capitulo_ref") val capituloRef: String? = null,
)

// ---- book_summaries ----
@Serializable
internal data class BookSummaryDto(
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
internal data class BookSummaryInsertDto(
    @SerialName("book_id") val bookId: String,
    @SerialName("club_id") val clubId: String,
    val texto: String,
    @SerialName("last_editor_id") val lastEditorId: String? = null,
)

// ---- book_ratings ----
@Serializable
internal data class BookRatingDto(
    @SerialName("book_id") val bookId: String,
    @SerialName("club_id") val clubId: String,
    @SerialName("user_id") val userId: String,
    val stars: Int,
    val comment: String = "",
    @SerialName("updated_at") val updatedAt: String? = null,
    val removido: Boolean = false,
    @SerialName("removido_por") val removidoPor: String? = null,
    @SerialName("motivo_remocao") val motivoRemocao: String? = null,
) {
    fun toDomain(): BookRating = BookRating(
        bookId = bookId, clubId = clubId, userId = userId,
        stars = stars, comment = comment, updatedAt = updatedAt.fromIso(),
        removido = removido, removidoPor = removidoPor, motivoRemocao = motivoRemocao,
    )
}

@Serializable
internal data class BookRatingInsertDto(
    @SerialName("book_id") val bookId: String,
    @SerialName("club_id") val clubId: String,
    @SerialName("user_id") val userId: String,
    val stars: Int,
    val comment: String = "",
)

// ---- book_favorites ----
@Serializable
internal data class BookFavoriteDto(
    @SerialName("user_id") val userId: String,
    @SerialName("book_id") val bookId: String,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
internal data class BookFavoriteInsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("book_id") val bookId: String,
)

// ---- book_suggestions ----
@Serializable
internal data class BookSuggestionDto(
    val id: String,
    @SerialName("club_id") val clubId: String,
    @SerialName("book_id") val bookId: String,
    @SerialName("voting_round_id") val votingRoundId: String? = null,
    @SerialName("sugerido_por") val sugeridoPor: String,
    val justificativa: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val removido: Boolean = false,
    @SerialName("removido_por") val removidoPor: String? = null,
    @SerialName("motivo_remocao") val motivoRemocao: String? = null,
) {
    fun toDomain(): BookSuggestion = BookSuggestion(
        id = id, clubId = clubId, bookId = bookId,
        suggestedByUserId = sugeridoPor,
        justificativa = justificativa ?: "",
        criadoEm = createdAt.fromIso(),
        removido = removido, removidoPor = removidoPor, motivoRemocao = motivoRemocao,
    )
}

@Serializable
internal data class BookSuggestionInsertDto(
    val id: String,
    @SerialName("club_id") val clubId: String,
    @SerialName("book_id") val bookId: String,
    @SerialName("sugerido_por") val sugeridoPor: String,
    val justificativa: String? = null,
)

// ---- meeting_patterns ----
@Serializable
internal data class MeetingPatternDto(
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
internal data class MeetingPatternInsertDto(
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
internal data class MemberRemovalDto(
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
internal data class MeetingMinutesDto(
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
internal data class MeetingMinutesInsertDto(
    @SerialName("meeting_id") val meetingId: String,
    val texto: String,
    @SerialName("last_editor_id") val lastEditorId: String? = null,
)

// ---- meeting_notes ----
@Serializable
internal data class MeetingNoteDto(
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
internal data class MeetingNoteInsertDto(
    @SerialName("meeting_id") val meetingId: String,
    @SerialName("user_id") val userId: String,
    val texto: String,
)
