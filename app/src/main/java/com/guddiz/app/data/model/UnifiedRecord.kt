package com.guddiz.app.data.model

enum class RecordType(val label: String, val pluralLabel: String) {
    SALE("Sale", "Sales"),
    PURCHASE("Purchase", "Purchases"),
    EXPENSE("Expense", "Expenses"),
    PR("PR Package", "PR Packages"),
    GIFT("Gift", "Gifts"),
    FOC("FOC", "FOC");

    companion object {
        val all: Set<RecordType> = entries.toSet()
    }
}

data class UnifiedRecord(
    val type: RecordType,
    val id: Long,
    val title: String,
    val subtitle: String,
    val amountPaisa: Long,
    val date: Long
)
