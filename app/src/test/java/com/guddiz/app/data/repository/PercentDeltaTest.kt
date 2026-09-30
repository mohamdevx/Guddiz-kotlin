package com.guddiz.app.data.repository

import com.guddiz.app.core.money.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PercentDeltaTest {

    @Test
    fun noData_isNull() {
        // Both zero → nothing meaningful to show
        assertNull(DashboardRepository.percentDelta(Money.ZERO, Money.ZERO))
    }

    @Test
    fun noPrevious_isNull() {
        // Last month had zero → avoid division by zero
        assertNull(DashboardRepository.percentDelta(Money.ofRupees(5000), Money.ZERO))
    }

    @Test
    fun sameAmount_isZero() {
        assertEquals(
            0,
            DashboardRepository.percentDelta(
                Money.ofRupees(1000),
                Money.ofRupees(1000)
            )
        )
    }

    @Test
    fun doubled_is100() {
        assertEquals(
            100,
            DashboardRepository.percentDelta(
                Money.ofRupees(2000),
                Money.ofRupees(1000)
            )
        )
    }

    @Test
    fun halved_isMinus50() {
        assertEquals(
            -50,
            DashboardRepository.percentDelta(
                Money.ofRupees(500),
                Money.ofRupees(1000)
            )
        )
    }

    @Test
    fun twelvePercentUp() {
        // 1000 -> 1120 = +12%
        assertEquals(
            12,
            DashboardRepository.percentDelta(
                Money.ofRupees(1120),
                Money.ofRupees(1000)
            )
        )
    }

    @Test
    fun downEightPercent() {
        assertEquals(
            -8,
            DashboardRepository.percentDelta(
                Money.ofRupees(920),
                Money.ofRupees(1000)
            )
        )
    }

    @Test
    fun largeNumbers_noOverflow() {
        // 1 crore -> 1.5 crore = +50%
        assertEquals(
            50,
            DashboardRepository.percentDelta(
                Money.ofRupees(15_000_000),
                Money.ofRupees(10_000_000)
            )
        )
    }

    @Test
    fun decimalPrecision_doesNotLosePaisa() {
        // 100.00 -> 112.50 = +12% (112.5/100 = 1.125 -> 12.5 -> truncated to 12)
        assertEquals(
            12,
            DashboardRepository.percentDelta(
                Money.parse("112.50")!!,
                Money.parse("100.00")!!
            )
        )
    }
}
