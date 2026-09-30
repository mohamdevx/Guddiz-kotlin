package com.guddiz.app.data.repository

import com.guddiz.app.core.calculation.CostCalculator
import com.guddiz.app.core.money.Money
import com.guddiz.app.data.local.dao.GiftDao
import com.guddiz.app.data.local.dao.ProductDao
import com.guddiz.app.data.local.dao.StockMovementDao
import com.guddiz.app.data.local.entity.GiftEntity
import com.guddiz.app.data.local.entity.GiftItemEntity
import com.guddiz.app.data.local.entity.StockMovementEntity
import com.guddiz.app.data.local.entity.StockMovementType
import kotlinx.coroutines.flow.Flow

class GiftRepository(
    private val giftDao: GiftDao,
    private val productDao: ProductDao,
    private val movementDao: StockMovementDao
) {

    fun observeGifts(query: String = ""): Flow<List<GiftEntity>> =
        giftDao.observeFiltered(query.trim())

    suspend fun getGift(id: Long): GiftEntity? = giftDao.getById(id)
    suspend fun getItems(id: Long): List<GiftItemEntity> = giftDao.getItems(id)

    suspend fun save(g: GiftEntity, items: List<GiftItemEntity>): Long {
        val now = System.currentTimeMillis()
        val lines = items.map { CostCalculator.Line(it.quantity, Money(it.unitPrice)) }
        val breakdown = CostCalculator.calculatePackageCost(
            lines = lines,
            packagingCost = Money(g.packagingCost),
            courierCost = Money(g.courierCost),
            otherCost = Money(g.otherCost)
        )
        val storedItems = items.mapIndexed { idx, it -> it.copy(lineCost = lines[idx].lineCost.paisa) }
        val toSave = g.copy(
            productCost = breakdown.productCost.paisa,
            totalCost = breakdown.totalCost.paisa
        )
        val id = giftDao.saveWithItems(
            toSave.copy(
                createdAt = if (toSave.id == 0L) now else toSave.createdAt,
                updatedAt = now
            ),
            storedItems
        )
        reverseStockFor(id)
        applyStockFor(id, toSave.recipient, storedItems, g.date)
        return id
    }

    suspend fun softDelete(g: GiftEntity) {
        reverseStockFor(g.id)
        giftDao.softDelete(g.id, System.currentTimeMillis())
    }

    suspend fun restore(g: GiftEntity) {
        giftDao.restore(g.id, System.currentTimeMillis())
        val items = giftDao.getItems(g.id)
        applyStockFor(g.id, g.recipient, items, g.date)
    }

    suspend fun hardDelete(id: Long) = giftDao.hardDelete(id)

    suspend fun duplicate(id: Long): Long? {
        val original = giftDao.getById(id) ?: return null
        val items = giftDao.getItems(id)
        val now = System.currentTimeMillis()
        val copy = original.copy(
            id = 0L,
            date = com.guddiz.app.core.util.DateUtils.today(),
            createdAt = now,
            updatedAt = now
        )
        val newId = giftDao.saveWithItems(copy, items.map { it.copy(id = 0L, giftId = 0L) })
        applyStockFor(newId, copy.recipient, items.map { it.copy(giftId = newId) }, copy.date)
        return newId
    }

    fun observeDeleted(): Flow<List<GiftEntity>> = giftDao.observeDeleted()
    suspend fun purgeDeletedBefore(cutoff: Long): List<GiftEntity> = giftDao.findDeletedBefore(cutoff)

    private suspend fun applyStockFor(
        giftId: Long, recipient: String,
        items: List<GiftItemEntity>, date: Long
    ) {
        items.forEach { item ->
            productDao.getById(item.productId)?.let { product ->
                productDao.update(
                    product.copy(
                        currentQuantity = (product.currentQuantity - item.quantity).coerceAtLeast(0),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            movementDao.insert(
                StockMovementEntity(
                    productId = item.productId,
                    productName = item.productName,
                    type = StockMovementType.GIFT.name,
                    delta = -item.quantity,
                    referenceType = "gift",
                    referenceId = giftId,
                    note = "Gift to $recipient",
                    date = date
                )
            )
        }
    }

    private suspend fun reverseStockFor(giftId: Long) {
        val previous = movementDao.findByReference("gift", giftId)
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
        movementDao.deleteByReference("gift", giftId)
    }

    fun observeTotalGiftCost(): Flow<Long> = giftDao.observeTotalGiftCost()
    suspend fun recentRecipients(limit: Int = 6): List<String> = giftDao.recentRecipients(limit)
}
