package com.guddiz.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "gift_items",
    foreignKeys = [
        ForeignKey(
            entity = GiftEntity::class,
            parentColumns = ["id"],
            childColumns = ["giftId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["giftId"]), Index(value = ["productId"])]
)
data class GiftItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val giftId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitPrice: Long,
    val lineCost: Long
)
