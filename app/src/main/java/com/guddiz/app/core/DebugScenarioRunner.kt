package com.guddiz.app.core

import android.content.Context
import android.util.Log
import com.guddiz.app.GuddizApplication
import com.guddiz.app.core.money.Money
import com.guddiz.app.core.util.DateUtils
import com.guddiz.app.data.local.entity.ExpenseCategory
import com.guddiz.app.data.local.entity.ExpenseEntity
import com.guddiz.app.data.local.entity.FocEntity
import com.guddiz.app.data.local.entity.GiftEntity
import com.guddiz.app.data.local.entity.GiftItemEntity
import com.guddiz.app.data.local.entity.PrPackageEntity
import com.guddiz.app.data.local.entity.PrPackageItemEntity
import com.guddiz.app.data.local.entity.ProductEntity
import com.guddiz.app.data.local.entity.PurchaseEntity
import com.guddiz.app.data.local.entity.SaleChannel
import com.guddiz.app.data.local.entity.SaleEntity
import com.guddiz.app.data.local.entity.SaleItemEntity
import com.guddiz.app.data.local.entity.StockMovementType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Runs every record type in sequence, checks stock + money at each step.
 * Debug-only. Logs to GuddizScenario.
 *   adb logcat -d | grep GuddizScenario
 */
object DebugScenarioRunner {

    private const val TAG = "GuddizScenario"

    suspend fun runAll(context: Context): Boolean {
        val container = (context.applicationContext as GuddizApplication).container

        Log.i(TAG, "")
        Log.i(TAG, "############ SCENARIO TEST ############")

        val db = com.guddiz.app.data.local.GuddizDatabase.get(context)
        db.saleDao().deleteAllSaleItems()
        db.saleDao().deleteAllSales()
        db.prPackageDao().deleteAllPrItems()
        db.prPackageDao().deleteAllPrPackages()
        db.giftDao().deleteAllGiftItems()
        db.giftDao().deleteAllGifts()
        db.focDao().deleteAllFoc()
        db.expenseDao().deleteAllExpenses()
        db.purchaseDao().deleteAllPurchases()
        db.stockMovementDao().deleteAllMovements()
        db.productDao().deleteAllProducts()

        var passed = 0
        var failed = 0

        fun check(label: String, expected: String, actual: String) {
            val ok = expected == actual
            if (ok) passed++ else failed++
            val mark = if (ok) "PASS" else "FAIL"
            Log.i(TAG, "  [$mark] $label  expected=$expected actual=$actual")
        }

        suspend fun getStock(productId: Long): Int {
            return container.productRepository.getProduct(productId)?.currentQuantity ?: -1
        }

        suspend fun expenseCount(): Int {
            return container.expenseRepository.observeExpenses().first().size
        }

        suspend fun expenses(): List<ExpenseEntity> {
            return container.expenseRepository.observeExpenses().first()
        }

        // ================= SCENARIO =================

        // 1. Product
        Log.i(TAG, "-- STEP 0: create product --")
        val productId = container.productRepository.save(
            ProductEntity(
                name = "ScenarioTest",
                sku = "SCN-1",
                category = "SKINCARE",
                purchasePrice = 10_000L,
                sellingPrice = 15_000L,
                currentQuantity = 10,
                minStockLevel = 1
            )
        )
        check("initial stock", "10", getStock(productId).toString())

        // 2. Purchase qty 5 @ 100
        Log.i(TAG, "-- STEP 1: purchase qty 5 @ 100 --")
        container.purchaseRepository.save(
            PurchaseEntity(
                productId = productId,
                productName = "ScenarioTest",
                quantity = 5,
                unitPrice = 10_000L,
                supplier = "TestSupplier",
                purchaseDate = DateUtils.today(),
                packagingCost = 0L,
                shippingCost = 0L,
                otherCost = 0L,
                totalCost = 0L
            ),
            previousQuantity = 0
        )
        check("after purchase stock", "15", getStock(productId).toString())

        // 3. Sale qty 2 @ 150
        Log.i(TAG, "-- STEP 2: sale qty 2 @ 150 --")
        container.saleRepository.save(
            SaleEntity(
                saleDate = DateUtils.today(),
                channel = SaleChannel.WALK_IN.name,
                customer = null,
                totalRevenue = 0L,
                totalCost = 0L,
                notes = null
            ),
            listOf(
                SaleItemEntity(
                    saleId = 0L,
                    productId = productId,
                    productName = "ScenarioTest",
                    quantity = 2,
                    unitPrice = 15_000L,
                    unitCost = 10_000L,
                    lineRevenue = 0L,
                    lineCost = 0L
                )
            )
        )
        check("after sale stock", "13", getStock(productId).toString())

        // 4. PR with 3 units
        Log.i(TAG, "-- STEP 3: PR with 3 units --")
        container.prPackageRepository.save(
            PrPackageEntity(
                recipientName = "Scenario Influencer",
                recipientType = "INFLUENCER",
                status = "SENT",
                date = DateUtils.today(),
                productCost = 0L,
                packagingCost = 0L,
                courierCost = 0L,
                otherCost = 0L,
                totalCost = 0L
            ),
            listOf(
                PrPackageItemEntity(
                    packageId = 0L,
                    productId = productId,
                    productName = "ScenarioTest",
                    quantity = 3,
                    unitPrice = 15_000L,
                    lineCost = 0L
                )
            )
        )
        check("after PR stock", "10", getStock(productId).toString())

        // 5. Gift 2 units
        Log.i(TAG, "-- STEP 4: gift 2 units --")
        container.giftRepository.save(
            GiftEntity(
                recipient = "Scenario Friend",
                occasion = "Birthday",
                date = DateUtils.today(),
                productCost = 0L,
                packagingCost = 0L,
                courierCost = 0L,
                otherCost = 0L,
                totalCost = 0L
            ),
            listOf(
                GiftItemEntity(
                    giftId = 0L,
                    productId = productId,
                    productName = "ScenarioTest",
                    quantity = 2,
                    unitPrice = 15_000L,
                    lineCost = 0L
                )
            )
        )
        check("after gift stock", "8", getStock(productId).toString())

        // 6. FOC 1 unit
        Log.i(TAG, "-- STEP 5: FOC 1 unit --")
        container.focRepository.save(
            FocEntity(
                productId = productId,
                productName = "ScenarioTest",
                quantity = 1,
                unitPrice = 10_000L,
                lineCost = 0L,
                recipient = "Scenario Recipient",
                campaign = "Test",
                date = DateUtils.today()
            )
        )
        check("after FOC stock", "7", getStock(productId).toString())
        check("FOC does NOT create expense", "0", expenseCount().toString())

        // 7. Personal Use 1 unit -> SHOULD create expense
        Log.i(TAG, "-- STEP 6: personal use 1 unit --")
        container.stockMovementRepository.record(
            productId = productId,
            productName = "ScenarioTest",
            type = StockMovementType.PERSONAL_USE,
            delta = -1,
            note = "Scenario personal use"
        )
        check("after personal use stock", "6", getStock(productId).toString())
        check("Personal Use creates 1 expense", "1", expenseCount().toString())
        val puExpenses = expenses()
        if (puExpenses.isNotEmpty()) {
            check("Personal Use expense amount", "10000", puExpenses.first().amount.toString())
            check("Personal Use expense category", ExpenseCategory.PERSONAL_USE.name, puExpenses.first().category)
        }

        // 8. Damaged 1 unit -> NO expense
        Log.i(TAG, "-- STEP 7: damaged 1 unit --")
        container.stockMovementRepository.record(
            productId = productId,
            productName = "ScenarioTest",
            type = StockMovementType.DAMAGED,
            delta = -1,
            note = "Scenario damage"
        )
        check("after damaged stock", "5", getStock(productId).toString())
        check("Damaged does NOT create expense", "1", expenseCount().toString())

        // Final totals
        Log.i(TAG, "-- FINAL TOTALS --")
        val summary = container.reportsRepository.loadSummary(0L, Long.MAX_VALUE)
        check("total sales revenue", "Rs 300.00", summary.totalSales.format())
        check("total cost of goods sold", "Rs 200.00", summary.totalCostOfGoods.format())
        check("total profit on sales", "Rs 100.00", summary.totalProfit.format())
        check("total expenses (only Personal Use)", "Rs 100.00", summary.totalExpenses.format())
        check("total FOC value", "Rs 100.00", summary.totalFocCost.format())
        check("total purchases", "Rs 500.00", summary.totalPurchases.format())
        check("stock quantity", "5", summary.totalStockQuantity.toString())
        check("stock value", "Rs 500.00", summary.totalStockValue.format())

        Log.i(TAG, "############ $passed PASSED / $failed FAILED ############")
        Log.i(TAG, "")

        return failed == 0
    }
}
