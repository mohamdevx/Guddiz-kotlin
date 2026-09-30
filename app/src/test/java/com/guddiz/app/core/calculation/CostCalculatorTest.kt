package com.guddiz.app.core.calculation

import com.guddiz.app.core.money.Money
import org.junit.Assert.assertEquals
import org.junit.Test

class CostCalculatorTest {

    private fun m(s: String): Money = Money.parse(s)!!

    // ---------------------------------------------------------------
    // calculatePurchaseCost
    // ---------------------------------------------------------------

    @Test
    fun purchase_simple() {
        val result = CostCalculator.calculatePurchaseCost(
            quantity = 3,
            unitPrice = m("100")
        )
        assertEquals(m("300"), result.productCost)
        assertEquals(Money.ZERO, result.packagingCost)
        assertEquals(Money.ZERO, result.shippingCost)
        assertEquals(Money.ZERO, result.otherCost)
        assertEquals(m("300"), result.totalCost)
    }

    @Test
    fun purchase_withAllCosts() {
        val result = CostCalculator.calculatePurchaseCost(
            quantity = 3,
            unitPrice = m("1250.50"),
            packagingCost = m("100"),
            shippingCost = m("250"),
            otherCost = m("50")
        )
        assertEquals(m("3751.50"), result.productCost)
        assertEquals(m("100"), result.packagingCost)
        assertEquals(m("250"), result.shippingCost)
        assertEquals(m("50"), result.otherCost)
        assertEquals(m("4151.50"), result.totalCost)
    }

    @Test
    fun purchase_quantityOne() {
        val result = CostCalculator.calculatePurchaseCost(
            quantity = 1,
            unitPrice = m("99.99")
        )
        assertEquals(m("99.99"), result.totalCost)
    }

    @Test
    fun purchase_quantityZero() {
        val result = CostCalculator.calculatePurchaseCost(
            quantity = 0,
            unitPrice = m("500"),
            packagingCost = m("25")
        )
        assertEquals(Money.ZERO, result.productCost)
        assertEquals(m("25"), result.totalCost)
    }

    @Test
    fun purchase_negativeQuantityIsClamped() {
        val result = CostCalculator.calculatePurchaseCost(
            quantity = -5,
            unitPrice = m("100")
        )
        assertEquals(Money.ZERO, result.productCost)
    }

    @Test
    fun purchase_largeQuantity() {
        val result = CostCalculator.calculatePurchaseCost(
            quantity = 10_000,
            unitPrice = m("12.50")
        )
        assertEquals(m("125000"), result.totalCost)
    }

    @Test
    fun purchase_zeroPackaging() {
        val result = CostCalculator.calculatePurchaseCost(
            quantity = 2,
            unitPrice = m("50"),
            packagingCost = Money.ZERO,
            shippingCost = Money.ZERO
        )
        assertEquals(m("100"), result.totalCost)
    }

    @Test
    fun purchase_decimalUnitPrice_noRounding() {
        val result = CostCalculator.calculatePurchaseCost(
            quantity = 3,
            unitPrice = m("33.33")
        )
        assertEquals(m("99.99"), result.totalCost)
    }

    // ---------------------------------------------------------------
    // calculatePackageCost (PR / Gift)
    // ---------------------------------------------------------------

    @Test
    fun package_singleLine() {
        val lines = listOf(
            CostCalculator.Line(quantity = 1, unitPrice = m("1500"))
        )
        val result = CostCalculator.calculatePackageCost(lines)
        assertEquals(m("1500"), result.productCost)
        assertEquals(m("1500"), result.totalCost)
    }

    @Test
    fun package_multiLineMatchesSpecExample() {
        val lines = listOf(
            CostCalculator.Line(1, m("1200")),
            CostCalculator.Line(1, m("800")),
            CostCalculator.Line(1, m("900"))
        )
        val result = CostCalculator.calculatePackageCost(
            lines = lines,
            packagingCost = m("100"),
            courierCost = m("250"),
            otherCost = m("50")
        )
        assertEquals(m("2900"), result.productCost)
        assertEquals(m("100"), result.packagingCost)
        assertEquals(m("250"), result.courierCost)
        assertEquals(m("50"), result.otherCost)
        assertEquals(m("3300"), result.totalCost)
    }

    @Test
    fun package_multiQuantityLines() {
        val lines = listOf(
            CostCalculator.Line(2, m("100")),
            CostCalculator.Line(3, m("200"))
        )
        val result = CostCalculator.calculatePackageCost(lines)
        assertEquals(m("800"), result.productCost)
        assertEquals(m("800"), result.totalCost)
    }

    @Test
    fun package_emptyLines() {
        val result = CostCalculator.calculatePackageCost(
            lines = emptyList(),
            packagingCost = m("50")
        )
        assertEquals(Money.ZERO, result.productCost)
        assertEquals(m("50"), result.totalCost)
    }

    @Test
    fun package_zeroExtraCosts() {
        val lines = listOf(
            CostCalculator.Line(1, m("999.99"))
        )
        val result = CostCalculator.calculatePackageCost(lines)
        assertEquals(m("999.99"), result.totalCost)
    }

    @Test
    fun package_decimalQuantityCosts_noLostPaisa() {
        val lines = listOf(
            CostCalculator.Line(3, m("33.33")),
            CostCalculator.Line(7, m("14.29"))
        )
        val result = CostCalculator.calculatePackageCost(lines)
        assertEquals(m("200.02"), result.productCost)
    }

    // ---------------------------------------------------------------
    // Line helper
    // ---------------------------------------------------------------

    @Test
    fun line_lineCost_isQuantityTimesPrice() {
        val line = CostCalculator.Line(4, m("250.75"))
        assertEquals(m("1003.00"), line.lineCost)
    }

    @Test
    fun line_negativeQuantityIsClamped() {
        val line = CostCalculator.Line(-2, m("100"))
        assertEquals(Money.ZERO, line.lineCost)
    }

    // ---------------------------------------------------------------
    // unitCost helper
    // ---------------------------------------------------------------

    @Test
    fun unitCost_evenDivision() {
        assertEquals(m("50"), CostCalculator.unitCost(m("150"), 3))
    }

    @Test
    fun unitCost_roundsHalfUp() {
        assertEquals(m("33.33"), CostCalculator.unitCost(m("100"), 3))
    }

    @Test
    fun unitCost_zeroQuantity_returnsZero() {
        assertEquals(Money.ZERO, CostCalculator.unitCost(m("100"), 0))
    }
}
