package com.guddiz.app.data.repository

import com.guddiz.app.core.money.Money
import com.guddiz.app.data.local.dao.ExpenseDao
import com.guddiz.app.data.local.dao.FocDao
import com.guddiz.app.data.local.dao.GiftDao
import com.guddiz.app.data.local.dao.PrPackageDao
import com.guddiz.app.data.local.dao.ProductDao
import com.guddiz.app.data.local.dao.PurchaseDao
import com.guddiz.app.data.local.dao.SaleDao
import com.guddiz.app.data.local.dao.TopProduct
import com.guddiz.app.data.local.entity.ExpenseCategory

data class ReportsSummary(
    val fromDate: Long = 0L,
    val toDate: Long = 0L,
    val totalSales: Money = Money.ZERO,
    val salesCount: Int = 0,
    val totalCostOfGoods: Money = Money.ZERO,
    val totalProfit: Money = Money.ZERO,
    val totalPurchases: Money = Money.ZERO,
    val purchaseCount: Int = 0,
    val totalExpenses: Money = Money.ZERO,
    val expenseCount: Int = 0,
    val totalPrCost: Money = Money.ZERO,
    val prCount: Int = 0,
    val totalGiftCost: Money = Money.ZERO,
    val giftCount: Int = 0,
    val totalFocCost: Money = Money.ZERO,
    val focCount: Int = 0,
    val totalPackaging: Money = Money.ZERO,
    val totalShipping: Money = Money.ZERO,
    val totalStockQuantity: Int = 0,
    val totalStockValue: Money = Money.ZERO,
    val expensesByCategory: List<Pair<String, Money>> = emptyList(),
    val topProducts: List<TopProduct> = emptyList()
)

class ReportsRepository(
    private val productDao: ProductDao,
    private val saleDao: SaleDao,
    private val purchaseDao: PurchaseDao,
    private val expenseDao: ExpenseDao,
    private val prDao: PrPackageDao,
    private val giftDao: GiftDao,
    private val focDao: FocDao
) {

    suspend fun loadSummary(from: Long, to: Long): ReportsSummary {
        val revenue = saleDao.sumRevenueBetween(from, to)
        val cogs = saleDao.sumCostBetween(from, to)
        val salesCount = saleDao.countBetween(from, to)
        val topProducts = saleDao.topProductsBetween(from, to, 5)

        val purchases = purchaseDao.sumBetween(from, to)
        val purchaseCount = purchaseDao.countBetween(from, to)
        val packaging = purchaseDao.sumPackagingBetween(from, to)
        val shipping = purchaseDao.sumShippingBetween(from, to)

        val expenses = expenseDao.sumBetween(from, to)
        val expenseCount = expenseDao.countBetween(from, to)
        val byCategory = expenseDao.totalsByCategory(from, to)
            .map { it.category to Money(it.total) }

        val pr = prDao.sumBetween(from, to)
        val prCount = prDao.countBetween(from, to)

        val gift = giftDao.sumBetween(from, to)
        val giftCount = giftDao.countBetween(from, to)

        val foc = focDao.sumBetween(from, to)
        val focCount = focDao.countBetween(from, to)

        val stockQty = productDao.observeTotalQuantityOnce()
        val stockValue = productDao.observeTotalStockValueOnce()

        val revenueMoney = Money(revenue)
        val cogsMoney = Money(cogs)

        return ReportsSummary(
            fromDate = from,
            toDate = to,
            totalSales = revenueMoney,
            salesCount = salesCount,
            totalCostOfGoods = cogsMoney,
            totalProfit = revenueMoney - cogsMoney,
            totalPurchases = Money(purchases),
            purchaseCount = purchaseCount,
            totalExpenses = Money(expenses),
            expenseCount = expenseCount,
            totalPrCost = Money(pr),
            prCount = prCount,
            totalGiftCost = Money(gift),
            giftCount = giftCount,
            totalFocCost = Money(foc),
            focCount = focCount,
            totalPackaging = Money(packaging),
            totalShipping = Money(shipping),
            totalStockQuantity = stockQty,
            totalStockValue = Money(stockValue),
            expensesByCategory = byCategory,
            topProducts = topProducts
        )
    }

    fun categoryLabel(storedName: String): String =
        ExpenseCategory.fromStorage(storedName).label
}
