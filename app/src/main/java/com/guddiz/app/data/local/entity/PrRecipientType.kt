package com.guddiz.app.data.local.entity

enum class PrRecipientType(val label: String) {
    INFLUENCER("Influencer"),
    CONTENT_CREATOR("Content Creator"),
    BLOGGER("Blogger"),
    MEDIA("Media"),
    BRAND_COLLAB("Brand Collaboration"),
    CLIENT("Client"),
    OTHER("Other");

    companion object {
        fun fromStorage(v: String?): PrRecipientType =
            entries.firstOrNull { it.name == v } ?: OTHER
    }
}
