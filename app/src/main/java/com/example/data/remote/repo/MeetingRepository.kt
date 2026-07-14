package com.example.data.remote.repo

import com.example.data.model.Meeting
import com.example.data.model.MeetingMinutes
import com.example.data.model.MeetingNote
import com.example.data.model.MeetingPattern
import com.example.data.model.MeetingRsvp
import kotlinx.coroutines.flow.Flow

/**
 * Encontros: reuniões, RSVPs, padrões de recorrência, atas e notas (tabelas
 * `meetings`, `meeting_rsvps`, `meeting_patterns`, `meeting_minutes`,
 * `meeting_notes`). Fatiado da god-repo (F3c).
 */
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
