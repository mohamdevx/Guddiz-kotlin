package com.guddiz.app.data.local.entity

enum class ProductCategory(val label: String) {
    SKINCARE("Skincare"),
    HAIRCARE("Haircare"),
    MAKEUP("Makeup"),
    BODY_CARE("Body Care"),
    FRAGRANCE("Fragrance"),
    HERBAL("Herbal"),
    OTHER("Other");

    companion object {
        fun fromStorage(value: String?): ProductCategory =
            entries.firstOrNull { it.name == value } ?: OTHER
    }
}
