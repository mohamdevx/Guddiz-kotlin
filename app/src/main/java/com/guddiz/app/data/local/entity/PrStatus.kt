package com.guddiz.app.data.local.entity

enum class PrStatus(val label: String) {
    PLANNED("Planned"),
    PREPARED("Prepared"),
    SENT("Sent"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled");

    companion object {
        fun fromStorage(v: String?): PrStatus =
            entries.firstOrNull { it.name == v } ?: PLANNED
    }
}
