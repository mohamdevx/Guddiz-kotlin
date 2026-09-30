package com.guddiz.app.di

import android.content.Context
import com.guddiz.app.core.backup.BackupManager
import com.guddiz.app.core.reset.AppResetManager
import com.guddiz.app.data.local.GuddizDatabase
import com.guddiz.app.data.repository.DashboardRepository
import com.guddiz.app.data.repository.ExpenseRepository
import com.guddiz.app.data.repository.FocRepository
import com.guddiz.app.data.repository.GiftRepository
import com.guddiz.app.data.repository.PrPackageRepository
import com.guddiz.app.data.repository.ProductRepository
import com.guddiz.app.data.repository.PurchaseRepository
import com.guddiz.app.data.repository.RecordsRepository
import com.guddiz.app.data.repository.RecycleBinRepository
import com.guddiz.app.data.repository.ReportsRepository
import com.guddiz.app.data.repository.SaleRepository
import com.guddiz.app.data.repository.StockMovementRepository

class AppContainer(context: Context) {

    private val database = GuddizDatabase.get(context)

    val productRepository = ProductRepository(database.productDao())

    val saleRepository = SaleRepository(
        saleDao = database.saleDao(),
        productDao = database.productDao(),
        movementDao = database.stockMovementDao()
    )

    val purchaseRepository = PurchaseRepository(
        purchaseDao = database.purchaseDao(),
        productDao = database.productDao(),
        movementDao = database.stockMovementDao()
    )

    val expenseRepository = ExpenseRepository(database.expenseDao())

    val prPackageRepository = PrPackageRepository(
        prDao = database.prPackageDao(),
        productDao = database.productDao(),
        movementDao = database.stockMovementDao()
    )

    val giftRepository = GiftRepository(
        giftDao = database.giftDao(),
        productDao = database.productDao(),
        movementDao = database.stockMovementDao()
    )

    val focRepository = FocRepository(
        focDao = database.focDao(),
        productDao = database.productDao(),
        movementDao = database.stockMovementDao()
    )

    val stockMovementRepository = StockMovementRepository(
        movementDao = database.stockMovementDao(),
        productDao = database.productDao(),
        expenseDao = database.expenseDao()
    )

    val recordsRepository = RecordsRepository(
        saleDao = database.saleDao(),
        purchaseDao = database.purchaseDao(),
        expenseDao = database.expenseDao(),
        prDao = database.prPackageDao(),
        giftDao = database.giftDao(),
        focDao = database.focDao()
    )

    val recycleBinRepository = RecycleBinRepository(
        productRepository = productRepository,
        saleRepository = saleRepository,
        purchaseRepository = purchaseRepository,
        expenseRepository = expenseRepository,
        prRepository = prPackageRepository,
        giftRepository = giftRepository,
        focRepository = focRepository
    )

    val dashboardRepository = DashboardRepository(
        productDao = database.productDao(),
        saleDao = database.saleDao(),
        purchaseDao = database.purchaseDao(),
        expenseDao = database.expenseDao(),
        prDao = database.prPackageDao(),
        giftDao = database.giftDao(),
        focDao = database.focDao()
    )

    val reportsRepository = ReportsRepository(
        productDao = database.productDao(),
        saleDao = database.saleDao(),
        purchaseDao = database.purchaseDao(),
        expenseDao = database.expenseDao(),
        prDao = database.prPackageDao(),
        giftDao = database.giftDao(),
        focDao = database.focDao()
    )

    val backupManager = BackupManager(context)
    val resetManager = AppResetManager(context)
}
