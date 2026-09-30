package com.guddiz.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.guddiz.app.data.local.entity.FocEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocDao {

    @Query("""
        SELECT * FROM foc_records
        WHERE deletedAt = 0
          AND (:query = ''
               OR productName LIKE '%' || :query || '%'
               OR recipient LIKE '%' || :query || '%'
               OR IFNULL(campaign,'') LIKE '%' || :query || '%')
        ORDER BY date DESC, id DESC
    """)
    fun observeFiltered(query: String): Flow<List<FocEntity>>

    @Query("SELECT * FROM foc_records WHERE id = :id")
    suspend fun getById(id: Long): FocEntity?

    @Insert suspend fun insert(f: FocEntity): Long
    @Update suspend fun update(f: FocEntity)

    @Query("UPDATE foc_records SET deletedAt = :timestamp, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Long, timestamp: Long)

    @Query("UPDATE foc_records SET deletedAt = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun restore(id: Long, timestamp: Long)

    @Query("DELETE FROM foc_records WHERE id = :id")
    suspend fun hardDelete(id: Long)

    @Query("SELECT * FROM foc_records WHERE deletedAt > 0 ORDER BY deletedAt DESC")
    fun observeDeleted(): Flow<List<FocEntity>>

    @Query("SELECT * FROM foc_records WHERE deletedAt > 0 AND deletedAt < :cutoff")
    suspend fun findDeletedBefore(cutoff: Long): List<FocEntity>

    @Query("SELECT IFNULL(SUM(lineCost), 0) FROM foc_records WHERE deletedAt = 0")
    fun observeTotalFocCost(): Flow<Long>

    @Query("SELECT IFNULL(SUM(lineCost), 0) FROM foc_records WHERE deletedAt = 0 AND date BETWEEN :from AND :to")
    suspend fun sumBetween(from: Long, to: Long): Long

    @Query("SELECT COUNT(*) FROM foc_records WHERE deletedAt = 0 AND date BETWEEN :from AND :to")
    suspend fun countBetween(from: Long, to: Long): Int

    @Query("""
        SELECT * FROM foc_records
        WHERE deletedAt = 0
          AND (:query = ''
               OR productName LIKE '%' || :query || '%'
               OR recipient LIKE '%' || :query || '%'
               OR IFNULL(campaign,'') LIKE '%' || :query || '%')
        ORDER BY date DESC, id DESC
        LIMIT :limit
    """)
    suspend fun search(query: String, limit: Int): List<FocEntity>

    @Query("SELECT IFNULL(recipient, '') FROM foc_records WHERE recipient != '' GROUP BY recipient ORDER BY MAX(createdAt) DESC LIMIT :limit")
    suspend fun recentRecipients(limit: Int): List<String>

    @Query("DELETE FROM foc_records")
    suspend fun deleteAllFoc()
}
