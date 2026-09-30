package com.guddiz.app.core.calculation

import com.guddiz.app.core.money.Money

/**
 * The ONLY place purchase / PR / gift / FOC math is calculated.
 * UI must never do arithmetic — always call this.
 */
object CostCalculator {

    data class PurchaseCost(
        val productCost: Money,
        val packagingCost: Money,
        val shippingCost: Money,
        val otherCost: Money,
        val totalCost: Money
    )

    data class PackageCost(
        val productCost: Money,
        val packagingCost: Money,
        val courierCost: Money,
        val otherCost: Money,
        val totalCost: Money
    )

    /** A single editable line: (quantity, unitPrice). */
    data class Line(val quantity: Int, val unitPrice: Money) {
        val lineCost: Money get() = unitPrice * (if (quantity < 0) 0 else quantity)
    }

    fun calculatePurchaseCost(
        quantity: Int,
        unitPrice: Money,
        packagingCost: Money = Money.ZERO,
        shippingCost: Money = Money.ZERO,
        otherCost: Money = Money.ZERO
    ): PurchaseCost {
        val safeQty = if (quantity < 0) 0 else quantity
        val productCost = unitPrice * safeQty
        val total = productCost + packagingCost + shippingCost + otherCost
        return PurchaseCost(
            productCost = productCost,
            packagingCost = packagingCost,
            shippingCost = shippingCost,
            otherCost = otherCost,
            totalCost = total
        )
    }

    /**
     * PR package / Gift:
     * productCost = sum of every line
     * totalCost   = productCost + packaging + courier + other
     */
    fun calculatePackageCost(
        lines: List<Line>,
        packagingCost: Money = Money.ZERO,
        courierCost: Money = Money.ZERO,
        otherCost: Money = Money.ZERO
    ): PackageCost {
        val productCost = Money.sum(lines.map { it.lineCost })
        val total = productCost + packagingCost + courierCost + otherCost
        return PackageCost(
            productCost = productCost,
            packagingCost = packagingCost,
            courierCost = courierCost,
            otherCost = otherCost,
            totalCost = total
        )
    }

    fun unitCost(totalCost: Money, quantity: Int): Money =
        if (quantity <= 0) Money.ZERO else totalCost / quantity
}
