package com.guddiz.app.data.local.entity

enum class StockMovementType(val label: String, val sign: Int) {
    PURCHASE("Purchase", 1),
    SALE("Sale", -1),
    PR_PACKAGE("PR Package", -1),
    GIFT("Gift", -1),
    FOC("Free of Cost", -1),
    SAMPLE("Sample", -1),
    DAMAGED("Damaged", -1),
    PERSONAL_USE("Personal Use", -1),
    ADJUSTMENT("Adjustment", 0);

    companion object {
        fun fromStorage(v: String?): StockMovementType {
            for (t in entries) {
                if (t.name == v) return t
            }
            return ADJUSTMENT
        }
    }
}
