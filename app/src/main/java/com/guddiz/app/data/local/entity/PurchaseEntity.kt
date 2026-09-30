package com.guddiz.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "purchases",
    indices = [Index(value = ["productId"]), Index(value = ["purchaseDate"])]
)
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitPrice: Long,
    val supplier: String? = null,
    val purchaseDate: Long,
    val packagingCost: Long = 0L,
    val shippingCost: Long = 0L,
    val otherCost: Long = 0L,
    val totalCost: Long,
    val receiptPath: String? = null,
    val notes: String? = null,
    val deletedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
