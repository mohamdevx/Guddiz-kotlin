package com.guddiz.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    indices = [Index(value = ["category"]), Index(value = ["expenseDate"])]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val category: String,
    val amount: Long,
    val expenseDate: Long,
    val vendor: String? = null,
    val description: String,
    val relatedProductId: Long? = null,
    val receiptPath: String? = null,
    val notes: String? = null,
    val deletedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
