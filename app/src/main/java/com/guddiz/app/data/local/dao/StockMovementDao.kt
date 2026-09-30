package com.guddiz.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.guddiz.app.data.local.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {

    @Insert suspend fun insert(m: StockMovementEntity): Long

    @Query("""
        SELECT * FROM stock_movements
        WHERE productId = :productId
        ORDER BY date DESC, id DESC
    """)
    fun observeForProduct(productId: Long): Flow<List<StockMovementEntity>>

    @Query("""
        SELECT * FROM stock_movements
        WHERE referenceType = :refType AND referenceId = :refId
    """)
    suspend fun findByReference(refType: String, refId: Long): List<StockMovementEntity>

    @Query("DELETE FROM stock_movements WHERE referenceType = :refType AND referenceId = :refId")
    suspend fun deleteByReference(refType: String, refId: Long)

    @Query("DELETE FROM stock_movements")
    suspend fun deleteAllMovements()
}
