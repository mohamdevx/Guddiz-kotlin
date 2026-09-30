package com.guddiz.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sales",
    indices = [Index(value = ["saleDate"]), Index(value = ["channel"])]
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val saleDate: Long,
    val channel: String,
    val customer: String? = null,
    val totalRevenue: Long,
    val totalCost: Long,
    /** Human-readable summary like "Vitamin C Serum ×2, Rose Face Wash ×1". */
    val itemSummary: String = "",
    /** Total number of physical units sold. */
    val itemCount: Int = 0,
    val notes: String? = null,
    val deletedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
