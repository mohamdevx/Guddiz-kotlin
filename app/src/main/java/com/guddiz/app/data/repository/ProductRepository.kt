package com.guddiz.app.data.repository

import com.guddiz.app.data.local.dao.ProductDao
import com.guddiz.app.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

class ProductRepository(private val productDao: ProductDao) {

    fun observeProducts(query: String = "", category: String = ""): Flow<List<ProductEntity>> =
        productDao.observeFiltered(query.trim(), category)

    fun observeProduct(id: Long): Flow<ProductEntity?> = productDao.observeById(id)

    suspend fun getProduct(id: Long): ProductEntity? = productDao.getById(id)

    suspend fun isSkuTaken(sku: String, excludeId: Long): Boolean {
        if (sku.isBlank()) return false
        return productDao.findBySku(sku.trim(), excludeId) != null
    }

    suspend fun save(product: ProductEntity): Long {
        val now = System.currentTimeMillis()
        return if (product.id == 0L) {
            productDao.insert(product.copy(createdAt = now, updatedAt = now))
        } else {
            productDao.update(product.copy(updatedAt = now))
            product.id
        }
    }

    suspend fun softDelete(id: Long) {
        productDao.softDelete(id, System.currentTimeMillis())
    }

    suspend fun restore(id: Long) {
        productDao.restore(id, System.currentTimeMillis())
    }

    suspend fun hardDelete(id: Long) = productDao.hardDelete(id)

    suspend fun duplicate(id: Long): Long? {
        val original = productDao.getById(id) ?: return null
        val now = System.currentTimeMillis()
        val copy = original.copy(
            id = 0L,
            name = "${original.name} (copy)",
            sku = null,
            productCode = null,
            createdAt = now,
            updatedAt = now
        )
        return productDao.insert(copy)
    }

    fun observeDeleted(): Flow<List<ProductEntity>> = productDao.observeDeleted()
    suspend fun purgeDeletedBefore(cutoff: Long): List<ProductEntity> =
        productDao.findDeletedBefore(cutoff)

    fun observeProductCount(): Flow<Int> = productDao.observeProductCount()
    fun observeTotalQuantity(): Flow<Int> = productDao.observeTotalQuantity()
    fun observeTotalStockValue(): Flow<Long> = productDao.observeTotalStockValue()
    suspend fun recentSuppliers(limit: Int = 6): List<String> = productDao.recentSuppliers(limit)
}
