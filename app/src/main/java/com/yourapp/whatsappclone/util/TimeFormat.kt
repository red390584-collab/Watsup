package com.yourapp.whatsappclone.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * WhatsApp-style time formatting: "13:37" for today, "Yesterday" for the
 * previous day, otherwise "dd/mm/yy".
 */
object TimeFormat {

    private val clock: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val date: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yy")

    fun short(millis: Long): String {
        if (millis <= 0L) return ""
        val zone = ZoneId.systemDefault()
        val then = Instant.ofEpochMilli(millis).atZone(zone)
        val today = LocalDate.now(zone)
        return when (then.toLocalDate()) {
            today -> clock.format(then.toLocalTime())
            today.minusDays(1L) -> "Yesterday"
            else -> date.format(then)
        }
    }

    fun clockOnly(millis: Long): String {
        if (millis <= 0L) return ""
        val zone = ZoneId.systemDefault()
        return clock.format(Instant.ofEpochMilli(millis).atZone(zone).toLocalTime())
    }

    fun isToday(millis: Long): Boolean {
        if (millis <= 0L) return false
        val zone = ZoneId.systemDefault()
        return Instant.ofEpochMilli(millis).atZone(zone).toLocalDate() == LocalDate.now(zone)
    }

    fun minutesSince(millis: Long): Long =
        TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - millis)
}
