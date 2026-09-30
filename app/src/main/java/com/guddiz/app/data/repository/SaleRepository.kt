package com.guddiz.app.data.repository

import com.guddiz.app.core.money.Money
import com.guddiz.app.data.local.dao.ProductDao
import com.guddiz.app.data.local.dao.SaleDao
import com.guddiz.app.data.local.dao.StockMovementDao
import com.guddiz.app.data.local.entity.SaleEntity
import com.guddiz.app.data.local.entity.SaleItemEntity
import com.guddiz.app.data.local.entity.StockMovementEntity
import com.guddiz.app.data.local.entity.StockMovementType
import com.guddiz.app.data.model.DailyRevenue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class SaleRepository(
    private val saleDao: SaleDao,
    private val productDao: ProductDao,
    private val movementDao: StockMovementDao
) {

    fun observeSales(query: String = "", channel: String = ""): Flow<List<SaleEntity>> =
        saleDao.observeFiltered(query.trim(), channel)

    suspend fun getSale(id: Long): SaleEntity? = saleDao.getById(id)
    suspend fun getItems(id: Long): List<SaleItemEntity> = saleDao.getItems(id)

    suspend fun save(entity: SaleEntity, items: List<SaleItemEntity>): Long {
        val now = System.currentTimeMillis()

        val lineRevenue = Money.sum(items.map { Money(it.unitPrice) * it.quantity })
        val lineCost = Money.sum(items.map { Money(it.unitCost) * it.quantity })

        val stored = items.map {
            it.copy(
                lineRevenue = (Money(it.unitPrice) * it.quantity).paisa,
                lineCost = (Money(it.unitCost) * it.quantity).paisa
            )
        }

        val summary = buildItemSummary(stored)
        val totalUnits = stored.sumOf { it.quantity }

        val toSave = entity.copy(
            totalRevenue = lineRevenue.paisa,
            totalCost = lineCost.paisa,
            itemSummary = summary,
            itemCount = totalUnits
        )

        val id = saleDao.saveWithItems(
            toSave.copy(
                createdAt = if (toSave.id == 0L) now else toSave.createdAt,
                updatedAt = now
            ),
            stored
        )

        reverseStockFor(id)
        applyStockFor(id, stored, entity.saleDate)
        return id
    }

    suspend fun softDelete(entity: SaleEntity) {
        reverseStockFor(entity.id)
        saleDao.softDelete(entity.id, System.currentTimeMillis())
    }

    suspend fun restore(entity: SaleEntity) {
        saleDao.restore(entity.id, System.currentTimeMillis())
        val items = saleDao.getItems(entity.id)
        applyStockFor(entity.id, items, entity.saleDate)
    }

    suspend fun hardDelete(id: Long) = saleDao.hardDelete(id)

    suspend fun duplicate(id: Long): Long? {
        val original = saleDao.getById(id) ?: return null
        val items = saleDao.getItems(id)
        val now = System.currentTimeMillis()
        val copy = original.copy(
            id = 0L,
            saleDate = com.guddiz.app.core.util.DateUtils.today(),
            createdAt = now,
            updatedAt = now
        )
        val newId = saleDao.saveWithItems(copy, items.map { it.copy(id = 0L, saleId = 0L) })
        applyStockFor(newId, items.map { it.copy(saleId = newId) }, copy.saleDate)
        return newId
    }

    fun observeDeleted(): Flow<List<SaleEntity>> = saleDao.observeDeleted()
    suspend fun purgeDeletedBefore(cutoff: Long): List<SaleEntity> = saleDao.findDeletedBefore(cutoff)

    fun observeTotalRevenue(): Flow<Long> = saleDao.observeTotalRevenue()
    fun observeTotalCostOfGoods(): Flow<Long> = saleDao.observeTotalCostOfGoods()
    fun observeRevenueSince(fromMillis: Long): Flow<Long> = saleDao.observeRevenueSince(fromMillis)
    fun observeRevenueBetween(from: Long, to: Long): Flow<Long> = saleDao.observeRevenueBetween(from, to)
    suspend fun recentCustomers(limit: Int = 6): List<String> = saleDao.recentCustomers(limit)

    fun observeLast7DaysRevenue(): Flow<List<DailyRevenue>> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_YEAR, -6)
        val weekStart = cal.timeInMillis

        return saleDao.observeSalesSince(weekStart).map { rows ->
            val byDay = mutableMapOf<Long, Long>()
            rows.forEach { r ->
                val day = com.guddiz.app.core.util.DateUtils.startOfDay(r.saleDate)
                byDay[day] = (byDay[day] ?: 0L) + r.totalRevenue
            }
            val buckets = mutableListOf<DailyRevenue>()
            val walker = Calendar.getInstance().apply { timeInMillis = weekStart }
            repeat(7) {
                val dayMillis = walker.timeInMillis
                buckets.add(
                    DailyRevenue(
                        dayMillis = dayMillis,
                        dayLabel = com.guddiz.app.core.util.DateUtils.weekdayShort(dayMillis),
                        totalPaisa = byDay[dayMillis] ?: 0L
                    )
                )
                walker.add(Calendar.DAY_OF_YEAR, 1)
            }
            buckets
        }
    }

    /** Human-readable summary like "Vitamin C Serum ×2, Rose Face Wash ×1". */
    private fun buildItemSummary(items: List<SaleItemEntity>): String {
        if (items.isEmpty()) return ""
        val named = items.map { it.productName + " ×" + it.quantity }
        val head = named.take(2).joinToString(", ")
        val extra = named.size - 2
        return if (extra > 0) head + "  +" + extra + " more" else head
    }

    private suspend fun applyStockFor(
        saleId: Long,
        items: List<SaleItemEntity>,
        date: Long
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
                    type = StockMovementType.SALE.name,
                    delta = -item.quantity,
                    referenceType = "sale",
                    referenceId = saleId,
                    note = "Sold",
                    date = date
                )
            )
        }
    }

    private suspend fun reverseStockFor(saleId: Long) {
        val previous = movementDao.findByReference("sale", saleId)
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
        movementDao.deleteByReference("sale", saleId)
    }
}
