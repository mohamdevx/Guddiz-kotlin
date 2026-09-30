package com.guddiz.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.guddiz.app.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("""
        SELECT * FROM products
        WHERE deletedAt = 0
          AND (:query = ''
               OR name LIKE '%' || :query || '%'
               OR IFNULL(sku,'') LIKE '%' || :query || '%'
               OR IFNULL(productCode,'') LIKE '%' || :query || '%'
               OR IFNULL(brand,'') LIKE '%' || :query || '%'
               OR IFNULL(supplier,'') LIKE '%' || :query || '%')
          AND (:category = '' OR category = :category)
        ORDER BY name COLLATE NOCASE ASC
    """)
    fun observeFiltered(query: String, category: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    fun observeById(id: Long): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE sku = :sku AND sku IS NOT NULL AND deletedAt = 0 AND id != :excludeId LIMIT 1")
    suspend fun findBySku(sku: String, excludeId: Long): ProductEntity?

    @Insert suspend fun insert(product: ProductEntity): Long
    @Update suspend fun update(product: ProductEntity)

    @Query("UPDATE products SET deletedAt = :timestamp, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Long, timestamp: Long)

    @Query("UPDATE products SET deletedAt = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun restore(id: Long, timestamp: Long)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun hardDelete(id: Long)

    @Query("SELECT * FROM products WHERE deletedAt > 0 ORDER BY deletedAt DESC")
    fun observeDeleted(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE deletedAt > 0 AND deletedAt < :cutoff")
    suspend fun findDeletedBefore(cutoff: Long): List<ProductEntity>

    @Query("SELECT COUNT(*) FROM products WHERE deletedAt = 0")
    fun observeProductCount(): Flow<Int>

    @Query("SELECT IFNULL(SUM(currentQuantity), 0) FROM products WHERE deletedAt = 0")
    fun observeTotalQuantity(): Flow<Int>

    @Query("SELECT IFNULL(SUM(CAST(currentQuantity AS INTEGER) * purchasePrice), 0) FROM products WHERE deletedAt = 0")
    fun observeTotalStockValue(): Flow<Long>

    @Query("SELECT IFNULL(SUM(currentQuantity), 0) FROM products WHERE deletedAt = 0")
    suspend fun observeTotalQuantityOnce(): Int

    @Query("SELECT IFNULL(SUM(CAST(currentQuantity AS INTEGER) * purchasePrice), 0) FROM products WHERE deletedAt = 0")
    suspend fun observeTotalStockValueOnce(): Long

    @Query("SELECT COUNT(*) FROM products WHERE deletedAt = 0 AND minStockLevel > 0 AND currentQuantity <= minStockLevel")
    fun observeLowStockCount(): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM products
        WHERE deletedAt = 0
          AND expiryDate IS NOT NULL
          AND expiryDate > 0
          AND expiryDate > :nowMillis
          AND expiryDate < :cutoffMillis
    """)
    fun observeExpiringSoonCount(nowMillis: Long, cutoffMillis: Long): Flow<Int>

    @Query("SELECT IFNULL(supplier, '') FROM purchases WHERE supplier IS NOT NULL AND supplier != '' GROUP BY supplier ORDER BY MAX(createdAt) DESC LIMIT :limit")
    suspend fun recentSuppliers(limit: Int): List<String>

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()
}
