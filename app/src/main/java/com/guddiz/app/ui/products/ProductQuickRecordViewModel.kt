package com.guddiz.app.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guddiz.app.core.money.Money
import com.guddiz.app.core.util.DateUtils
import com.guddiz.app.data.local.entity.ProductEntity
import com.guddiz.app.data.local.entity.PurchaseEntity
import com.guddiz.app.data.local.entity.SaleChannel
import com.guddiz.app.data.local.entity.SaleEntity
import com.guddiz.app.data.local.entity.SaleItemEntity
import com.guddiz.app.data.repository.PurchaseRepository
import com.guddiz.app.data.repository.SaleRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

enum class QuickRecordType(val title: String) {
    SALE("Record Sale"),
    PURCHASE("Record Purchase")
}

sealed interface QuickRecordEvent {
    data class Message(val text: String) : QuickRecordEvent
    data object Saved : QuickRecordEvent
}

class ProductQuickRecordViewModel(
    private val saleRepository: SaleRepository,
    private val purchaseRepository: PurchaseRepository
) : ViewModel() {

    private val _events = Channel<QuickRecordEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun recordSale(
        product: ProductEntity,
        quantity: Int,
        unitPrice: Money,
        channel: SaleChannel = SaleChannel.WALK_IN
    ) {
        if (quantity <= 0) {
            viewModelScope.launch { _events.send(QuickRecordEvent.Message("Enter a quantity.")) }
            return
        }
        viewModelScope.launch {
            try {
                val today = DateUtils.today()
                val entity = SaleEntity(
                    saleDate = today,
                    channel = channel.name,
                    customer = null,
                    totalRevenue = 0L,
                    totalCost = 0L,
                    notes = null
                )
                val item = SaleItemEntity(
                    saleId = 0L,
                    productId = product.id,
                    productName = product.name,
                    quantity = quantity,
                    unitPrice = unitPrice.paisa,
                    unitCost = product.purchasePrice,
                    lineRevenue = 0L,
                    lineCost = 0L
                )
                saleRepository.save(entity, listOf(item))
                _events.send(QuickRecordEvent.Saved)
            } catch (e: Exception) {
                _events.send(QuickRecordEvent.Message("Could not record the sale."))
            }
        }
    }

    fun recordPurchase(
        product: ProductEntity,
        quantity: Int,
        unitPrice: Money,
        supplier: String? = null
    ) {
        if (quantity <= 0) {
            viewModelScope.launch { _events.send(QuickRecordEvent.Message("Enter a quantity.")) }
            return
        }
        viewModelScope.launch {
            try {
                val today = DateUtils.today()
                val entity = PurchaseEntity(
                    productId = product.id,
                    productName = product.name,
                    quantity = quantity,
                    unitPrice = unitPrice.paisa,
                    supplier = supplier?.takeIf { it.isNotBlank() } ?: product.supplier,
                    purchaseDate = today,
                    packagingCost = 0L,
                    shippingCost = 0L,
                    otherCost = 0L,
                    totalCost = 0L,
                    receiptPath = null,
                    notes = null
                )
                purchaseRepository.save(entity, previousQuantity = 0)
                _events.send(QuickRecordEvent.Saved)
            } catch (e: Exception) {
                _events.send(QuickRecordEvent.Message("Could not record the purchase."))
            }
        }
    }
}
