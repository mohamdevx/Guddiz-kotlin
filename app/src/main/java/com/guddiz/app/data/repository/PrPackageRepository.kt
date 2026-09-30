package com.guddiz.app.data.repository

import com.guddiz.app.core.calculation.CostCalculator
import com.guddiz.app.core.money.Money
import com.guddiz.app.data.local.dao.PrPackageDao
import com.guddiz.app.data.local.dao.ProductDao
import com.guddiz.app.data.local.dao.StockMovementDao
import com.guddiz.app.data.local.entity.PrPackageEntity
import com.guddiz.app.data.local.entity.PrPackageItemEntity
import com.guddiz.app.data.local.entity.StockMovementEntity
import com.guddiz.app.data.local.entity.StockMovementType
import kotlinx.coroutines.flow.Flow

class PrPackageRepository(
    private val prDao: PrPackageDao,
    private val productDao: ProductDao,
    private val movementDao: StockMovementDao
) {

    fun observePackages(query: String = ""): Flow<List<PrPackageEntity>> =
        prDao.observeFiltered(query.trim())

    fun observeItems(packageId: Long): Flow<List<PrPackageItemEntity>> =
        prDao.observeItems(packageId)

    suspend fun getPackage(id: Long): PrPackageEntity? = prDao.getById(id)
    suspend fun getItems(id: Long): List<PrPackageItemEntity> = prDao.getItems(id)

    suspend fun save(pkg: PrPackageEntity, items: List<PrPackageItemEntity>): Long {
        val now = System.currentTimeMillis()
        val lines = items.map { CostCalculator.Line(it.quantity, Money(it.unitPrice)) }
        val breakdown = CostCalculator.calculatePackageCost(
            lines = lines,
            packagingCost = Money(pkg.packagingCost),
            courierCost = Money(pkg.courierCost),
            otherCost = Money(pkg.otherCost)
        )
        val storedItems = items.mapIndexed { idx, it -> it.copy(lineCost = lines[idx].lineCost.paisa) }
        val toSave = pkg.copy(
            productCost = breakdown.productCost.paisa,
            totalCost = breakdown.totalCost.paisa
        )
        val id = prDao.saveWithItems(
            toSave.copy(
                createdAt = if (toSave.id == 0L) now else toSave.createdAt,
                updatedAt = now
            ),
            storedItems
        )
        reverseStockFor(id)
        applyStockFor(id, toSave.recipientName, storedItems, pkg.date)
        return id
    }

    suspend fun softDelete(pkg: PrPackageEntity) {
        reverseStockFor(pkg.id)
        prDao.softDelete(pkg.id, System.currentTimeMillis())
    }

    suspend fun restore(pkg: PrPackageEntity) {
        prDao.restore(pkg.id, System.currentTimeMillis())
        val items = prDao.getItems(pkg.id)
        applyStockFor(pkg.id, pkg.recipientName, items, pkg.date)
    }

    suspend fun hardDelete(id: Long) = prDao.hardDelete(id)

    suspend fun duplicate(id: Long): Long? {
        val original = prDao.getById(id) ?: return null
        val items = prDao.getItems(id)
        val now = System.currentTimeMillis()
        val copy = original.copy(
            id = 0L,
            date = com.guddiz.app.core.util.DateUtils.today(),
            status = "PLANNED",
            createdAt = now,
            updatedAt = now
        )
        val newId = prDao.saveWithItems(copy, items.map { it.copy(id = 0L, packageId = 0L) })
        applyStockFor(newId, copy.recipientName, items.map { it.copy(packageId = newId) }, copy.date)
        return newId
    }

    fun observeDeleted(): Flow<List<PrPackageEntity>> = prDao.observeDeleted()
    suspend fun purgeDeletedBefore(cutoff: Long): List<PrPackageEntity> =
        prDao.findDeletedBefore(cutoff)

    private suspend fun applyStockFor(
        packageId: Long,
        recipient: String,
        items: List<PrPackageItemEntity>,
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
                    type = StockMovementType.PR_PACKAGE.name,
                    delta = -item.quantity,
                    referenceType = "pr_package",
                    referenceId = packageId,
                    note = "PR to $recipient",
                    date = date
                )
            )
        }
    }

    private suspend fun reverseStockFor(packageId: Long) {
        val previous = movementDao.findByReference("pr_package", packageId)
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
        movementDao.deleteByReference("pr_package", packageId)
    }

    fun observeTotalPrCost(): Flow<Long> = prDao.observeTotalPrCost()
    suspend fun recentRecipients(limit: Int = 6): List<String> = prDao.recentRecipients(limit)
    suspend fun recentCampaigns(limit: Int = 6): List<String> = prDao.recentCampaigns(limit)
}
