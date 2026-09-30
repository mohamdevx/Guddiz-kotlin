package com.guddiz.app.core.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {

    @Test
    fun ofRupees_convertsToPaisa() {
        assertEquals(Money(125000), Money.ofRupees(1250))
    }

    @Test
    fun parse_plainInteger() {
        assertEquals(Money(125000), Money.parse("1250"))
    }

    @Test
    fun parse_withDecimals() {
        assertEquals(Money(125050), Money.parse("1250.50"))
    }

    @Test
    fun parse_withOneDecimal() {
        // "1250.5" should become 1250.50 exactly
        assertEquals(Money(125050), Money.parse("1250.5"))
    }

    @Test
    fun parse_withCommasAndRs() {
        assertEquals(Money(125050), Money.parse("Rs 1,250.50"))
    }

    @Test
    fun parse_lowercaseRs() {
        assertEquals(Money(99900), Money.parse("rs 999"))
    }

    @Test
    fun parse_roundsHalfUp() {
        // 1250.555 -> 1250.56 -> 125056 paisa
        assertEquals(Money(125056), Money.parse("1250.555"))
    }

    @Test
    fun parse_emptyReturnsNull() {
        assertNull(Money.parse(""))
    }

    @Test
    fun parse_garbageReturnsNull() {
        assertNull(Money.parse("abc"))
    }

    @Test
    fun parse_negative_isAllowed() {
        // Negative money is allowed at the type level — validation happens in ViewModels
        assertEquals(Money(-12500), Money.parse("-125"))
    }

    @Test
    fun plus_addsPaisa() {
        assertEquals(Money(375000), Money(125000) + Money(250000))
    }

    @Test
    fun minus_subtractsPaisa() {
        assertEquals(Money(75000), Money(200000) - Money(125000))
    }

    @Test
    fun minus_canGoNegative() {
        assertEquals(Money(-50000), Money(50000) - Money(100000))
    }

    @Test
    fun times_byQuantity() {
        // 1250.50 x 3 = 3751.50
        assertEquals(Money(375150), Money(125050) * 3)
    }

    @Test
    fun times_byZero_isZero() {
        assertEquals(Money.ZERO, Money(125050) * 0)
    }

    @Test
    fun div_roundsHalfUp() {
        // 1.00 split three ways -> 0.33 (33 paisa, half-up rounding)
        assertEquals(Money(33), Money(100) / 3)
    }

    @Test
    fun div_byZero_throws() {
        try {
            Money(100) / 0
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun format_basic() {
        // Locale.US thousands separators, always two decimals
        assertEquals("Rs 1,250.50", Money(125050).format())
    }

    @Test
    fun format_zero() {
        assertEquals("Rs 0.00", Money.ZERO.format())
    }

    @Test
    fun format_smallAmount() {
        assertEquals("Rs 0.05", Money(5).format())
    }

    @Test
    fun format_largeAmount() {
        assertEquals("Rs 12,500,000.00", Money(1_250_000_000L).format())
    }

    @Test
    fun toEditText_noSeparators() {
        assertEquals("1250.50", Money(125050).toEditText())
    }

    @Test
    fun toEditText_zero() {
        assertEquals("0.00", Money.ZERO.toEditText())
    }

    @Test
    fun isZero_works() {
        assertTrue(Money.ZERO.isZero)
        assertTrue(!Money(1).isZero)
    }

    @Test
    fun isNegative_works() {
        assertTrue(Money(-1).isNegative)
        assertTrue(!Money(0).isNegative)
        assertTrue(!Money(1).isNegative)
    }

    @Test
    fun isPositive_works() {
        assertTrue(Money(1).isPositive)
        assertTrue(!Money(0).isPositive)
        assertTrue(!Money(-1).isPositive)
    }

    @Test
    fun sum_addsAllValues() {
        val total = Money.sum(listOf(Money(100), Money(250), Money(1000)))
        assertEquals(Money(1350), total)
    }

    @Test
    fun sum_emptyList() {
        assertEquals(Money.ZERO, Money.sum(emptyList()))
    }

    @Test
    fun comparison_lessThan() {
        assertTrue(Money(100) < Money(200))
    }

    @Test
    fun comparison_equal() {
        assertEquals(0, Money(100).compareTo(Money(100)))
    }

    @Test
    fun parseThenFormat_roundTrips() {
        val original = "Rs 1,234.56"
        val money = Money.parse(original)!!
        assertEquals(original, money.format())
    }
}
