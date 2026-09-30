package com.guddiz.app.data.repository

import android.util.Log
import com.guddiz.app.core.calculation.CostCalculator
import com.guddiz.app.core.money.Money
import com.guddiz.app.data.local.dao.ProductDao
import com.guddiz.app.data.local.dao.PurchaseDao
import com.guddiz.app.data.local.dao.StockMovementDao
import com.guddiz.app.data.local.entity.PurchaseEntity
import com.guddiz.app.data.local.entity.StockMovementEntity
import com.guddiz.app.data.local.entity.StockMovementType
import kotlinx.coroutines.flow.Flow

class PurchaseRepository(
    private val purchaseDao: PurchaseDao,
    private val productDao: ProductDao,
    private val movementDao: StockMovementDao
) {

    fun observePurchases(query: String = ""): Flow<List<PurchaseEntity>> =
        purchaseDao.observeFiltered(query.trim())

    suspend fun getPurchase(id: Long): PurchaseEntity? = purchaseDao.getById(id)

    suspend fun save(entity: PurchaseEntity, previousQuantity: Int = 0): Long {
        val now = System.currentTimeMillis()

        val breakdown = CostCalculator.calculatePurchaseCost(
            quantity = entity.quantity,
            unitPrice = Money(entity.unitPrice),
            packagingCost = Money(entity.packagingCost),
            shippingCost = Money(entity.shippingCost),
            otherCost = Money(entity.otherCost)
        )
        val toSave = entity.copy(totalCost = breakdown.totalCost.paisa)

        val savedId = if (toSave.id == 0L) {
            purchaseDao.insert(toSave.copy(createdAt = now, updatedAt = now))
        } else {
            purchaseDao.update(toSave.copy(updatedAt = now))
            toSave.id
        }

        val delta = entity.quantity - previousQuantity
        productDao.getById(entity.productId)?.let { product ->
            val before = product.currentQuantity
            if (delta != 0) {
                val after = (before + delta).coerceAtLeast(0)
                productDao.update(
                    product.copy(
                        currentQuantity = after,
                        updatedAt = now
                    )
                )
                Log.i("GuddizStock", "PURCHASE: " + product.name + " " + before + " -> " + after + " (delta " + delta + ")")
            }
        }

        movementDao.deleteByReference("purchase", savedId)
        if (delta != 0) {
            movementDao.insert(
                StockMovementEntity(
                    productId = entity.productId,
                    productName = entity.productName,
                    type = StockMovementType.PURCHASE.name,
                    delta = delta,
                    referenceType = "purchase",
                    referenceId = savedId,
                    note = "Purchase",
                    date = entity.purchaseDate
                )
            )
        }

        return savedId
    }

    suspend fun softDelete(entity: PurchaseEntity) {
        purchaseDao.softDelete(entity.id, System.currentTimeMillis())
        productDao.getById(entity.productId)?.let { product ->
            productDao.update(
                product.copy(
                    currentQuantity = (product.currentQuantity - entity.quantity).coerceAtLeast(0),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        movementDao.deleteByReference("purchase", entity.id)
    }

    suspend fun restore(entity: PurchaseEntity) {
        purchaseDao.restore(entity.id, System.currentTimeMillis())
        productDao.getById(entity.productId)?.let { product ->
            productDao.update(
                product.copy(
                    currentQuantity = product.currentQuantity + entity.quantity,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        movementDao.insert(
            StockMovementEntity(
                productId = entity.productId,
                productName = entity.productName,
                type = StockMovementType.PURCHASE.name,
                delta = entity.quantity,
                referenceType = "purchase",
                referenceId = entity.id,
                note = "Purchase (restored)",
                date = entity.purchaseDate
            )
        )
    }

    suspend fun hardDelete(id: Long) = purchaseDao.hardDelete(id)

    suspend fun duplicate(id: Long): Long? {
        val original = purchaseDao.getById(id) ?: return null
        val now = System.currentTimeMillis()
        val copy = original.copy(
            id = 0L,
            purchaseDate = com.guddiz.app.core.util.DateUtils.today(),
            createdAt = now,
            updatedAt = now
        )
        return save(copy)
    }

    fun observeDeleted(): Flow<List<PurchaseEntity>> = purchaseDao.observeDeleted()
    suspend fun purgeDeletedBefore(cutoff: Long): List<PurchaseEntity> =
        purchaseDao.findDeletedBefore(cutoff)

    fun observeTotalPurchaseCost(): Flow<Long> = purchaseDao.observeTotalPurchaseCost()
    fun observeTotalPackaging(): Flow<Long> = purchaseDao.observeTotalPackaging()
    fun observeTotalShipping(): Flow<Long> = purchaseDao.observeTotalShipping()
}
