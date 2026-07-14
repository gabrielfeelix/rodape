package com.example.data.remote.repo

import com.example.data.model.Meeting
import com.example.data.model.MeetingMinutes
import com.example.data.model.MeetingNote
import com.example.data.model.MeetingPattern
import com.example.data.model.MeetingRsvp
import com.example.data.remote.MeetingDto
import com.example.data.remote.MeetingInsertDto
import com.example.data.remote.MeetingMinutesDto
import com.example.data.remote.MeetingMinutesInsertDto
import com.example.data.remote.MeetingNoteDto
import com.example.data.remote.MeetingNoteInsertDto
import com.example.data.remote.MeetingPatternDto
import com.example.data.remote.MeetingPatternInsertDto
import com.example.data.remote.MeetingRsvpDto
import com.example.data.remote.MeetingRsvpInsertDto
import com.example.data.remote.SyncEngine
import com.example.data.remote.rsvpToEnum
import com.example.data.remote.toInsertDto
import com.example.util.MeetingTime
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import javax.inject.Inject

// F3c: encontros — meetings + rsvps + patterns + minutes + notes. Corpos
// movidos VERBATIM do RemoteRepository — comportamento idêntico. Amarra:
// insertMeeting semeia a FK de books via dao (compartilhado pela engine).
interface MeetingRepository {
    suspend fun insertMeeting(meeting: Meeting)
    fun getLatestMeetingFlow(clubId: String): Flow<Meeting?>
    fun getAllMeetingsFlow(clubId: String): Flow<List<Meeting>>
    fun getScheduledMeetingsForClubFlow(clubId: String): Flow<List<Meeting>>
    suspend fun getMeetingById(meetingId: String): Meeting?
    fun getMeetingByIdFlow(meetingId: String): Flow<Meeting?>
    fun getMeetingsForBookFlow(clubId: String, bookId: String): Flow<List<Meeting>>
    suspend fun getMeetingsForBookList(clubId: String, bookId: String): List<Meeting>
    suspend fun updateMeetingStatus(meetingId: String, status: String)
    suspend fun deleteMeeting(meetingId: String)
    suspend fun deleteRsvpsForMeeting(meetingId: String)
    suspend fun insertMeetingRsvp(rsvp: MeetingRsvp)
    fun getRsvpsForMeetingFlow(meetingId: String): Flow<List<MeetingRsvp>>
    fun getRsvpForMeetingOfUserFlow(meetingId: String, userId: String): Flow<MeetingRsvp?>
    suspend fun insertMeetingPattern(pattern: MeetingPattern)
    suspend fun generateMeetingsFromPattern(pattern: MeetingPattern, horizon: Int = 8)
    fun getActiveMeetingPatternFlow(clubId: String): Flow<MeetingPattern?>
    suspend fun getActiveMeetingPattern(clubId: String): MeetingPattern?
    suspend fun deactivateMeetingPatterns(clubId: String)
    suspend fun insertMeetingMinutes(minutes: MeetingMinutes)
    fun getMeetingMinutesFlow(meetingId: String): Flow<MeetingMinutes?>
    suspend fun insertMeetingNote(note: MeetingNote)
    fun getMeetingNoteFlow(meetingId: String, userId: String): Flow<MeetingNote?>
}

internal class OfflineFirstMeetingRepository @Inject constructor(
    engine: SyncEngine,
) : OfflineFirstRepository(engine), MeetingRepository {

    override suspend fun insertMeeting(meeting: Meeting) {
        // Instante real: prefere dataEpoch (já calculado no fuso local pela VM);
        // fallback pra parse do rótulo. NUNCA interpreta hora local como UTC.
        val epoch = if (meeting.dataEpoch != 0L) meeting.dataEpoch
            else MeetingTime.parseLocal(meeting.data, meeting.hora) ?: System.currentTimeMillis()
        val dataIso = MeetingTime.epochToIso(epoch)
        // Garante que o Room guarde o epoch mesmo se a VM não preencheu.
        val toStore = if (meeting.dataEpoch != 0L) meeting else meeting.copy(dataEpoch = epoch)
        // Local-first: grava no Room antes do remoto pra o encontro aparecer na hora.
        // notifyLocalMutation (re-fetch + prune) só após o remoto confirmar, senão
        // o re-sync poda o encontro otimista ainda-não-sincronizado.
        dao.upsertMeetings(listOf(toStore))
        val dto = MeetingInsertDto(
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
        // Offline-first + fila (P0-2): offline não perde mais o encontro. O push do
        // livro (FK meetings_book_id_fkey) fica DENTRO do bloco pra também rodar no
        // replay da fila (o handler insert_meeting repete a mesma lógica).
        tryRemoteOrEnqueue("insert_meeting", json.encodeToString(dto), notifyTable = "meetings") {
            meeting.bookId?.let { bid ->
                dao.book(bid)?.let { b -> runCatching { supabase.from("books").upsert(b.toInsertDto()) } }
            }
            supabase.from("meetings").upsert(dto)
        }
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

    override fun getLatestMeetingFlow(clubId: String): Flow<Meeting?> {
        val reload: suspend () -> Unit = { syncMeetingsForClub(clubId) }
        scope.launch { runCatching { reload() } }
        ensureRealtime("meetings", filterColumn = "club_id", filterValue = clubId, reload = reload)
        // "Próximo encontro" = agendado futuro mais próximo (instante real).
        return dao.nextMeetingForClubFlow(clubId, System.currentTimeMillis())
    }

    override fun getAllMeetingsFlow(clubId: String): Flow<List<Meeting>> {
        val reload: suspend () -> Unit = { syncMeetingsForClub(clubId) }
        scope.launch { runCatching { reload() } }
        ensureRealtime("meetings", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.meetingsForClubFlow(clubId)
    }

    override fun getScheduledMeetingsForClubFlow(clubId: String): Flow<List<Meeting>> {
        val reload: suspend () -> Unit = { syncMeetingsForClub(clubId) }
        scope.launch { runCatching { reload() } }
        ensureRealtime("meetings", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.scheduledMeetingsForClubFlow(clubId)
    }

    override suspend fun getMeetingById(meetingId: String): Meeting? {
        dao.meetingById(meetingId)?.let { return it }
        return runCatching {
            supabase.from("meetings").select {
                filter { eq("id", meetingId) }
                limit(1)
            }.decodeSingleOrNull<MeetingDto>()?.toDomain()
                ?.also { dao.upsertMeetings(listOf(it)) }
        }.getOrNull()
    }

    override fun getMeetingByIdFlow(meetingId: String): Flow<Meeting?> {
        scope.launch { runCatching { getMeetingById(meetingId) } }
        return dao.meetingByIdFlow(meetingId)
    }

    override fun getMeetingsForBookFlow(clubId: String, bookId: String): Flow<List<Meeting>> {
        val reload: suspend () -> Unit = { syncMeetingsForClub(clubId) }
        scope.launch { runCatching { reload() } }
        // Realtime: encontros do livro atualizam ao vivo entre dispositivos (antes
        // era one-shot e ficava desatualizado até refresh manual).
        ensureRealtime("meetings", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.meetingsForBookFlow(clubId, bookId)
    }

    override suspend fun getMeetingsForBookList(clubId: String, bookId: String): List<Meeting> = runCatching {
        supabase.from("meetings").select {
            filter {
                eq("club_id", clubId)
                eq("book_id", bookId)
            }
            order("data", Order.ASCENDING)
        }.decodeList<MeetingDto>().map { it.toDomain() }
            .also { dao.upsertMeetings(it) }
    }.getOrDefault(emptyList())

    override suspend fun updateMeetingStatus(meetingId: String, status: String) {
        runCatching {
            supabase.from("meetings").update({ set("status", status) }) {
                filter { eq("id", meetingId) }
            }
            notifyLocalMutation("meetings")
        }
    }

    override suspend fun deleteMeeting(meetingId: String) {
        runCatching {
            supabase.from("meetings").delete { filter { eq("id", meetingId) } }
            dao.deleteMeeting(meetingId)
            notifyLocalMutation("meetings")
        }
    }

    override suspend fun deleteRsvpsForMeeting(meetingId: String) {
        runCatching {
            supabase.from("meeting_rsvps").delete { filter { eq("meeting_id", meetingId) } }
            dao.deleteAllRsvpsForMeeting(meetingId)
            notifyLocalMutation("meeting_rsvps")
        }
    }

    // ---- meeting_rsvps ----

    override suspend fun insertMeetingRsvp(rsvp: MeetingRsvp) {
        dao.upsertMeetingRsvp(rsvp)
        val statusEnum = rsvpToEnum(rsvp.status)
        val payload = """{"meetingId":"${rsvp.meetingId}","userId":"${rsvp.userId}","status":"$statusEnum"}"""
        tryRemoteOrEnqueue("insert_meeting_rsvp", payload, notifyTable = "meeting_rsvps") {
            supabase.from("meeting_rsvps").upsert(
                MeetingRsvpInsertDto(
                    meetingId = rsvp.meetingId,
                    userId = rsvp.userId,
                    status = statusEnum,
                )
            )
        }
    }

    override fun getRsvpsForMeetingFlow(meetingId: String): Flow<List<MeetingRsvp>> {
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

    override fun getRsvpForMeetingOfUserFlow(meetingId: String, userId: String): Flow<MeetingRsvp?> {
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

    override suspend fun insertMeetingPattern(pattern: MeetingPattern) {
        // Local-first: grava o padrão no Room ANTES do remoto (separado), pra ele
        // aparecer na hora e não sumir se a rede falhar (antes era tudo num
        // runCatching remoto-primeiro que engolia a escrita local).
        dao.upsertMeetingPattern(pattern)
        notifyLocalMutation("meeting_patterns")
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
        }.onFailure { android.util.Log.w("Rodape/Repo", "insertMeetingPattern remote falhou: ${it.message}") }
    }

    /**
     * Gera encontros concretos a partir de um padrão de recorrência — o que
     * fazia a recorrência FINALMENTE funcionar (antes o padrão era só um rótulo,
     * nenhum encontro nascia dele). Idempotente: id determinístico por data
     * (`mtg_pat_<clubId>_<yyyymmdd>`), então re-gerar não duplica. Antes de gerar,
     * remove encontros auto-gerados FUTUROS (caso o dia/hora do padrão tenha
     * mudado) — nunca toca em encontros criados à mão.
     *
     * [horizon] = quantas ocorrências à frente gerar (default 8).
     */
    override suspend fun generateMeetingsFromPattern(pattern: MeetingPattern, horizon: Int) {
        if (pattern.tipoRecorrencia == "unica" || !pattern.ativo) return
        val now = System.currentTimeMillis()
        val epochs = MeetingTime.nextOccurrenceEpochs(
            tipo = pattern.tipoRecorrencia,
            diaSemana = pattern.diaSemana,
            valor = pattern.valorRecorrencia,
            hora = pattern.hora,
            fromEpoch = now,
            count = horizon,
        )
        if (epochs.isEmpty()) return

        // Limpa auto-gerados futuros (local + remoto) antes de recriar.
        runCatching { dao.deleteFutureGeneratedMeetings(pattern.clubId, now) }
        runCatching {
            supabase.from("meetings").delete {
                filter {
                    eq("club_id", pattern.clubId)
                    like("id", "mtg_pat_%")
                    gte("data", MeetingTime.epochToIso(now))
                }
            }
        }

        val zone = java.time.ZoneId.systemDefault()
        val meetings = epochs.map { epoch ->
            val ymd = java.time.Instant.ofEpochMilli(epoch).atZone(zone).toLocalDate()
            val id = "mtg_pat_${pattern.clubId}_%04d%02d%02d".format(ymd.year, ymd.monthValue, ymd.dayOfMonth)
            val (label, horaStr) = MeetingTime.epochToLabel(epoch)
            Meeting(
                id = id, clubId = pattern.clubId,
                data = label, hora = horaStr,
                local = pattern.local, agenda = pattern.agendaTemplate,
                bookId = null, chapterStart = null, chapterEnd = null,
                status = "agendado", dataEpoch = epoch,
            )
        }
        meetings.forEach { insertMeeting(it) }
    }

    override fun getActiveMeetingPatternFlow(clubId: String): Flow<MeetingPattern?> {
        scope.launch {
            runCatching {
                val p = getActiveMeetingPattern(clubId)
                if (p != null) dao.upsertMeetingPattern(p)
            }
        }
        return dao.activeMeetingPatternForClubFlow(clubId)
    }

    override suspend fun getActiveMeetingPattern(clubId: String): MeetingPattern? = runCatching {
        supabase.from("meeting_patterns").select {
            filter {
                eq("club_id", clubId)
                eq("ativo", true)
            }
            limit(1)
        }.decodeSingleOrNull<MeetingPatternDto>()?.toDomain()
    }.getOrNull()

    override suspend fun deactivateMeetingPatterns(clubId: String) {
        runCatching {
            supabase.from("meeting_patterns").update({ set("ativo", false) }) {
                filter { eq("club_id", clubId) }
            }
            notifyLocalMutation("meeting_patterns")
        }
    }

    // ---- meeting_minutes / meeting_notes ----

    override suspend fun insertMeetingMinutes(minutes: MeetingMinutes) {
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

    override fun getMeetingMinutesFlow(meetingId: String): Flow<MeetingMinutes?> {
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

    override suspend fun insertMeetingNote(note: MeetingNote) {
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

    override fun getMeetingNoteFlow(meetingId: String, userId: String): Flow<MeetingNote?> {
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
}
