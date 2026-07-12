package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

/**
 * Verifica a matemática de data/timezone e recorrência dos encontros — a parte
 * que o time marcava como "só falha em runtime". Testes puros (java.time), sem
 * Android, com ZoneId explícito pra serem determinísticos em qualquer máquina.
 */
class MeetingTimeTest {

    private val SP = ZoneId.of("America/Sao_Paulo") // UTC-3

    // ---- timezone: escrever local != gravar como UTC ----

    @Test
    fun `parseLocal e epochToLabel batem no fuso local`() {
        // 25/12/2026 19:30 em SP -> rótulo bonito de volta no MESMO fuso.
        val epoch = MeetingTime.parseLocal("25/12/2026", "19:30", SP)!!
        val (data, hora) = MeetingTime.epochToLabel(epoch, SP)
        assertEquals("SEXTA-FEIRA, 25 DE DEZEMBRO DE 2026", data)
        assertEquals("19:30", hora)
    }

    @Test
    fun `epoch guardado e o instante local correto (nao UTC)`() {
        // 19:00 local em SP (UTC-3) = 22:00 UTC. O bug antigo gravava 19:00 UTC.
        val epoch = MeetingTime.parseLocal("15/06/2026", "19:00", SP)!!
        val utc = java.time.Instant.ofEpochMilli(epoch).atZone(ZoneId.of("UTC"))
        assertEquals(22, utc.hour) // 19 + 3
    }

    @Test
    fun `isoToEpoch e epochToIso round-trip`() {
        val epoch = MeetingTime.parseLocal("01/01/2027", "08:00", SP)!!
        val iso = MeetingTime.epochToIso(epoch)
        assertEquals(epoch, MeetingTime.isoToEpoch(iso))
    }

    @Test
    fun `parseLocal invalido retorna null`() {
        assertNull(MeetingTime.parseLocal("nao-e-data", "19:00", SP))
        assertNull(MeetingTime.parseLocal("30/02", "19:00", SP)) // sem ano
    }

    // ---- recorrência ----

    @Test
    fun `semanal gera o mesmo dia da semana a cada 7 dias`() {
        // Ter=Calendar.TUESDAY(3). A partir de seg 13/07/2026.
        val out = MeetingTime.nextOccurrenceDates("semanal", Calendar.TUESDAY, 0, LocalDate.of(2026, 7, 13), 3)
        assertEquals(listOf(LocalDate.of(2026,7,14), LocalDate.of(2026,7,21), LocalDate.of(2026,7,28)), out)
    }

    @Test
    fun `quinzenal a cada 14 dias`() {
        val out = MeetingTime.nextOccurrenceDates("quinzenal", Calendar.TUESDAY, 0, LocalDate.of(2026, 7, 13), 3)
        assertEquals(listOf(LocalDate.of(2026,7,14), LocalDate.of(2026,7,28), LocalDate.of(2026,8,11)), out)
    }

    @Test
    fun `mensal_dia_semana pega a 2a terca do mes`() {
        // valor=2 (2ª ocorrência). Julho/2026: 1º é qua -> 1ª ter = 07, 2ª ter = 14.
        val out = MeetingTime.nextOccurrenceDates("mensal_dia_semana", Calendar.TUESDAY, 2, LocalDate.of(2026, 7, 1), 2)
        assertEquals(listOf(LocalDate.of(2026,7,14), LocalDate.of(2026,8,11)), out)
    }

    @Test
    fun `mensal_dia_semana valor 5 pega a ultima ocorrencia do mes`() {
        // última sexta de julho/2026 = 31 (sextas: 3,10,17,24,31).
        val out = MeetingTime.nextOccurrenceDates("mensal_dia_semana", Calendar.FRIDAY, 5, LocalDate.of(2026, 7, 1), 1)
        assertEquals(listOf(LocalDate.of(2026,7,31)), out)
    }

    @Test
    fun `mensal_dia_mes pula o dia ja passado e clampa meses curtos`() {
        // dia 15, a partir de 20/07 -> ago/15, set/15.
        val out = MeetingTime.nextOccurrenceDates("mensal_dia_mes", 0, 15, LocalDate.of(2026, 7, 20), 2)
        assertEquals(listOf(LocalDate.of(2026,8,15), LocalDate.of(2026,9,15)), out)
        // dia 31 em fevereiro clampa pro último dia do mês.
        val fev = MeetingTime.nextOccurrenceDates("mensal_dia_mes", 0, 31, LocalDate.of(2027, 2, 1), 1)
        assertEquals(listOf(LocalDate.of(2027,2,28)), fev)
    }

    @Test
    fun `personalizado_dias a cada N dias a partir de hoje`() {
        val out = MeetingTime.nextOccurrenceDates("personalizado_dias", 0, 10, LocalDate.of(2026, 7, 13), 3)
        assertEquals(listOf(LocalDate.of(2026,7,13), LocalDate.of(2026,7,23), LocalDate.of(2026,8,2)), out)
    }

    @Test
    fun `unica nao gera recorrencia`() {
        assertTrue(MeetingTime.nextOccurrenceDates("unica", Calendar.TUESDAY, 0, LocalDate.of(2026, 7, 13), 5).isEmpty())
    }

    @Test
    fun `nextOccurrenceEpochs nao gera datas no passado`() {
        val now = MeetingTime.parseLocal("13/07/2026", "12:00", SP)!!
        val epochs = MeetingTime.nextOccurrenceEpochs("semanal", Calendar.MONDAY, 0, "19:00", now, 4, SP)
        assertTrue(epochs.all { it >= now - 60_000L })
        assertTrue(epochs.isNotEmpty())
    }
}
