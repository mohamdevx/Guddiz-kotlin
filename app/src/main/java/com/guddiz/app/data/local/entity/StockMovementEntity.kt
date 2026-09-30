package com.guddiz.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Every change to a product's quantity is recorded here.
 * Rows are NEVER deleted — they are the audit trail.
 */
@Entity(
    tableName = "stock_movements",
    indices = [
        Index(value = ["productId"]),
        Index(value = ["date"]),
        Index(value = ["referenceType", "referenceId"])
    ]
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val productId: Long,
    val productName: String,
    /** Enum name (PURCHASE, PR_PACKAGE, ...). */
    val type: String,
    /** Signed: +50 for purchase, -3 for FOC, etc. */
    val delta: Int,
    /** e.g. "purchase", "pr_package", "gift", "foc" — lets us reverse on delete. */
    val referenceType: String? = null,
    val referenceId: Long? = null,
    val note: String? = null,
    val date: Long,
    val createdAt: Long = System.currentTimeMillis()
)
