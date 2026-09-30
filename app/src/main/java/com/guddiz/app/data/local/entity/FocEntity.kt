package com.guddiz.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "foc_records",
    indices = [Index(value = ["date"]), Index(value = ["productId"])]
)
data class FocEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitPrice: Long,
    val lineCost: Long,
    val recipient: String,
    val campaign: String? = null,
    val date: Long,
    val receiptPath: String? = null,
    val notes: String? = null,
    val deletedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
