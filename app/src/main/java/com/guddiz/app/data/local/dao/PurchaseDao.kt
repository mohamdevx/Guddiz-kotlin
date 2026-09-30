package com.guddiz.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.guddiz.app.data.local.entity.PurchaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {

    @Query("""
        SELECT * FROM purchases
        WHERE deletedAt = 0
          AND (:query = ''
               OR productName LIKE '%' || :query || '%'
               OR IFNULL(supplier,'') LIKE '%' || :query || '%')
        ORDER BY purchaseDate DESC, id DESC
    """)
    fun observeFiltered(query: String): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases WHERE id = :id")
    suspend fun getById(id: Long): PurchaseEntity?

    @Insert suspend fun insert(p: PurchaseEntity): Long
    @Update suspend fun update(p: PurchaseEntity)

    @Query("UPDATE purchases SET deletedAt = :timestamp, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Long, timestamp: Long)

    @Query("UPDATE purchases SET deletedAt = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun restore(id: Long, timestamp: Long)

    @Query("DELETE FROM purchases WHERE id = :id")
    suspend fun hardDelete(id: Long)

    @Query("SELECT * FROM purchases WHERE deletedAt > 0 ORDER BY deletedAt DESC")
    fun observeDeleted(): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases WHERE deletedAt > 0 AND deletedAt < :cutoff")
    suspend fun findDeletedBefore(cutoff: Long): List<PurchaseEntity>

    @Query("SELECT IFNULL(SUM(totalCost), 0) FROM purchases WHERE deletedAt = 0")
    fun observeTotalPurchaseCost(): Flow<Long>

    @Query("SELECT IFNULL(SUM(packagingCost), 0) FROM purchases WHERE deletedAt = 0")
    fun observeTotalPackaging(): Flow<Long>

    @Query("SELECT IFNULL(SUM(shippingCost), 0) FROM purchases WHERE deletedAt = 0")
    fun observeTotalShipping(): Flow<Long>

    @Query("SELECT IFNULL(SUM(totalCost), 0) FROM purchases WHERE deletedAt = 0 AND purchaseDate BETWEEN :from AND :to")
    suspend fun sumBetween(from: Long, to: Long): Long

    @Query("SELECT IFNULL(SUM(packagingCost), 0) FROM purchases WHERE deletedAt = 0 AND purchaseDate BETWEEN :from AND :to")
    suspend fun sumPackagingBetween(from: Long, to: Long): Long

    @Query("SELECT IFNULL(SUM(shippingCost), 0) FROM purchases WHERE deletedAt = 0 AND purchaseDate BETWEEN :from AND :to")
    suspend fun sumShippingBetween(from: Long, to: Long): Long

    @Query("SELECT COUNT(*) FROM purchases WHERE deletedAt = 0 AND purchaseDate BETWEEN :from AND :to")
    suspend fun countBetween(from: Long, to: Long): Int

    @Query("DELETE FROM purchases")
    suspend fun deleteAllPurchases()

    @Query("""
        SELECT * FROM purchases
        WHERE deletedAt = 0
          AND (:query = ''
               OR productName LIKE '%' || :query || '%'
               OR IFNULL(supplier,'') LIKE '%' || :query || '%')
        ORDER BY purchaseDate DESC, id DESC
        LIMIT :limit
    """)
    suspend fun search(query: String, limit: Int): List<PurchaseEntity>
}
