package com.guddiz.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.guddiz.app.data.local.entity.PrPackageEntity
import com.guddiz.app.data.local.entity.PrPackageItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrPackageDao {

    @Query("""
        SELECT * FROM pr_packages
        WHERE deletedAt = 0
          AND (:query = ''
               OR recipientName LIKE '%' || :query || '%'
               OR IFNULL(campaignName,'') LIKE '%' || :query || '%')
        ORDER BY date DESC, id DESC
    """)
    fun observeFiltered(query: String): Flow<List<PrPackageEntity>>

    @Query("SELECT * FROM pr_packages WHERE id = :id")
    suspend fun getById(id: Long): PrPackageEntity?

    @Query("SELECT * FROM pr_package_items WHERE packageId = :packageId ORDER BY id ASC")
    suspend fun getItems(packageId: Long): List<PrPackageItemEntity>

    @Query("SELECT * FROM pr_package_items WHERE packageId = :packageId ORDER BY id ASC")
    fun observeItems(packageId: Long): Flow<List<PrPackageItemEntity>>

    @Insert suspend fun insert(pkg: PrPackageEntity): Long
    @Update suspend fun update(pkg: PrPackageEntity)

    @Query("UPDATE pr_packages SET deletedAt = :timestamp, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Long, timestamp: Long)

    @Query("UPDATE pr_packages SET deletedAt = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun restore(id: Long, timestamp: Long)

    @Query("DELETE FROM pr_packages WHERE id = :id")
    suspend fun hardDelete(id: Long)

    @Query("SELECT * FROM pr_packages WHERE deletedAt > 0 ORDER BY deletedAt DESC")
    fun observeDeleted(): Flow<List<PrPackageEntity>>

    @Query("SELECT * FROM pr_packages WHERE deletedAt > 0 AND deletedAt < :cutoff")
    suspend fun findDeletedBefore(cutoff: Long): List<PrPackageEntity>

    @Insert suspend fun insertItem(item: PrPackageItemEntity): Long
    @Query("DELETE FROM pr_package_items WHERE packageId = :packageId")
    suspend fun deleteItemsFor(packageId: Long)

    @Transaction
    suspend fun saveWithItems(pkg: PrPackageEntity, items: List<PrPackageItemEntity>): Long {
        val id = if (pkg.id == 0L) insert(pkg) else { update(pkg); pkg.id }
        deleteItemsFor(id)
        items.forEach { insertItem(it.copy(id = 0, packageId = id)) }
        return id
    }

    @Query("SELECT IFNULL(SUM(totalCost), 0) FROM pr_packages WHERE deletedAt = 0")
    fun observeTotalPrCost(): Flow<Long>

    @Query("SELECT IFNULL(SUM(totalCost), 0) FROM pr_packages WHERE deletedAt = 0 AND date BETWEEN :from AND :to")
    suspend fun sumBetween(from: Long, to: Long): Long

    @Query("SELECT COUNT(*) FROM pr_packages WHERE deletedAt = 0 AND date BETWEEN :from AND :to")
    suspend fun countBetween(from: Long, to: Long): Int

    @Query("""
        SELECT * FROM pr_packages
        WHERE deletedAt = 0
          AND (:query = ''
               OR recipientName LIKE '%' || :query || '%'
               OR IFNULL(campaignName,'') LIKE '%' || :query || '%')
        ORDER BY date DESC, id DESC
        LIMIT :limit
    """)
    suspend fun search(query: String, limit: Int): List<PrPackageEntity>

    @Query("SELECT IFNULL(recipientName, '') FROM pr_packages WHERE recipientName != '' GROUP BY recipientName ORDER BY MAX(createdAt) DESC LIMIT :limit")
    suspend fun recentRecipients(limit: Int): List<String>

    @Query("SELECT IFNULL(campaignName, '') FROM pr_packages WHERE campaignName IS NOT NULL AND campaignName != '' GROUP BY campaignName ORDER BY MAX(createdAt) DESC LIMIT :limit")
    suspend fun recentCampaigns(limit: Int): List<String>

    @Query("DELETE FROM pr_package_items")
    suspend fun deleteAllPrItems()

    @Query("DELETE FROM pr_packages")
    suspend fun deleteAllPrPackages()
}
