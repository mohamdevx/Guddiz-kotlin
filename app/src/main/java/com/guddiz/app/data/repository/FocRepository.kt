package com.guddiz.app.data.repository

import android.util.Log
import com.guddiz.app.core.calculation.CostCalculator
import com.guddiz.app.core.money.Money
import com.guddiz.app.data.local.dao.FocDao
import com.guddiz.app.data.local.dao.ProductDao
import com.guddiz.app.data.local.dao.StockMovementDao
import com.guddiz.app.data.local.entity.FocEntity
import com.guddiz.app.data.local.entity.StockMovementEntity
import com.guddiz.app.data.local.entity.StockMovementType
import kotlinx.coroutines.flow.Flow

class FocRepository(
    private val focDao: FocDao,
    private val productDao: ProductDao,
    private val movementDao: StockMovementDao
) {

    fun observeFoc(query: String = ""): Flow<List<FocEntity>> =
        focDao.observeFiltered(query.trim())

    suspend fun getFoc(id: Long): FocEntity? = focDao.getById(id)

    suspend fun save(f: FocEntity): Long {
        Log.i("GuddizFOC", "save() called: productId=" + f.productId + " qty=" + f.quantity + " recipient=" + f.recipient)
        val now = System.currentTimeMillis()
        val line = CostCalculator.Line(f.quantity, Money(f.unitPrice))
        val toSave = f.copy(
            lineCost = line.lineCost.paisa,
            createdAt = if (f.id == 0L) now else f.createdAt,
            updatedAt = now
        )
        val id = if (toSave.id == 0L) focDao.insert(toSave) else {
            focDao.update(toSave); toSave.id
        }
        reverseStockFor(id)
        applyStockFor(id, toSave.recipient, toSave.productId, toSave.productName,
            toSave.quantity, f.date)
        return id
    }

    suspend fun softDelete(f: FocEntity) {
        reverseStockFor(f.id)
        focDao.softDelete(f.id, System.currentTimeMillis())
    }

    suspend fun restore(f: FocEntity) {
        focDao.restore(f.id, System.currentTimeMillis())
        applyStockFor(f.id, f.recipient, f.productId, f.productName, f.quantity, f.date)
    }

    suspend fun hardDelete(id: Long) = focDao.hardDelete(id)

    suspend fun duplicate(id: Long): Long? {
        val original = focDao.getById(id) ?: return null
        val now = System.currentTimeMillis()
        val copy = original.copy(
            id = 0L,
            date = com.guddiz.app.core.util.DateUtils.today(),
            createdAt = now,
            updatedAt = now
        )
        return save(copy)
    }

    fun observeDeleted(): Flow<List<FocEntity>> = focDao.observeDeleted()
    suspend fun purgeDeletedBefore(cutoff: Long): List<FocEntity> = focDao.findDeletedBefore(cutoff)

    private suspend fun applyStockFor(
        focId: Long, recipient: String,
        productId: Long, productName: String,
        qty: Int, date: Long
    ) {
        productDao.getById(productId)?.let { product ->
            val before = product.currentQuantity
            val after = (before - qty).coerceAtLeast(0)
            productDao.update(
                product.copy(
                    currentQuantity = after,
                    updatedAt = System.currentTimeMillis()
                )
            )
            Log.i("GuddizStock", "FOC: " + productName + " " + before + " -> " + after)
        }
        movementDao.insert(
            StockMovementEntity(
                productId = productId,
                productName = productName,
                type = StockMovementType.FOC.name,
                delta = -qty,
                referenceType = "foc",
                referenceId = focId,
                note = "FOC to $recipient",
                date = date
            )
        )
    }

    private suspend fun reverseStockFor(focId: Long) {
        val previous = movementDao.findByReference("foc", focId)
        previous.forEach { m ->
            productDao.getById(m.productId)?.let { product ->
                productDao.update(
                    product.copy(
                        currentQuantity = (product.currentQuantity - m.delta).coerceAtLeast(0),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
        movementDao.deleteByReference("foc", focId)
    }

    fun observeTotalFocCost(): Flow<Long> = focDao.observeTotalFocCost()
    suspend fun recentRecipients(limit: Int = 6): List<String> = focDao.recentRecipients(limit)
}
