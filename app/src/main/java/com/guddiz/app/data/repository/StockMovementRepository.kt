package com.guddiz.app.data.repository

import com.guddiz.app.core.money.Money
import com.guddiz.app.data.local.dao.ExpenseDao
import com.guddiz.app.data.local.dao.ProductDao
import com.guddiz.app.data.local.dao.StockMovementDao
import com.guddiz.app.data.local.entity.ExpenseCategory
import com.guddiz.app.data.local.entity.ExpenseEntity
import com.guddiz.app.data.local.entity.StockMovementEntity
import com.guddiz.app.data.local.entity.StockMovementType
import kotlinx.coroutines.flow.Flow

class StockMovementRepository(
    private val movementDao: StockMovementDao,
    private val productDao: ProductDao,
    private val expenseDao: ExpenseDao
) {
    fun observeForProduct(productId: Long): Flow<List<StockMovementEntity>> =
        movementDao.observeForProduct(productId)

    suspend fun record(
        productId: Long,
        productName: String,
        type: StockMovementType,
        delta: Int,
        note: String?
    ) {
        if (delta == 0) return
        val now = System.currentTimeMillis()
        val product = productDao.getById(productId) ?: return
        val newQty = (product.currentQuantity + delta).coerceAtLeast(0)
        productDao.update(product.copy(currentQuantity = newQty, updatedAt = now))

        movementDao.insert(
            StockMovementEntity(
                productId = productId,
                productName = productName,
                type = type.name,
                delta = delta,
                referenceType = "manual",
                referenceId = null,
                note = note?.takeIf { it.isNotBlank() },
                date = now
            )
        )

        val typeName = type.name
        val isPersonalUse = typeName == "PERSONAL_USE"
        if (isPersonalUse && delta < 0) {
            val unitsTaken = -delta
            val unitCost = product.purchasePrice
            val totalValue = Money(unitCost) * unitsTaken
            val expense = ExpenseEntity(
                category = ExpenseCategory.PERSONAL_USE.name,
                amount = totalValue.paisa,
                expenseDate = now,
                vendor = null,
                description = "Personal use: " + productName + " (x" + unitsTaken + ")",
                relatedProductId = productId,
                receiptPath = null,
                notes = note?.takeIf { it.isNotBlank() }
            )
            expenseDao.insert(expense)
        }
    }
}
