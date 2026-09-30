package com.guddiz.app.data.repository

import com.guddiz.app.data.local.dao.ExpenseDao
import com.guddiz.app.data.local.dao.FocDao
import com.guddiz.app.data.local.dao.GiftDao
import com.guddiz.app.data.local.dao.PrPackageDao
import com.guddiz.app.data.local.dao.PurchaseDao
import com.guddiz.app.data.local.dao.SaleDao
import com.guddiz.app.data.local.entity.SaleChannel
import com.guddiz.app.data.model.RecordType
import com.guddiz.app.data.model.UnifiedRecord

class RecordsRepository(
    private val saleDao: SaleDao,
    private val purchaseDao: PurchaseDao,
    private val expenseDao: ExpenseDao,
    private val prDao: PrPackageDao,
    private val giftDao: GiftDao,
    private val focDao: FocDao
) {
    suspend fun search(
        query: String,
        types: Set<RecordType>,
        perTypeLimit: Int = 60,
        totalLimit: Int = 150
    ): List<UnifiedRecord> {
        val trimmed = query.trim()
        val out = ArrayList<UnifiedRecord>(totalLimit)

        if (RecordType.SALE in types) {
            saleDao.search(trimmed, perTypeLimit).forEach { s ->
                out += UnifiedRecord(
                    type = RecordType.SALE,
                    id = s.id,
                    title = s.customer?.takeIf { it.isNotBlank() } ?: "Walk-in customer",
                    subtitle = SaleChannel.fromStorage(s.channel).label,
                    amountPaisa = s.totalRevenue,
                    date = s.saleDate
                )
            }
        }

        if (RecordType.PURCHASE in types) {
            purchaseDao.search(trimmed, perTypeLimit).forEach { p ->
                out += UnifiedRecord(
                    type = RecordType.PURCHASE,
                    id = p.id,
                    title = p.productName,
                    subtitle = buildString {
                        append("Qty ").append(p.quantity)
                        val s = p.supplier
                        if (!s.isNullOrBlank()) append("  •  ").append(s)
                    },
                    amountPaisa = p.totalCost,
                    date = p.purchaseDate
                )
            }
        }

        if (RecordType.EXPENSE in types) {
            expenseDao.search(trimmed, perTypeLimit).forEach { e ->
                out += UnifiedRecord(
                    type = RecordType.EXPENSE,
                    id = e.id,
                    title = e.description,
                    subtitle = buildString {
                        append(categoryLabel(e.category))
                        val v = e.vendor
                        if (!v.isNullOrBlank()) append("  •  ").append(v)
                    },
                    amountPaisa = e.amount,
                    date = e.expenseDate
                )
            }
        }

        if (RecordType.PR in types) {
            prDao.search(trimmed, perTypeLimit).forEach { p ->
                out += UnifiedRecord(
                    type = RecordType.PR,
                    id = p.id,
                    title = p.recipientName,
                    subtitle = buildString {
                        append(prettyEnum(p.status))
                        val c = p.campaignName
                        if (!c.isNullOrBlank()) append("  •  ").append(c)
                    },
                    amountPaisa = p.totalCost,
                    date = p.date
                )
            }
        }

        if (RecordType.GIFT in types) {
            giftDao.search(trimmed, perTypeLimit).forEach { g ->
                out += UnifiedRecord(
                    type = RecordType.GIFT,
                    id = g.id,
                    title = g.recipient,
                    subtitle = g.occasion,
                    amountPaisa = g.totalCost,
                    date = g.date
                )
            }
        }

        if (RecordType.FOC in types) {
            focDao.search(trimmed, perTypeLimit).forEach { f ->
                out += UnifiedRecord(
                    type = RecordType.FOC,
                    id = f.id,
                    title = f.productName,
                    subtitle = buildString {
                        append("Qty ").append(f.quantity).append(" to ").append(f.recipient)
                        val c = f.campaign
                        if (!c.isNullOrBlank()) append("  •  ").append(c)
                    },
                    amountPaisa = f.lineCost,
                    date = f.date
                )
            }
        }

        return out.sortedByDescending { it.date }.take(totalLimit)
    }

    private fun categoryLabel(stored: String): String = when (stored) {
        "PACKAGING" -> "Packaging"
        "COURIER" -> "Courier"
        "COD_FEE" -> "COD Fee"
        "ADVERTISING" -> "Advertising"
        "PR_MARKETING" -> "PR / Marketing"
        "INFLUENCER" -> "Influencer"
        "PURCHASE" -> "Purchase"
        "PERSONAL_USE" -> "Personal Use"
        "TRAVEL" -> "Travel"
        "OFFICE" -> "Office"
        "UTILITIES" -> "Utilities"
        "MISCELLANEOUS" -> "Miscellaneous"
        else -> "Other"
    }

    private fun prettyEnum(stored: String): String = stored
        .lowercase()
        .split('_')
        .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
}
