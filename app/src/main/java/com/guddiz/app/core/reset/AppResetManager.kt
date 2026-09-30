package com.guddiz.app.core.reset

import android.content.Context
import com.guddiz.app.core.receipt.ReceiptStorage
import com.guddiz.app.data.local.GuddizDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface ResetResult {
    data object Success : ResetResult
    data class Error(val message: String) : ResetResult
}

class AppResetManager(private val context: Context) {

    suspend fun resetEverything(): ResetResult = withContext(Dispatchers.IO) {
        try {
            val db = GuddizDatabase.get(context)

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

            try {
                val dir = ReceiptStorage.receiptsDir(context)
                dir.listFiles()?.forEach { it.delete() }
            } catch (_: Exception) { }

            try {
                context.getSharedPreferences("guddiz_backup", Context.MODE_PRIVATE)
                    .edit().clear().apply()
                context.getSharedPreferences("debug_seed", Context.MODE_PRIVATE)
                    .edit().clear().apply()
            } catch (_: Exception) { }

            ResetResult.Success
        } catch (e: Exception) {
            ResetResult.Error(e.message ?: "Could not clear data.")
        }
    }
}
