package com.guddiz.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pr_package_items",
    foreignKeys = [
        ForeignKey(
            entity = PrPackageEntity::class,
            parentColumns = ["id"],
            childColumns = ["packageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["packageId"]), Index(value = ["productId"])]
)
data class PrPackageItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val packageId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitPrice: Long,
    val lineCost: Long
)
