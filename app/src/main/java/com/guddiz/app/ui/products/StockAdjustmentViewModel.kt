package com.guddiz.app.ui.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guddiz.app.data.local.entity.ProductEntity
import com.guddiz.app.data.local.entity.StockMovementType
import com.guddiz.app.data.repository.ProductRepository
import com.guddiz.app.data.repository.StockMovementRepository
import com.guddiz.app.ui.navigation.Routes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface StockAdjustmentEvent {
    data class Message(val text: String, val isError: Boolean) : StockAdjustmentEvent
    data object Saved : StockAdjustmentEvent
}

class StockAdjustmentViewModel(
    private val productRepository: ProductRepository,
    private val movementRepository: StockMovementRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val productId: Long =
        savedStateHandle.get<String>(Routes.ARG_PRODUCT_ID)?.toLongOrNull() ?: 0L

    val product: StateFlow<ProductEntity?> = productRepository.observeProduct(productId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _events = Channel<StockAdjustmentEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun submit(type: StockMovementType, units: Int, note: String?) {
        val current = product.value
        if (current == null) {
            viewModelScope.launch {
                _events.send(StockAdjustmentEvent.Message("Product not found.", true))
            }
            return
        }
        if (units < 0) {
            viewModelScope.launch {
                _events.send(StockAdjustmentEvent.Message("Enter a valid number.", true))
            }
            return
        }

        val delta = when (type) {
            StockMovementType.ADJUSTMENT -> units - current.currentQuantity
            else -> -units
        }

        if (delta == 0) {
            viewModelScope.launch {
                _events.send(
                    StockAdjustmentEvent.Message("Stock is already at this amount.", true)
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                movementRepository.record(
                    productId = current.id,
                    productName = current.name,
                    type = type,
                    delta = delta,
                    note = note
                )
                _events.send(StockAdjustmentEvent.Saved)
            } catch (e: Exception) {
                _events.send(
                    StockAdjustmentEvent.Message("Could not save. Please try again.", true)
                )
            }
        }
    }
}
