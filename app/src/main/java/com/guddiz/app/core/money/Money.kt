package com.guddiz.app.core.money

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Decimal-safe money type. Stored as whole paisa (1 rupee = 100 paisa).
 * Never do money math with Double — use this.
 */
@JvmInline
value class Money(val paisa: Long) : Comparable<Money> {

    operator fun plus(other: Money) = Money(paisa + other.paisa)
    operator fun minus(other: Money) = Money(paisa - other.paisa)
    operator fun times(quantity: Int) = Money(paisa * quantity.toLong())
    operator fun times(quantity: Long) = Money(paisa * quantity)

    operator fun div(divisor: Int): Money {
        require(divisor != 0) { "Cannot divide money by zero" }
        return Money(
            BigDecimal.valueOf(paisa)
                .divide(BigDecimal.valueOf(divisor.toLong()), 0, RoundingMode.HALF_UP)
                .longValueExact()
        )
    }

    override fun compareTo(other: Money) = paisa.compareTo(other.paisa)

    val isZero: Boolean get() = paisa == 0L
    val isNegative: Boolean get() = paisa < 0L
    val isPositive: Boolean get() = paisa > 0L

    fun format(): String {
        val f = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "Rs " + f.format(BigDecimal.valueOf(paisa, 2))
    }

    fun toEditText(): String = BigDecimal.valueOf(paisa, 2).toPlainString()

    /** Short form for chart labels: 12k, 1.2M, etc. */
    fun formatCompact(): String {
        val rupees = paisa / 100L
        return when {
            rupees >= 10_000_000L -> (rupees / 10_000_000L).toString() + "Cr"
            rupees >= 100_000L -> {
                val l = rupees / 100_000L
                val r = (rupees % 100_000L) / 10_000L
                if (r == 0L) "${l}L" else "$l.${r}L"
            }
            rupees >= 1_000L -> {
                val k = rupees / 1_000L
                val r = (rupees % 1_000L) / 100L
                if (r == 0L) "${k}k" else "$k.${r}k"
            }
            else -> rupees.toString()
        }
    }

    companion object {
        val ZERO = Money(0)
        fun ofRupees(rupees: Long): Money = Money(rupees * 100)

        fun parse(input: String): Money? {
            val cleaned = input.trim()
                .replace(",", "").replace(" ", "")
                .removePrefix("Rs").removePrefix("rs").removePrefix("RS").trim()
            if (cleaned.isEmpty()) return null
            val decimal = cleaned.toBigDecimalOrNull() ?: return null
            return try {
                Money(decimal.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact())
            } catch (e: ArithmeticException) { null }
        }

        fun sum(values: Iterable<Money>): Money =
            Money(values.fold(0L) { acc, m -> acc + m.paisa })
    }
}
