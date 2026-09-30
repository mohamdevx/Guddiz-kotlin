package com.guddiz.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "gifts",
    indices = [Index(value = ["date"])]
)
data class GiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val recipient: String,
    val occasion: String,
    val date: Long,
    val productCost: Long,
    val packagingCost: Long = 0L,
    val courierCost: Long = 0L,
    val otherCost: Long = 0L,
    val totalCost: Long,
    val receiptPath: String? = null,
    val notes: String? = null,
    val deletedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
