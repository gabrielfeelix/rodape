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
