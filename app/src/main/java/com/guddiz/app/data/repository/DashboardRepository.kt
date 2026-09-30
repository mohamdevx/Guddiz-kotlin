package com.guddiz.app.data.repository

import com.guddiz.app.core.money.Money
import com.guddiz.app.core.util.DateUtils
import com.guddiz.app.data.local.dao.ExpenseDao
import com.guddiz.app.data.local.dao.FocDao
import com.guddiz.app.data.local.dao.GiftDao
import com.guddiz.app.data.local.dao.PrPackageDao
import com.guddiz.app.data.local.dao.ProductDao
import com.guddiz.app.data.local.dao.PurchaseDao
import com.guddiz.app.data.local.dao.SaleDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar

data class DashboardStats(
    val totalProducts: Int = 0,
    val totalStockQuantity: Int = 0,
    val totalPurchaseValue: Money = Money.ZERO,
    val totalSales: Money = Money.ZERO,
    val totalExpenses: Money = Money.ZERO,
    val totalPrCost: Money = Money.ZERO,
    val totalGiftCost: Money = Money.ZERO,
    val totalFocCost: Money = Money.ZERO,
    val currentMonthRevenue: Money = Money.ZERO,
    val lastMonthRevenue: Money = Money.ZERO,
    val currentMonthExpenses: Money = Money.ZERO,
    val lastMonthExpenses: Money = Money.ZERO,
    val lowStockCount: Int = 0,
    val expiringSoonCount: Int = 0
)

class DashboardRepository(
    productDao: ProductDao,
    saleDao: SaleDao,
    purchaseDao: PurchaseDao,
    expenseDao: ExpenseDao,
    prDao: PrPackageDao,
    giftDao: GiftDao,
    focDao: FocDao
) {
    private val now = System.currentTimeMillis()
    private val soonCutoff = now + 30L * 24L * 60L * 60L * 1000L

    private val thisMonthStart: Long = DateUtils.startOfCurrentMonth()
    private val lastMonthStart: Long
    private val lastMonthEnd: Long

    init {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.MONTH, -1)
        lastMonthStart = cal.timeInMillis
        lastMonthEnd = thisMonthStart - 1L
    }

    val stats: Flow<DashboardStats> = combine(
        combine(
            productDao.observeProductCount(),
            productDao.observeTotalQuantity(),
            productDao.observeTotalStockValue()
        ) { c, q, v -> Triple(c, q, v) },

        combine(
            saleDao.observeTotalRevenue(),
            saleDao.observeRevenueBetween(thisMonthStart, now),
            saleDao.observeRevenueBetween(lastMonthStart, lastMonthEnd)
        ) { total, thisMonth, lastMonth -> Triple(total, thisMonth, lastMonth) },

        combine(
            expenseDao.observeTotalExpenses(),
            expenseDao.observeExpensesBetween(thisMonthStart, now),
            expenseDao.observeExpensesBetween(lastMonthStart, lastMonthEnd)
        ) { total, thisMonth, lastMonth -> Triple(total, thisMonth, lastMonth) },

        combine(
            combine(
                prDao.observeTotalPrCost(),
                giftDao.observeTotalGiftCost(),
                focDao.observeTotalFocCost()
            ) { pr, g, f -> Triple(pr, g, f) },
            combine(
                productDao.observeLowStockCount(),
                productDao.observeExpiringSoonCount(now, soonCutoff)
            ) { low, exp -> low to exp }
        ) { (pr, g, f), (low, exp) ->
            DashboardStats(
                totalPrCost = Money(pr),
                totalGiftCost = Money(g),
                totalFocCost = Money(f),
                lowStockCount = low,
                expiringSoonCount = exp
            )
        }
    ) { (c, q, v), (totalSales, monthSales, lastMonthSales),
        (totalExp, monthExp, lastMonthExp), extra ->

        DashboardStats(
            totalProducts = c,
            totalStockQuantity = q,
            totalPurchaseValue = Money(v),
            totalSales = Money(totalSales),
            totalExpenses = Money(totalExp),
            currentMonthRevenue = Money(monthSales),
            lastMonthRevenue = Money(lastMonthSales),
            currentMonthExpenses = Money(monthExp),
            lastMonthExpenses = Money(lastMonthExp),
            totalPrCost = extra.totalPrCost,
            totalGiftCost = extra.totalGiftCost,
            totalFocCost = extra.totalFocCost,
            lowStockCount = extra.lowStockCount,
            expiringSoonCount = extra.expiringSoonCount
        )
    }

    companion object {
        /**
         * Percentage change from previous to current.
         * Returns null when there's nothing meaningful to show.
         */
        fun percentDelta(current: Money, previous: Money): Int? {
            if (previous.isZero) return null
            val diff = current.paisa - previous.paisa
            return ((diff * 100L) / previous.paisa).toInt()
        }
    }
}
