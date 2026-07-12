package com.example.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale

/**
 * Fonte única de verdade pra data/hora dos encontros.
 *
 * Regra de ouro: o banco guarda 1 `timestamptz` (instante absoluto). O app
 * exibe (data, hora) SEMPRE no fuso do device. Antes o código montava o
 * instante como UTC (`ZoneOffset.UTC`), o que gravava 19:00 local como 19:00
 * UTC — o encontro caía horas erradas e um dia antes pra fusos negativos.
 *
 * Toda conversão local<->instante passa por aqui, com funções puras
 * (testáveis sem Android). A matemática de recorrência também.
 */
object MeetingTime {
    private val PT = Locale("pt", "BR")
    private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    /** Instante (epoch ms) de uma data/hora no fuso do device. */
    fun localToEpoch(
        ano: Int, mes: Int, dia: Int, hora: Int, minuto: Int,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long = LocalDateTime.of(ano, mes, dia, hora, minuto)
        .atZone(zone).toInstant().toEpochMilli()

    /** Parse "DD/MM/YYYY" + "HH:mm" (fuso local) -> epoch ms, ou null se inválido. */
    fun parseLocal(data: String, hora: String, zone: ZoneId = ZoneId.systemDefault()): Long? =
        runCatching {
            val p = data.trim().split("/")
            if (p.size != 3) return@runCatching null
            val dia = p[0].toInt(); val mes = p[1].toInt(); val ano = p[2].toInt()
            val (h, m) = parseHora(hora)
            localToEpoch(ano, mes, dia, h, m, zone)
        }.getOrNull()

    /** "HH:mm" -> (hora, minuto) coeridos a faixas válidas; default 19:00. */
    fun parseHora(hora: String): Pair<Int, Int> {
        val parts = hora.trim().split(":")
        val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 19
        val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        return h to m
    }

    /** ISO-8601 (UTC) do instante — formato aceito por `timestamptz`. */
    fun epochToIso(epoch: Long): String =
        Instant.ofEpochMilli(epoch).atOffset(ZoneOffset.UTC).format(ISO)

    /** ISO-8601 -> epoch ms (ou null). */
    fun isoToEpoch(iso: String?): Long? {
        if (iso.isNullOrBlank()) return null
        return runCatching { OffsetDateTime.parse(iso).toInstant().toEpochMilli() }.getOrNull()
    }

    /** epoch -> ("DOMINGO, 25 DE MAIO DE 2026", "19:00") no fuso local. */
    fun epochToLabel(epoch: Long, zone: ZoneId = ZoneId.systemDefault()): Pair<String, String> {
        val z = Instant.ofEpochMilli(epoch).atZone(zone)
        val dia = z.dayOfWeek.getDisplayName(TextStyle.FULL, PT).uppercase()
        val mes = z.month.getDisplayName(TextStyle.FULL, PT).uppercase()
        val data = "$dia, ${z.dayOfMonth} DE $mes DE ${z.year}"
        val hora = "%02d:%02d".format(z.hour, z.minute)
        return data to hora
    }

    /** Calendar (Dom=1..Sáb=7) -> java.time.DayOfWeek (Seg=1..Dom=7). */
    fun calendarDayToDayOfWeek(calDay: Int): DayOfWeek =
        if (calDay == Calendar.SUNDAY) DayOfWeek.SUNDAY else DayOfWeek.of(calDay - 1)

    /**
     * Próximas [count] datas de um padrão de recorrência, a partir de [from]
     * (inclusive). Função PURA — testável sem Android. Retorna vazio pra "unica"
     * (encontro único é criado à mão, não recorre).
     *
     *  - semanal: toda [diaSemana], +7d
     *  - quinzenal: toda [diaSemana], +14d
     *  - mensal_dia_semana: a Nª ([valor]) ocorrência de [diaSemana] no mês (5=última)
     *  - mensal_dia_mes: todo dia [valor] do mês (clampado ao tamanho do mês)
     *  - personalizado_dias: a cada [valor] dias a partir de [from]
     */
    fun nextOccurrenceDates(
        tipo: String,
        diaSemana: Int,
        valor: Int,
        from: LocalDate,
        count: Int,
    ): List<LocalDate> {
        if (count <= 0) return emptyList()
        val out = ArrayList<LocalDate>(count)
        when (tipo) {
            "semanal", "quinzenal" -> {
                val step = if (tipo == "quinzenal") 14L else 7L
                val target = calendarDayToDayOfWeek(diaSemana)
                var d = from
                while (d.dayOfWeek != target) d = d.plusDays(1)
                repeat(count) { out.add(d); d = d.plusDays(step) }
            }
            "mensal_dia_semana" -> {
                val target = calendarDayToDayOfWeek(diaSemana)
                var month = from.withDayOfMonth(1)
                while (out.size < count) {
                    val d = nthWeekdayOfMonth(month.year, month.monthValue, target, valor)
                    if (!d.isBefore(from)) out.add(d)
                    month = month.plusMonths(1)
                }
            }
            "mensal_dia_mes" -> {
                var month = from.withDayOfMonth(1)
                while (out.size < count) {
                    val day = valor.coerceIn(1, month.lengthOfMonth())
                    val d = month.withDayOfMonth(day)
                    if (!d.isBefore(from)) out.add(d)
                    month = month.plusMonths(1)
                }
            }
            "personalizado_dias" -> {
                val step = valor.coerceAtLeast(1).toLong()
                var d = from
                repeat(count) { out.add(d); d = d.plusDays(step) }
            }
            else -> { /* "unica" ou desconhecido: nada */ }
        }
        return out
    }

    /** A Nª ocorrência de [dow] no mês (nth=5 => a última do mês). */
    private fun nthWeekdayOfMonth(year: Int, month: Int, dow: DayOfWeek, nth: Int): LocalDate {
        val first = LocalDate.of(year, month, 1)
        var d = first
        while (d.dayOfWeek != dow) d = d.plusDays(1)
        // d = 1ª ocorrência. Avança (nth-1) semanas, sem passar do mês.
        if (nth >= 5) {
            var last = d
            while (last.plusWeeks(1).monthValue == month) last = last.plusWeeks(1)
            return last
        }
        val candidate = d.plusWeeks((nth - 1).coerceAtLeast(0).toLong())
        return if (candidate.monthValue == month) candidate else d
    }

    /**
     * Próximos [count] instantes (epoch ms) de um padrão, combinando a data
     * calculada com a [hora] no fuso local.
     */
    fun nextOccurrenceEpochs(
        tipo: String,
        diaSemana: Int,
        valor: Int,
        hora: String,
        fromEpoch: Long,
        count: Int,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<Long> {
        val fromDate = Instant.ofEpochMilli(fromEpoch).atZone(zone).toLocalDate()
        val (h, m) = parseHora(hora)
        return nextOccurrenceDates(tipo, diaSemana, valor, fromDate, count)
            .map { localToEpoch(it.year, it.monthValue, it.dayOfMonth, h, m, zone) }
            .filter { it >= fromEpoch - 60_000L } // tolera arredondamento; não gera passado
    }
}
