package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ddMMM = SimpleDateFormat("dd 'de' MMM", Locale("pt", "BR"))
private val ddMMMyyyy = SimpleDateFormat("dd 'de' MMM 'de' yyyy", Locale("pt", "BR"))
private val MMMyyyy = SimpleDateFormat("MMM/yyyy", Locale("pt", "BR"))

fun formatShortDate(timestamp: Long): String = ddMMM.format(Date(timestamp))

fun formatFullDate(timestamp: Long): String = ddMMMyyyy.format(Date(timestamp))

fun formatMonthYear(timestamp: Long): String = MMMyyyy.format(Date(timestamp))

fun timeAgo(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val diff = (now - timestamp).coerceAtLeast(0L)
    val min = diff / 60_000
    val hour = diff / 3_600_000
    val day = diff / 86_400_000
    return when {
        min < 1 -> "agora"
        min < 60 -> "há ${min} min"
        hour < 24 -> "há ${hour}h"
        day < 7 -> "há ${day}d"
        else -> formatShortDate(timestamp)
    }
}

private val ptMonths = listOf(
    "janeiro", "fevereiro", "março", "abril", "maio", "junho",
    "julho", "agosto", "setembro", "outubro", "novembro", "dezembro",
)

/**
 * Dias até o encontro a partir do label persistido ("DOMINGO, 24 DE OUTUBRO").
 * O label não carrega ano: se a data cair mais de 6 meses no passado, assume o
 * ano seguinte. Retorna null se o label não for parseável (aí a UI esconde o
 * countdown em vez de inventar um número).
 */
fun daysUntilMeetingLabel(
    label: String,
    today: java.time.LocalDate = java.time.LocalDate.now(),
): Int? {
    val rest = label.substringAfter(",", "").trim().lowercase()
    if (rest.isEmpty()) return null
    val day = rest.takeWhile { it.isDigit() }.toIntOrNull() ?: return null
    val monthIdx = ptMonths.indexOfFirst { rest.contains(it) }
    if (monthIdx < 0) return null
    var date = runCatching { java.time.LocalDate.of(today.year, monthIdx + 1, day) }
        .getOrNull() ?: return null
    if (date.isBefore(today.minusMonths(6))) date = date.plusYears(1)
    return java.time.temporal.ChronoUnit.DAYS.between(today, date).toInt()
}
