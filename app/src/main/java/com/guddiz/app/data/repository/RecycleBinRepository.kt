package com.guddiz.app.data.repository

import com.guddiz.app.data.model.DeletedItem
import com.guddiz.app.data.model.DeletedItemType
import kotlinx.coroutines.flow.first

class RecycleBinRepository(
    private val productRepository: ProductRepository,
    private val saleRepository: SaleRepository,
    private val purchaseRepository: PurchaseRepository,
    private val expenseRepository: ExpenseRepository,
    private val prRepository: PrPackageRepository,
    private val giftRepository: GiftRepository,
    private val focRepository: FocRepository
) {

    suspend fun loadAll(): List<DeletedItem> {
        val items = mutableListOf<DeletedItem>()

        productRepository.observeDeleted().first().forEach { p ->
            items += DeletedItem(
                type = DeletedItemType.PRODUCT,
                id = p.id,
                title = p.name,
                subtitle = "Product",
                deletedAt = p.deletedAt
            )
        }

        saleRepository.observeDeleted().first().forEach { s ->
            items += DeletedItem(
                type = DeletedItemType.SALE,
                id = s.id,
                title = s.customer?.takeIf { it.isNotBlank() } ?: "Walk-in sale",
                subtitle = "Sale",
                deletedAt = s.deletedAt
            )
        }

        purchaseRepository.observeDeleted().first().forEach { p ->
            items += DeletedItem(
                type = DeletedItemType.PURCHASE,
                id = p.id,
                title = p.productName,
                subtitle = "Qty " + p.quantity,
                deletedAt = p.deletedAt
            )
        }

        expenseRepository.observeDeleted().first().forEach { e ->
            items += DeletedItem(
                type = DeletedItemType.EXPENSE,
                id = e.id,
                title = e.description,
                subtitle = "Expense",
                deletedAt = e.deletedAt
            )
        }

        prRepository.observeDeleted().first().forEach { p ->
            items += DeletedItem(
                type = DeletedItemType.PR,
                id = p.id,
                title = p.recipientName,
                subtitle = "PR package",
                deletedAt = p.deletedAt
            )
        }

        giftRepository.observeDeleted().first().forEach { g ->
            items += DeletedItem(
                type = DeletedItemType.GIFT,
                id = g.id,
                title = g.recipient,
                subtitle = g.occasion,
                deletedAt = g.deletedAt
            )
        }

        focRepository.observeDeleted().first().forEach { f ->
            items += DeletedItem(
                type = DeletedItemType.FOC,
                id = f.id,
                title = f.productName,
                subtitle = "FOC to " + f.recipient,
                deletedAt = f.deletedAt
            )
        }

        return items.sortedByDescending { it.deletedAt }
    }

    suspend fun restore(item: DeletedItem) {
        when (item.type) {
            DeletedItemType.PRODUCT -> productRepository.restore(item.id)
            DeletedItemType.SALE ->
                saleRepository.getSale(item.id)?.let { saleRepository.restore(it) }
            DeletedItemType.PURCHASE ->
                purchaseRepository.getPurchase(item.id)?.let { purchaseRepository.restore(it) }
            DeletedItemType.EXPENSE -> expenseRepository.restore(item.id)
            DeletedItemType.PR ->
                prRepository.getPackage(item.id)?.let { prRepository.restore(it) }
            DeletedItemType.GIFT ->
                giftRepository.getGift(item.id)?.let { giftRepository.restore(it) }
            DeletedItemType.FOC ->
                focRepository.getFoc(item.id)?.let { focRepository.restore(it) }
        }
    }

    suspend fun hardDelete(item: DeletedItem) {
        when (item.type) {
            DeletedItemType.PRODUCT -> productRepository.hardDelete(item.id)
            DeletedItemType.SALE -> saleRepository.hardDelete(item.id)
            DeletedItemType.PURCHASE -> purchaseRepository.hardDelete(item.id)
            DeletedItemType.EXPENSE -> expenseRepository.hardDelete(item.id)
            DeletedItemType.PR -> prRepository.hardDelete(item.id)
            DeletedItemType.GIFT -> giftRepository.hardDelete(item.id)
            DeletedItemType.FOC -> focRepository.hardDelete(item.id)
        }
    }

    suspend fun purgeOld(days: Int = 30) {
        val cutoff = System.currentTimeMillis() - days * 24L * 60L * 60L * 1000L
        productRepository.purgeDeletedBefore(cutoff).forEach { productRepository.hardDelete(it.id) }
        saleRepository.purgeDeletedBefore(cutoff).forEach { saleRepository.hardDelete(it.id) }
        purchaseRepository.purgeDeletedBefore(cutoff).forEach { purchaseRepository.hardDelete(it.id) }
        expenseRepository.purgeDeletedBefore(cutoff).forEach { expenseRepository.hardDelete(it.id) }
        prRepository.purgeDeletedBefore(cutoff).forEach { prRepository.hardDelete(it.id) }
        giftRepository.purgeDeletedBefore(cutoff).forEach { giftRepository.hardDelete(it.id) }
        focRepository.purgeDeletedBefore(cutoff).forEach { focRepository.hardDelete(it.id) }
    }
}
