package com.guddiz.app.core

import android.content.Context
import com.guddiz.app.data.local.GuddizDatabase
import com.guddiz.app.data.local.entity.ExpenseCategory
import com.guddiz.app.data.local.entity.ExpenseEntity
import com.guddiz.app.data.local.entity.ProductEntity
import com.guddiz.app.data.local.entity.SaleChannel
import com.guddiz.app.data.local.entity.SaleEntity
import com.guddiz.app.data.local.entity.SaleItemEntity
import com.guddiz.app.core.util.DateUtils
import com.guddiz.app.data.repository.SaleRepository

/**
 * Debug-only. Seeds known data so calculation totals can be verified.
 *
 * Expected totals after seeding (visible in the audit log):
 *   Sales count         = 2
 *   Total revenue       = Rs 3,097.00
 *   Total COGS          = Rs 2,150.00
 *   Total profit        = Rs 947.00
 *   Expenses count      = 1
 *   Total expenses      = Rs 250.00
 *   Stock quantity      = 138
 *   Stock value         = Rs 85,890.00
 */
object DebugSeeder {

    suspend fun seed(context: Context) {
        val db = GuddizDatabase.get(context)
        val now = System.currentTimeMillis()
        val today = DateUtils.today()
        val yesterday = today - 24L * 60L * 60L * 1000L

        // ---- 6 products ----
        val products = listOf(
            ProductEntity(
                name = "Vitamin C Serum", sku = "VCS-30", productCode = "GZ001",
                category = "SKINCARE", brand = "GlowLab", supplier = "Ali Cosmetics",
                purchasePrice = 85000, prs = 129900, sellingPrice = 119900,
                currentQuantity = 24, minStockLevel = 5,
                batchNumber = "B2401", notes = "Best seller",
                createdAt = now, updatedAt = now
            ),
            ProductEntity(
                name = "Rose Face Wash", sku = "RFW-100", productCode = "GZ002",
                category = "SKINCARE", brand = "Herbal Co", supplier = "Ali Cosmetics",
                purchasePrice = 45000, prs = 74900, sellingPrice = 69900,
                currentQuantity = 42, minStockLevel = 10,
                batchNumber = "B2402",
                createdAt = now, updatedAt = now
            ),
            ProductEntity(
                name = "Herbal Shampoo", sku = "HS-250", productCode = "GZ003",
                category = "HAIRCARE", brand = "Herbal Co", supplier = "Hassan Traders",
                purchasePrice = 62000, prs = 99900, sellingPrice = 89900,
                currentQuantity = 18, minStockLevel = 6,
                batchNumber = "B2403",
                createdAt = now, updatedAt = now
            ),
            ProductEntity(
                name = "Matte Lipstick Red", sku = "MLR-05", productCode = "GZ004",
                category = "MAKEUP", brand = "ColorPop", supplier = "Beauty Hub",
                purchasePrice = 55000, prs = 89900, sellingPrice = 79900,
                currentQuantity = 35, minStockLevel = 8,
                batchNumber = "B2404", notes = "Shade #5",
                createdAt = now, updatedAt = now
            ),
            ProductEntity(
                name = "Body Butter Cocoa", sku = "BBC-200", productCode = "GZ005",
                category = "BODY_CARE", brand = "GlowLab", supplier = "Hassan Traders",
                purchasePrice = 78000, prs = 119900, sellingPrice = 109900,
                currentQuantity = 12, minStockLevel = 5,
                batchNumber = "B2405",
                createdAt = now, updatedAt = now
            ),
            ProductEntity(
                name = "Lavender Essential Oil", sku = "LEO-10", productCode = "GZ006",
                category = "HERBAL", brand = "Pure Herbs", supplier = "Herbal Supplies",
                purchasePrice = 95000, prs = 149900, sellingPrice = 139900,
                currentQuantity = 8, minStockLevel = 4,
                batchNumber = "B2406", notes = "Herbal line",
                createdAt = now, updatedAt = now
            )
        )

        val ids = products.map { db.productDao().insert(it) }

        // Known sale seed: Vitamin C Serum x2 @ 1199  →  revenue Rs 2,398, cost Rs 1,700
        val saleRepo = SaleRepository(
            saleDao = db.saleDao(),
            productDao = db.productDao(),
            movementDao = db.stockMovementDao()
        )

        // ---- Sale 1: today, Vitamin C Serum ×2 ----
        saleRepo.save(
            SaleEntity(
                saleDate = today,
                channel = SaleChannel.WALK_IN.name,
                customer = null,
                totalRevenue = 0L,
                totalCost = 0L,
                notes = null
            ),
            listOf(
                SaleItemEntity(
                    saleId = 0L,
                    productId = ids[0],
                    productName = "Vitamin C Serum",
                    quantity = 2,
                    unitPrice = 119900,
                    unitCost = 85000,
                    lineRevenue = 0L,
                    lineCost = 0L
                )
            )
        )

        // ---- Sale 2: yesterday, Rose Face Wash ×1 ----
        saleRepo.save(
            SaleEntity(
                saleDate = yesterday,
                channel = SaleChannel.WHATSAPP.name,
                customer = null,
                totalRevenue = 0L,
                totalCost = 0L,
                notes = null
            ),
            listOf(
                SaleItemEntity(
                    saleId = 0L,
                    productId = ids[1],
                    productName = "Rose Face Wash",
                    quantity = 1,
                    unitPrice = 69900,
                    unitCost = 45000,
                    lineRevenue = 0L,
                    lineCost = 0L
                )
            )
        )

        // ---- Expense: today, courier Rs 250 ----
        db.expenseDao().insert(
            ExpenseEntity(
                category = ExpenseCategory.COURIER.name,
                amount = 25000,
                expenseDate = today,
                vendor = "Test Courier",
                description = "Sample courier charge",
                relatedProductId = null,
                receiptPath = null,
                notes = null
            )
        )
    }
}
