package com.guddiz.app.core.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
    private val weekdayFormat = SimpleDateFormat("EEE", Locale.US)

    fun today(): Long = startOfDay(System.currentTimeMillis())

    fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun endOfDay(millis: Long): Long = startOfDay(millis) + 86_399_999L

    fun weekdayShort(millis: Long): String = weekdayFormat.format(Date(millis))

    fun format(millis: Long?): String =
        if (millis == null || millis <= 0L) "—" else displayFormat.format(Date(millis))

    fun startOfCurrentMonth(): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun startOfCurrentYear(): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun isExpired(expiryMillis: Long?): Boolean =
        expiryMillis != null && expiryMillis in 1..System.currentTimeMillis()

    fun isExpiringSoon(expiryMillis: Long?, withinDays: Int = 30): Boolean {
        if (expiryMillis == null || expiryMillis <= 0L) return false
        val now = System.currentTimeMillis()
        if (expiryMillis < now) return false
        return expiryMillis - now <= withinDays * 24L * 60L * 60L * 1000L
    }
}
