package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// ============================================================================
// Domain models — mesma data class serve como:
//  - Domain pra UI (Compose le essas props direto)
//  - @Entity do Room (cache local)
//  - Tipo de retorno do RemoteRepository (converte do DTO Supabase pra ca)
//
// Convencao Room: nomes das colunas batem com as props Kotlin (camelCase).
// O bridge pra snake_case do Postgres acontece no RemoteRepository via @SerialName.
// ============================================================================

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val nome: String,
    val email: String,
    val avatarUrl: String,
    // Pronome opcional (ele/ela/elu/…). null = não informado. Campo por último
    // com default pra não quebrar os construtores posicionais existentes.
    val pronome: String? = null,
)

@Entity(tableName = "clubs")
data class Club(
    @PrimaryKey val id: String,
    val nome: String,
    val descricao: String,
    val codigo: String,
    val cor: String, // swatch color index or hex
    val privacidade: String, // "convidados" | "publico"
    val criadorId: String,
    val criadoEm: Long,
    val arquivado: Boolean,
)

@Entity(tableName = "club_members", primaryKeys = ["clubId", "userId"])
data class ClubMember(
    val clubId: String,
    val userId: String,
    val papel: String, // "admin" | "member" | "super_admin"
    val entrouEm: Long,
)

@Entity(tableName = "books")
data class Book(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val coverUrl: String,            // https://... | file:///... | "" (placeholder com iniciais)
    val openlibraryId: String,
    val isbn: String,
    val isManual: Boolean,           // true se foi cadastrado manualmente pelo usuário
    val totalPaginas: Int?,          // opcional — reservado pra calculadora literária (7c)
    val editora: String?,            // opcional cadastro manual
    val anoPublicacao: Int?,         // opcional cadastro manual
    val idioma: String?,             // default "pt"
)

@Entity(tableName = "club_books", primaryKeys = ["clubId", "bookId"])
data class ClubBook(
    val clubId: String,
    val bookId: String,
    val status: String, // "current" | "finished" | "suggested" | "next"
    val ordem: Int,
    val dataEncontro: Long?,
)

@Entity(tableName = "chapters")
data class Chapter(
    @PrimaryKey val id: String,
    val bookId: String,
    val numero: Int,
    val titulo: String,
)

@Entity(tableName = "user_progress", primaryKeys = ["userId", "clubId", "bookId"])
data class UserProgress(
    val userId: String,
    val clubId: String,
    val bookId: String,
    val currentChapter: Int,
)

@Entity(tableName = "comments")
data class Comment(
    @PrimaryKey val id: String,
    val chapterId: String,
    val clubId: String,
    val userId: String,
    val texto: String,
    val criadoEm: Long,
    val removido: Boolean,
    val removidoPor: String?,
    val motivoRemocao: String?,
)

@Entity(tableName = "reactions", primaryKeys = ["commentId", "userId", "emoji"])
data class Reaction(
    val commentId: String,
    val userId: String,
    val emoji: String,
)

// PK inclui votingRoundId: o servidor mantém 1 voto por (round, user, book),
// e o mesmo livro pode ser votado em rodadas diferentes. Sem votingRoundId na PK
// (bug antigo), votos de rodadas distintas colapsavam numa linha só e sumiam.
@Entity(tableName = "votes", primaryKeys = ["votingRoundId", "clubBookId", "userId"])
data class Vote(
    val votingRoundId: String,
    val clubBookId: String,
    val userId: String,
    val votedAt: Long,
)

@Entity(tableName = "meetings")
data class Meeting(
    @PrimaryKey val id: String,
    val clubId: String,
    val data: String,   // rótulo de exibição no fuso local ("DOMINGO, 25 DE MAIO DE 2026")
    val hora: String,   // "HH:mm" no fuso local
    val local: String,
    val agenda: String,
    val bookId: String?,
    val chapterStart: Int?,
    val chapterEnd: Int?,
    val status: String, // "agendado" | "concluido" | "cancelado"
    // Instante real do encontro (epoch ms). Fonte de verdade pra ORDENAÇÃO
    // cronológica — `data`/`hora` são derivados só pra exibição. 0 = desconhecido
    // (linha antiga antes do sync; auto-corrige no próximo sync do servidor).
    @ColumnInfo(defaultValue = "0") val dataEpoch: Long = 0L,
)

@Entity(tableName = "meeting_rsvps", primaryKeys = ["meetingId", "userId"])
data class MeetingRsvp(
    val meetingId: String,
    val userId: String,
    val status: String, // "Vou" | "Talvez" | "Não vou"
)

@Entity(tableName = "notifications")
data class DbNotification(
    @PrimaryKey val id: String,
    val userId: String,
    val clubId: String,
    val tipo: String,
    val payloadJson: String,
    val lida: Boolean,
    val criadoEm: Long,
)

@Entity(tableName = "saved_quotes")
data class SavedQuote(
    @PrimaryKey val id: String,
    val userId: String,
    val clubId: String,
    val bookId: String,
    val texto: String,
    val capituloRef: String,
    val criadoEm: Long,
    // Moderação (0010): admin pode remover conteúdo abusivo. Filtrado no cliente.
    @ColumnInfo(defaultValue = "0") val removido: Boolean = false,
    val removidoPor: String? = null,
    val motivoRemocao: String? = null,
)

@Entity(tableName = "book_summaries", primaryKeys = ["bookId", "clubId"])
data class BookSummary(
    val bookId: String,
    val clubId: String,
    val texto: String,
    val lastEditorId: String,
    val updatedAt: Long,
)

@Entity(tableName = "book_ratings", primaryKeys = ["bookId", "clubId", "userId"])
data class BookRating(
    val bookId: String,
    val clubId: String,
    val userId: String,
    val stars: Int,
    val comment: String,
    val updatedAt: Long,
    // Moderação (0010): remoção só zera o texto da resenha (a nota em estrelas fica).
    @ColumnInfo(defaultValue = "0") val removido: Boolean = false,
    val removidoPor: String? = null,
    val motivoRemocao: String? = null,
)

// Favorito PESSOAL de livro. Chave (userId, bookId) — vale entre TODOS os clubes
// do usuário (um livro é global; favoritar é do usuário, não do clube). Espelha o
// padrão de book_ratings no sync. books.id é uuid, então book_id no servidor é uuid.
@Entity(tableName = "book_favorites", primaryKeys = ["userId", "bookId"])
data class BookFavorite(
    val userId: String,
    val bookId: String,
    val criadoEm: Long,
)

@Entity(tableName = "book_suggestions")
data class BookSuggestion(
    @PrimaryKey val id: String,
    val clubId: String,
    val bookId: String,
    val suggestedByUserId: String,
    val justificativa: String,
    val criadoEm: Long,
    // Moderação (0010): admin pode remover sugestão abusiva. Filtrado no cliente.
    @ColumnInfo(defaultValue = "0") val removido: Boolean = false,
    val removidoPor: String? = null,
    val motivoRemocao: String? = null,
)

@Entity(tableName = "voting_rounds")
data class VotingRound(
    @PrimaryKey val id: String,
    val clubId: String,
    val criadoPor: String,
    val abertaEm: Long,
    val fechaEm: Long,
    val nLivros: Int,
    val cadencia: String,
    val status: String,
    val vencedoresJson: String,
)

@Entity(tableName = "meeting_patterns")
data class MeetingPattern(
    @PrimaryKey val id: String,
    val clubId: String,
    val diaSemana: Int,
    val hora: String,
    val local: String,
    val agendaTemplate: String,
    val ativo: Boolean,
    /**
     * "semanal" | "quinzenal" | "mensal_dia_semana" | "mensal_dia_mes" | "personalizado_dias"
     */
    val tipoRecorrencia: String,
    val valorRecorrencia: Int,
)

@Entity(tableName = "member_removals")
data class MemberRemoval(
    @PrimaryKey val id: String,
    val clubId: String,
    val userId: String,
    val removedByUserId: String,
    val motivo: String,
    val removedAt: Long,
)

@Entity(tableName = "meeting_minutes")
data class MeetingMinutes(
    @PrimaryKey val meetingId: String,
    val texto: String,
    val lastEditorId: String,
    val updatedAt: Long,
)

@Entity(tableName = "meeting_notes", primaryKeys = ["meetingId", "userId"])
data class MeetingNote(
    val meetingId: String,
    val userId: String,
    val texto: String,
    val updatedAt: Long,
)

// ============================================================================
// Moderação (migration 0010) — denúncia + bloqueio.
// ============================================================================

// Bloqueio de usuário. Cache local (também no servidor via user_blocks) pra
// filtrar conteúdo de/para bloqueados OFFLINE nas listas. Bidirecional na leitura.
@Entity(tableName = "user_blocks", primaryKeys = ["blockerId", "blockedId"])
data class UserBlock(
    val blockerId: String,
    val blockedId: String,
    val criadoEm: Long,
)

// Tipo de alvo de denúncia — bate com o enum public.report_target_type.
enum class ReportTargetType(val wire: String) {
    COMMENT("comment"),
    SAVED_QUOTE("saved_quote"),
    BOOK_RATING("book_rating"),
    BOOK_SUGGESTION("book_suggestion"),
    PROFILE("profile"),
    REACTION("reaction"),
}

// Motivo — bate com o enum public.report_reason.
enum class ReportReason(val wire: String, val label: String) {
    SPAM("spam", "Spam ou propaganda"),
    ASSEDIO("assedio", "Assédio ou bullying"),
    ABUSO("abuso", "Abuso ou ameaça"),
    CONTEUDO_IMPROPRIO("conteudo_improprio", "Conteúdo impróprio"),
    DISCURSO_ODIO("discurso_odio", "Discurso de ódio"),
    OUTRO("outro", "Outro"),
}

// Denúncia lida do servidor pra fila de moderação do admin (não é @Entity Room —
// a fila é online). targetUserNome/preview são resolvidos por join no RemoteRepository.
data class ContentReport(
    val id: String,
    val clubId: String,
    val targetType: ReportTargetType,
    val targetId: String,
    val targetUserId: String,
    val reporterId: String,
    val motivo: ReportReason,
    val detalhe: String?,
    val status: String, // "pendente" | "resolvido" | "descartado"
    val criadoEm: Long,
    val targetUserNome: String? = null,
    val reporterNome: String? = null,
    val conteudoPreview: String? = null,
)
