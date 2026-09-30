package com.guddiz.app.data.local.entity

enum class ExpenseCategory(val label: String) {
    PACKAGING("Packaging"),
    COURIER("Courier"),
    COD_FEE("COD Fee"),
    ADVERTISING("Advertising"),
    PR_MARKETING("PR / Marketing"),
    INFLUENCER("Influencer"),
    PURCHASE("Purchase"),
    PERSONAL_USE("Personal Use"),
    TRAVEL("Travel"),
    OFFICE("Office"),
    UTILITIES("Utilities"),
    MISCELLANEOUS("Miscellaneous"),
    OTHER("Other");

    companion object {
        fun fromStorage(value: String?): ExpenseCategory {
            for (c in entries) {
                if (c.name == value) return c
            }
            return OTHER
        }
    }
}
