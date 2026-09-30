package com.guddiz.app.data.model

enum class DeletedItemType(val label: String) {
    PRODUCT("Product"),
    SALE("Sale"),
    PURCHASE("Purchase"),
    EXPENSE("Expense"),
    PR("PR Package"),
    GIFT("Gift"),
    FOC("FOC")
}

data class DeletedItem(
    val type: DeletedItemType,
    val id: Long,
    val title: String,
    val subtitle: String,
    val deletedAt: Long
)
