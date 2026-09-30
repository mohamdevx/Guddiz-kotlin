package com.guddiz.app.data.local.entity

enum class SaleChannel(val label: String) {
    WALK_IN("Walk-in"),
    SHOPIFY("Shopify"),
    WHATSAPP("WhatsApp"),
    INSTAGRAM("Instagram"),
    OTHER("Other");

    companion object {
        fun fromStorage(v: String?): SaleChannel =
            entries.firstOrNull { it.name == v } ?: WALK_IN
    }
}
