package com.guddiz.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DateUtilsTest {

    @Test
    fun startOfDay_clearsTimeFields() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 42)
            set(Calendar.SECOND, 33)
            set(Calendar.MILLISECOND, 999)
        }
        val start = DateUtils.startOfDay(cal.timeInMillis)

        val verify = Calendar.getInstance().apply { timeInMillis = start }
        assertEquals(0, verify.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, verify.get(Calendar.MINUTE))
        assertEquals(0, verify.get(Calendar.SECOND))
        assertEquals(0, verify.get(Calendar.MILLISECOND))
    }

    @Test
    fun today_isAtStartOfDay() {
        val today = DateUtils.today()
        val cal = Calendar.getInstance().apply { timeInMillis = today }
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
    }

    @Test
    fun endOfDay_isOneMsBeforeNextMidnight() {
        val start = DateUtils.today()
        val end = DateUtils.endOfDay(start)
        assertEquals(start + 86_399_999L, end)
    }

    @Test
    fun format_nullShowsDash() {
        assertEquals("—", DateUtils.format(null))
    }

    @Test
    fun format_zeroShowsDash() {
        assertEquals("—", DateUtils.format(0L))
    }

    @Test
    fun format_realDateIsReadable() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.SEPTEMBER) // 0-indexed
            set(Calendar.DAY_OF_MONTH, 29)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals("29 Sep 2026", DateUtils.format(cal.timeInMillis))
    }

    @Test
    fun isExpired_pastDateIsTrue() {
        val yesterday = System.currentTimeMillis() - 24L * 60 * 60 * 1000
        assertTrue(DateUtils.isExpired(yesterday))
    }

    @Test
    fun isExpired_futureDateIsFalse() {
        val tomorrow = System.currentTimeMillis() + 24L * 60 * 60 * 1000
        assertFalse(DateUtils.isExpired(tomorrow))
    }

    @Test
    fun isExpired_nullIsFalse() {
        assertFalse(DateUtils.isExpired(null))
    }

    @Test
    fun isExpired_zeroIsFalse() {
        assertFalse(DateUtils.isExpired(0L))
    }

    @Test
    fun isExpiringSoon_withinThirtyDays() {
        val inTenDays = System.currentTimeMillis() + 10L * 24 * 60 * 60 * 1000
        assertTrue(DateUtils.isExpiringSoon(inTenDays))
    }

    @Test
    fun isExpiringSoon_beyondThirtyDays() {
        val inSixtyDays = System.currentTimeMillis() + 60L * 24 * 60 * 60 * 1000
        assertFalse(DateUtils.isExpiringSoon(inSixtyDays))
    }

    @Test
    fun isExpiringSoon_alreadyExpiredIsFalse() {
        val yesterday = System.currentTimeMillis() - 24L * 60 * 60 * 1000
        assertFalse(DateUtils.isExpiringSoon(yesterday))
    }

    @Test
    fun isExpiringSoon_nullIsFalse() {
        assertFalse(DateUtils.isExpiringSoon(null))
    }

    @Test
    fun startOfCurrentMonth_isDayOne() {
        val start = DateUtils.startOfCurrentMonth()
        val cal = Calendar.getInstance().apply { timeInMillis = start }
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun startOfCurrentYear_isJanuaryFirst() {
        val start = DateUtils.startOfCurrentYear()
        val cal = Calendar.getInstance().apply { timeInMillis = start }
        assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH))
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH))
    }
}
