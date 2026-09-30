package com.guddiz.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["name"]),
        Index(value = ["sku"]),
        Index(value = ["category"])
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val sku: String? = null,
    val productCode: String? = null,
    val category: String,
    val brand: String? = null,
    val supplier: String? = null,
    val purchasePrice: Long = 0L,
    val prs: Long = 0L,
    val sellingPrice: Long = 0L,
    val currentQuantity: Int = 0,
    val minStockLevel: Int = 0,
    val batchNumber: String? = null,
    val expiryDate: Long? = null,
    val photoPath: String? = null,
    val notes: String? = null,
    val deletedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
