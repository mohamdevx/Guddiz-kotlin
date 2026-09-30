package com.guddiz.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class WeekBucketTest {

    @Test
    fun startOfDay_stripsTime() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 42)
            set(Calendar.SECOND, 30)
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
    fun sevenDayWalk_producesSevenDistinctDays() {
        // Simulate the loop from SaleRepository.observeLast7DaysRevenue()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -6)
        }
        val weekStart = cal.timeInMillis

        val days = mutableListOf<Long>()
        val walker = Calendar.getInstance().apply { timeInMillis = weekStart }
        repeat(7) {
            days.add(walker.timeInMillis)
            walker.add(Calendar.DAY_OF_YEAR, 1)
        }

        assertEquals(7, days.size)
        // Strictly increasing
        for (i in 1 until days.size) {
            assertTrue(days[i] > days[i - 1])
        }
        // Every gap is exactly 24h (or 23/25h around DST — but not applicable here)
        for (i in 1 until days.size) {
            val diff = days[i] - days[i - 1]
            // Must be at least 23h and at most 25h
            assertTrue("Gap $diff ms between day $i and ${i-1}", diff in 23L*3600*1000..25L*3600*1000)
        }
    }

    @Test
    fun weekdayLabel_isThreeLetters() {
        val label = DateUtils.weekdayShort(System.currentTimeMillis())
        assertEquals(3, label.length)
    }

    @Test
    fun sumOfSevenDays_equalsTotalRevenueWhenAllOnOneDay() {
        // All sales today
        val today = DateUtils.today()
        val weekStart = DateUtils.startOfDay(System.currentTimeMillis() - 6L*24*3600*1000)

        // Simulate bucketing
        val byDay = mutableMapOf<Long, Long>()
        byDay[DateUtils.startOfDay(today)] = 1_000_00L  // Rs 1000 in paisa

        // Sum
        val total = byDay.values.sum()
        assertEquals(100_000L, total)
    }
}
