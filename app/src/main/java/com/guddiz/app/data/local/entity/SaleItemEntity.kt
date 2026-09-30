package com.guddiz.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["saleId"]), Index(value = ["productId"])]
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    /** Actual sold price per unit — could differ from the listed price. */
    val unitPrice: Long,
    /** Product's purchase price at the time of sale. Frozen for profit reports. */
    val unitCost: Long,
    val lineRevenue: Long,
    val lineCost: Long
)
