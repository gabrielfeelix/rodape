package com.example.data.model


data class User(
    val id: String,
    val nome: String,
    val email: String,
    val avatarUrl: String
)

data class Club(
    val id: String,
    val nome: String,
    val descricao: String,
    val codigo: String,
    val cor: String, // swatch color index or hex
    val privacidade: String, // "convidados" | "publico"
    val criadorId: String,
    val criadoEm: Long,
    val arquivado: Boolean
)

data class ClubMember(
    val clubId: String,
    val userId: String,
    val papel: String, // "admin" | "member"
    val entrouEm: Long
)

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String,            // https://... | file:///... | "" (placeholder com iniciais)
    val openlibraryId: String,
    val isbn: String,
    val isManual: Boolean,           // true se foi cadastrado manualmente pelo usuário
    val totalPaginas: Int?,          // opcional — reservado pra calculadora literária (7c)
    val editora: String?,            // opcional cadastro manual
    val anoPublicacao: Int?,         // opcional cadastro manual
    val idioma: String?              // default "pt"
)

data class ClubBook(
    val clubId: String,
    val bookId: String,
    val status: String, // "current" | "finished" | "suggested" | "next"
    val ordem: Int,
    val dataEncontro: Long?
)

data class Chapter(
    val id: String,
    val bookId: String,
    val numero: Int,
    val titulo: String
)

data class UserProgress(
    val userId: String,
    val clubId: String,
    val bookId: String,
    val currentChapter: Int
)

data class Comment(
    val id: String,
    val chapterId: String,
    val clubId: String,
    val userId: String,
    val texto: String,
    val criadoEm: Long,
    val removido: Boolean,
    val removidoPor: String?,
    val motivoRemocao: String?
)

data class Reaction(
    val commentId: String,
    val userId: String,
    val emoji: String
)

data class Vote(
    val clubBookId: String,
    val userId: String,
    val votedAt: Long,
    val votingRoundId: String?
)

data class Meeting(
    val id: String,
    val clubId: String,
    val data: String,
    val hora: String,
    val local: String,
    val agenda: String,
    val bookId: String?,
    val chapterStart: Int?,
    val chapterEnd: Int?,
    val status: String // "agendado" | "concluido" | "cancelado"
)

data class MeetingRsvp(
    val meetingId: String,
    val userId: String,
    val status: String // "Vou" | "Talvez" | "Não vou"
)

data class DbNotification(
    val id: String,
    val userId: String,
    val clubId: String,
    val tipo: String,
    val payloadJson: String,
    val lida: Boolean,
    val criadoEm: Long
)

data class SavedQuote(
    val id: String,
    val userId: String,
    val clubId: String,
    val bookId: String,
    val texto: String,
    val capituloRef: String,
    val criadoEm: Long
)

data class BookSummary(
    val bookId: String,
    val clubId: String,
    val texto: String,
    val lastEditorId: String,
    val updatedAt: Long
)

data class BookRating(
    val bookId: String,
    val clubId: String,
    val userId: String,
    val stars: Int,
    val comment: String,
    val updatedAt: Long
)

data class BookSuggestion(
    val id: String,
    val clubId: String,
    val bookId: String,
    val suggestedByUserId: String,
    val justificativa: String,
    val criadoEm: Long
)

data class VotingRound(
    val id: String,
    val clubId: String,
    val criadoPor: String,
    val abertaEm: Long,
    val fechaEm: Long,
    val nLivros: Int,
    val cadencia: String,
    val status: String,
    val vencedoresJson: String
)

data class MeetingPattern(
    val id: String,
    val clubId: String,
    val diaSemana: Int,
    val hora: String,
    val local: String,
    val agendaTemplate: String,
    val ativo: Boolean,
    /**
     * "semanal" | "quinzenal" | "mensal_dia_semana" | "mensal_dia_mes" | "personalizado_dias"
     * Semântica de [valorRecorrencia] depende:
     *  - semanal/quinzenal: ignorado (usa diaSemana)
     *  - mensal_dia_semana: ordinal da semana (1=1ª, 2=2ª, …, 5=última) — combina com diaSemana
     *  - mensal_dia_mes: dia do mês (1–31)
     *  - personalizado_dias: número de dias entre encontros
     */
    val tipoRecorrencia: String,
    val valorRecorrencia: Int
)

data class MemberRemoval(
    val id: String,
    val clubId: String,
    val userId: String,
    val removedByUserId: String,
    val motivo: String,
    val removedAt: Long
)

data class MeetingMinutes(
    val meetingId: String,
    val texto: String,
    val lastEditorId: String,
    val updatedAt: Long
)

data class MeetingNote(
    val meetingId: String,
    val userId: String,
    val texto: String,
    val updatedAt: Long
)
