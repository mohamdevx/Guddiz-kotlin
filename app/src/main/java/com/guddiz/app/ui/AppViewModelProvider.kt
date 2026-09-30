package com.guddiz.app.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.guddiz.app.GuddizApplication
import com.guddiz.app.ui.backup.BackupViewModel
import com.guddiz.app.ui.dashboard.DashboardViewModel
import com.guddiz.app.ui.expenses.ExpenseFormViewModel
import com.guddiz.app.ui.expenses.ExpenseListViewModel
import com.guddiz.app.ui.foc.FocFormViewModel
import com.guddiz.app.ui.foc.FocListViewModel
import com.guddiz.app.ui.gifts.GiftFormViewModel
import com.guddiz.app.ui.gifts.GiftListViewModel
import com.guddiz.app.ui.pr.PrFormViewModel
import com.guddiz.app.ui.pr.PrListViewModel
import com.guddiz.app.ui.products.ProductDetailViewModel
import com.guddiz.app.ui.products.ProductFormViewModel
import com.guddiz.app.ui.products.ProductListViewModel
import com.guddiz.app.ui.products.ProductQuickRecordViewModel
import com.guddiz.app.ui.products.StockAdjustmentViewModel
import com.guddiz.app.ui.products.StockMovementViewModel
import com.guddiz.app.ui.purchases.PurchaseFormViewModel
import com.guddiz.app.ui.purchases.PurchaseListViewModel
import com.guddiz.app.ui.records.RecordsViewModel
import com.guddiz.app.ui.recyclebin.RecycleBinViewModel
import com.guddiz.app.ui.reports.ReportsViewModel
import com.guddiz.app.ui.sales.SaleFormViewModel
import com.guddiz.app.ui.sales.SaleListViewModel

object AppViewModelProvider {

    val Factory = viewModelFactory {
        initializer {
            DashboardViewModel(
                repository = guddizApplication().container.dashboardRepository,
                saleRepository = guddizApplication().container.saleRepository
            )
        }
        initializer { ReportsViewModel(guddizApplication().container.reportsRepository) }
        initializer { BackupViewModel(guddizApplication()) }
        initializer { RecordsViewModel(guddizApplication().container.recordsRepository) }
        initializer { RecycleBinViewModel(guddizApplication().container.recycleBinRepository) }

        initializer { ProductListViewModel(guddizApplication().container.productRepository) }
        initializer {
            ProductFormViewModel(
                repository = guddizApplication().container.productRepository,
                savedStateHandle = createSavedStateHandle()
            )
        }
        initializer {
            ProductDetailViewModel(
                repository = guddizApplication().container.productRepository,
                savedStateHandle = createSavedStateHandle()
            )
        }
        initializer {
            StockAdjustmentViewModel(
                productRepository = guddizApplication().container.productRepository,
                movementRepository = guddizApplication().container.stockMovementRepository,
                savedStateHandle = createSavedStateHandle()
            )
        }
        initializer {
            StockMovementViewModel(guddizApplication().container.stockMovementRepository)
        }
        initializer {
            ProductQuickRecordViewModel(
                saleRepository = guddizApplication().container.saleRepository,
                purchaseRepository = guddizApplication().container.purchaseRepository
            )
        }

        initializer { SaleListViewModel(guddizApplication().container.saleRepository) }
        initializer {
            SaleFormViewModel(
                repository = guddizApplication().container.saleRepository,
                productRepository = guddizApplication().container.productRepository,
                savedStateHandle = createSavedStateHandle()
            )
        }

        initializer { PurchaseListViewModel(guddizApplication().container.purchaseRepository) }
        initializer {
            PurchaseFormViewModel(
                purchaseRepository = guddizApplication().container.purchaseRepository,
                productRepository = guddizApplication().container.productRepository,
                savedStateHandle = createSavedStateHandle()
            )
        }
        initializer { ExpenseListViewModel(guddizApplication().container.expenseRepository) }
        initializer {
            ExpenseFormViewModel(
                repository = guddizApplication().container.expenseRepository,
                savedStateHandle = createSavedStateHandle()
            )
        }
        initializer { PrListViewModel(guddizApplication().container.prPackageRepository) }
        initializer {
            PrFormViewModel(
                repository = guddizApplication().container.prPackageRepository,
                productRepository = guddizApplication().container.productRepository,
                savedStateHandle = createSavedStateHandle()
            )
        }
        initializer { GiftListViewModel(guddizApplication().container.giftRepository) }
        initializer {
            GiftFormViewModel(
                repository = guddizApplication().container.giftRepository,
                productRepository = guddizApplication().container.productRepository,
                savedStateHandle = createSavedStateHandle()
            )
        }
        initializer { FocListViewModel(guddizApplication().container.focRepository) }
        initializer {
            FocFormViewModel(
                repository = guddizApplication().container.focRepository,
                productRepository = guddizApplication().container.productRepository,
                savedStateHandle = createSavedStateHandle()
            )
        }
    }
}

private fun CreationExtras.guddizApplication(): GuddizApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GuddizApplication
