package com.guddiz.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.guddiz.app.data.local.entity.SaleEntity
import com.guddiz.app.data.local.entity.SaleItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {

    @Query("""
        SELECT * FROM sales
        WHERE deletedAt = 0
          AND (:query = ''
               OR IFNULL(customer,'') LIKE '%' || :query || '%'
               OR IFNULL(notes,'') LIKE '%' || :query || '%')
          AND (:channel = '' OR channel = :channel)
        ORDER BY saleDate DESC, id DESC
    """)
    fun observeFiltered(query: String, channel: String): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getById(id: Long): SaleEntity?

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId ORDER BY id ASC")
    suspend fun getItems(saleId: Long): List<SaleItemEntity>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId ORDER BY id ASC")
    fun observeItems(saleId: Long): Flow<List<SaleItemEntity>>

    @Insert suspend fun insert(s: SaleEntity): Long
    @Update suspend fun update(s: SaleEntity)

    @Query("UPDATE sales SET deletedAt = :timestamp, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Long, timestamp: Long)

    @Query("UPDATE sales SET deletedAt = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun restore(id: Long, timestamp: Long)

    @Query("DELETE FROM sales WHERE id = :id")
    suspend fun hardDelete(id: Long)

    @Query("SELECT * FROM sales WHERE deletedAt > 0 ORDER BY deletedAt DESC")
    fun observeDeleted(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE deletedAt > 0 AND deletedAt < :cutoff")
    suspend fun findDeletedBefore(cutoff: Long): List<SaleEntity>

    @Insert suspend fun insertItem(item: SaleItemEntity): Long
    @Query("DELETE FROM sale_items WHERE saleId = :saleId")
    suspend fun deleteItemsFor(saleId: Long)

    @Transaction
    suspend fun saveWithItems(s: SaleEntity, items: List<SaleItemEntity>): Long {
        val id = if (s.id == 0L) insert(s) else { update(s); s.id }
        deleteItemsFor(id)
        items.forEach { insertItem(it.copy(id = 0, saleId = id)) }
        return id
    }

    // ---- Dashboard + Reports aggregates ---------------------------------

    @Query("SELECT IFNULL(SUM(totalRevenue), 0) FROM sales WHERE deletedAt = 0")
    fun observeTotalRevenue(): Flow<Long>

    @Query("SELECT IFNULL(SUM(totalCost), 0) FROM sales WHERE deletedAt = 0")
    fun observeTotalCostOfGoods(): Flow<Long>

    @Query("SELECT IFNULL(SUM(totalRevenue), 0) FROM sales WHERE deletedAt = 0 AND saleDate >= :fromMillis")
    fun observeRevenueSince(fromMillis: Long): Flow<Long>

    @Query("SELECT IFNULL(SUM(totalRevenue), 0) FROM sales WHERE deletedAt = 0 AND saleDate BETWEEN :from AND :to")
    fun observeRevenueBetween(from: Long, to: Long): Flow<Long>

    @Query("SELECT IFNULL(SUM(totalRevenue), 0) FROM sales WHERE deletedAt = 0 AND saleDate BETWEEN :from AND :to")
    suspend fun sumRevenueBetween(from: Long, to: Long): Long

    @Query("SELECT IFNULL(SUM(totalCost), 0) FROM sales WHERE deletedAt = 0 AND saleDate BETWEEN :from AND :to")
    suspend fun sumCostBetween(from: Long, to: Long): Long

    @Query("SELECT COUNT(*) FROM sales WHERE deletedAt = 0 AND saleDate BETWEEN :from AND :to")
    suspend fun countBetween(from: Long, to: Long): Int

    @Query("""
        SELECT * FROM sales
        WHERE deletedAt = 0
          AND (:query = ''
               OR IFNULL(customer,'') LIKE '%' || :query || '%'
               OR IFNULL(notes,'') LIKE '%' || :query || '%')
        ORDER BY saleDate DESC, id DESC
        LIMIT :limit
    """)
    suspend fun search(query: String, limit: Int): List<SaleEntity>

    /** Top-selling products by quantity, for a light "best sellers" view later. */
    @Query("""
        SELECT productName, SUM(quantity) AS qty, SUM(lineRevenue) AS revenue
        FROM sale_items
        WHERE saleId IN (SELECT id FROM sales WHERE deletedAt = 0 AND saleDate BETWEEN :from AND :to)
        GROUP BY productId, productName
        ORDER BY qty DESC
        LIMIT :limit
    """)
    suspend fun topProductsBetween(from: Long, to: Long, limit: Int): List<TopProduct>

    @Query("SELECT IFNULL(customer, '') FROM sales WHERE customer IS NOT NULL AND customer != '' GROUP BY customer ORDER BY MAX(createdAt) DESC LIMIT :limit")
    suspend fun recentCustomers(limit: Int): List<String>

    @Query("DELETE FROM sale_items")
    suspend fun deleteAllSaleItems()

    @Query("DELETE FROM sales")
    suspend fun deleteAllSales()

    @Query("SELECT saleDate, totalRevenue FROM sales WHERE deletedAt = 0 AND saleDate >= :fromMillis ORDER BY saleDate ASC")
    fun observeSalesSince(fromMillis: Long): kotlinx.coroutines.flow.Flow<List<SaleDateRevenue>>

    @Query("SELECT saleDate, totalRevenue FROM sales WHERE deletedAt = 0 AND saleDate >= :fromMillis ORDER BY saleDate ASC")
    suspend fun getSalesSince(fromMillis: Long): List<SaleDateRevenue>
}

data class SaleDateRevenue(
    val saleDate: Long,
    val totalRevenue: Long
)

data class TopProduct(
    val productName: String,
    val qty: Int,
    val revenue: Long
)
