package com.guddiz.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.guddiz.app.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

data class ExpenseCategoryTotal(
    val category: String,
    val total: Long
)

@Dao
interface ExpenseDao {

    @Query("""
        SELECT * FROM expenses
        WHERE deletedAt = 0
          AND (:query = ''
               OR description LIKE '%' || :query || '%'
               OR IFNULL(vendor,'') LIKE '%' || :query || '%')
          AND (:category = '' OR category = :category)
        ORDER BY expenseDate DESC, id DESC
    """)
    fun observeFiltered(query: String, category: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): ExpenseEntity?

    @Insert suspend fun insert(e: ExpenseEntity): Long
    @Update suspend fun update(e: ExpenseEntity)

    @Query("UPDATE expenses SET deletedAt = :timestamp, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Long, timestamp: Long)

    @Query("UPDATE expenses SET deletedAt = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun restore(id: Long, timestamp: Long)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun hardDelete(id: Long)

    @Query("SELECT * FROM expenses WHERE deletedAt > 0 ORDER BY deletedAt DESC")
    fun observeDeleted(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE deletedAt > 0 AND deletedAt < :cutoff")
    suspend fun findDeletedBefore(cutoff: Long): List<ExpenseEntity>

    @Query("SELECT IFNULL(SUM(amount), 0) FROM expenses WHERE deletedAt = 0")
    fun observeTotalExpenses(): Flow<Long>

    @Query("SELECT IFNULL(SUM(amount), 0) FROM expenses WHERE deletedAt = 0 AND expenseDate >= :fromMillis")
    fun observeExpensesSince(fromMillis: Long): Flow<Long>

    @Query("SELECT IFNULL(SUM(amount), 0) FROM expenses WHERE deletedAt = 0 AND expenseDate BETWEEN :from AND :to")
    fun observeExpensesBetween(from: Long, to: Long): Flow<Long>

    @Query("SELECT IFNULL(SUM(amount), 0) FROM expenses WHERE deletedAt = 0 AND expenseDate BETWEEN :from AND :to")
    suspend fun sumBetween(from: Long, to: Long): Long

    @Query("SELECT COUNT(*) FROM expenses WHERE deletedAt = 0 AND expenseDate BETWEEN :from AND :to")
    suspend fun countBetween(from: Long, to: Long): Int

    @Query("""
        SELECT category AS category, SUM(amount) AS total
        FROM expenses
        WHERE deletedAt = 0 AND expenseDate BETWEEN :from AND :to
        GROUP BY category
        ORDER BY total DESC
    """)
    suspend fun totalsByCategory(from: Long, to: Long): List<ExpenseCategoryTotal>

    @Query("""
        SELECT * FROM expenses
        WHERE deletedAt = 0
          AND (:query = ''
               OR description LIKE '%' || :query || '%'
               OR IFNULL(vendor,'') LIKE '%' || :query || '%')
        ORDER BY expenseDate DESC, id DESC
        LIMIT :limit
    """)
    suspend fun search(query: String, limit: Int): List<ExpenseEntity>

    @Query("SELECT IFNULL(vendor, '') FROM expenses WHERE vendor IS NOT NULL AND vendor != '' GROUP BY vendor ORDER BY MAX(createdAt) DESC LIMIT :limit")
    suspend fun recentVendors(limit: Int): List<String>

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()
}
