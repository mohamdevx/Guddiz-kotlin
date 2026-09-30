package com.guddiz.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.guddiz.app.data.local.entity.GiftEntity
import com.guddiz.app.data.local.entity.GiftItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GiftDao {

    @Query("""
        SELECT * FROM gifts
        WHERE deletedAt = 0
          AND (:query = ''
               OR recipient LIKE '%' || :query || '%'
               OR occasion LIKE '%' || :query || '%')
        ORDER BY date DESC, id DESC
    """)
    fun observeFiltered(query: String): Flow<List<GiftEntity>>

    @Query("SELECT * FROM gifts WHERE id = :id")
    suspend fun getById(id: Long): GiftEntity?

    @Query("SELECT * FROM gift_items WHERE giftId = :giftId ORDER BY id ASC")
    suspend fun getItems(giftId: Long): List<GiftItemEntity>

    @Insert suspend fun insert(g: GiftEntity): Long
    @Update suspend fun update(g: GiftEntity)

    @Query("UPDATE gifts SET deletedAt = :timestamp, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Long, timestamp: Long)

    @Query("UPDATE gifts SET deletedAt = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun restore(id: Long, timestamp: Long)

    @Query("DELETE FROM gifts WHERE id = :id")
    suspend fun hardDelete(id: Long)

    @Query("SELECT * FROM gifts WHERE deletedAt > 0 ORDER BY deletedAt DESC")
    fun observeDeleted(): Flow<List<GiftEntity>>

    @Query("SELECT * FROM gifts WHERE deletedAt > 0 AND deletedAt < :cutoff")
    suspend fun findDeletedBefore(cutoff: Long): List<GiftEntity>

    @Insert suspend fun insertItem(item: GiftItemEntity): Long
    @Query("DELETE FROM gift_items WHERE giftId = :giftId")
    suspend fun deleteItemsFor(giftId: Long)

    @Transaction
    suspend fun saveWithItems(g: GiftEntity, items: List<GiftItemEntity>): Long {
        val id = if (g.id == 0L) insert(g) else { update(g); g.id }
        deleteItemsFor(id)
        items.forEach { insertItem(it.copy(id = 0, giftId = id)) }
        return id
    }

    @Query("SELECT IFNULL(SUM(totalCost), 0) FROM gifts WHERE deletedAt = 0")
    fun observeTotalGiftCost(): Flow<Long>

    @Query("SELECT IFNULL(SUM(totalCost), 0) FROM gifts WHERE deletedAt = 0 AND date BETWEEN :from AND :to")
    suspend fun sumBetween(from: Long, to: Long): Long

    @Query("SELECT COUNT(*) FROM gifts WHERE deletedAt = 0 AND date BETWEEN :from AND :to")
    suspend fun countBetween(from: Long, to: Long): Int

    @Query("""
        SELECT * FROM gifts
        WHERE deletedAt = 0
          AND (:query = ''
               OR recipient LIKE '%' || :query || '%'
               OR occasion LIKE '%' || :query || '%')
        ORDER BY date DESC, id DESC
        LIMIT :limit
    """)
    suspend fun search(query: String, limit: Int): List<GiftEntity>

    @Query("SELECT IFNULL(recipient, '') FROM gifts WHERE recipient != '' GROUP BY recipient ORDER BY MAX(createdAt) DESC LIMIT :limit")
    suspend fun recentRecipients(limit: Int): List<String>

    @Query("DELETE FROM gift_items")
    suspend fun deleteAllGiftItems()

    @Query("DELETE FROM gifts")
    suspend fun deleteAllGifts()
}
