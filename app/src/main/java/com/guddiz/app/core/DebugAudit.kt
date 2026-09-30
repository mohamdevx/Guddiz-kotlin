package com.guddiz.app.core

import android.content.Context
import android.util.Log
import com.guddiz.app.GuddizApplication

/**
 * Debug-only. Prints every calculated total to logcat so we can verify
 * the math against known seed values.
 *
 * Filter with: adb logcat -d | grep GuddizAudit
 */
object DebugAudit {

    private const val TAG = "GuddizAudit"

    suspend fun run(context: Context) {
        val container = (context.applicationContext as GuddizApplication).container
        val summary = container.reportsRepository.loadSummary(0L, Long.MAX_VALUE)

        Log.i(TAG, "")
        Log.i(TAG, "================= AUDIT =================")
        Log.i(TAG, "-- SALES --")
        Log.i(TAG, "  salesCount          = ${summary.salesCount}")
        Log.i(TAG, "  totalRevenue        = ${summary.totalSales.format()}")
        Log.i(TAG, "  totalCostOfGoods    = ${summary.totalCostOfGoods.format()}")
        Log.i(TAG, "  totalProfit         = ${summary.totalProfit.format()}")
        Log.i(TAG, "-- PURCHASES --")
        Log.i(TAG, "  purchaseCount       = ${summary.purchaseCount}")
        Log.i(TAG, "  totalPurchases      = ${summary.totalPurchases.format()}")
        Log.i(TAG, "  totalPackaging      = ${summary.totalPackaging.format()}")
        Log.i(TAG, "  totalShipping       = ${summary.totalShipping.format()}")
        Log.i(TAG, "-- EXPENSES --")
        Log.i(TAG, "  expenseCount        = ${summary.expenseCount}")
        Log.i(TAG, "  totalExpenses       = ${summary.totalExpenses.format()}")
        summary.expensesByCategory.forEach { pair ->
            Log.i(TAG, "    ${pair.first}      = ${pair.second.format()}")
        }
        Log.i(TAG, "-- GIVEAWAYS --")
        Log.i(TAG, "  PR cost             = ${summary.totalPrCost.format()}  (${summary.prCount})")
        Log.i(TAG, "  Gift cost           = ${summary.totalGiftCost.format()}  (${summary.giftCount})")
        Log.i(TAG, "  FOC cost            = ${summary.totalFocCost.format()}  (${summary.focCount})")
        Log.i(TAG, "-- STOCK --")
        Log.i(TAG, "  stockQuantity       = ${summary.totalStockQuantity}")
        Log.i(TAG, "  stockValue          = ${summary.totalStockValue.format()}")
        Log.i(TAG, "-- TOP PRODUCTS --")
        summary.topProducts.forEach { p ->
            Log.i(TAG, "  #${p.productName}  x${p.qty}  =  ${com.guddiz.app.core.money.Money(p.revenue).format()}")
        }
        Log.i(TAG, "==========================================")
        Log.i(TAG, "")
    }
}
