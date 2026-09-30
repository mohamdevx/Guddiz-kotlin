package com.guddiz.app.ui.purchases

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guddiz.app.core.calculation.CostCalculator
import com.guddiz.app.core.money.Money
import com.guddiz.app.core.util.DateUtils
import com.guddiz.app.data.local.entity.ProductEntity
import com.guddiz.app.data.local.entity.PurchaseEntity
import com.guddiz.app.data.repository.ProductRepository
import com.guddiz.app.data.repository.PurchaseRepository
import com.guddiz.app.ui.navigation.Routes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PurchaseFormState(
    val id: Long = 0L,
    val productId: Long? = null,
    val productName: String = "",
    val quantity: String = "1",
    val unitPrice: String = "",
    val supplier: String = "",
    val purchaseDate: Long = DateUtils.today(),
    val packagingCost: String = "",
    val shippingCost: String = "",
    val otherCost: String = "",
    val receiptPath: String? = null,
    val notes: String = "",
    val products: List<ProductEntity> = emptyList(),
    val previousQuantity: Int = 0,
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errors: Map<String, String> = emptyMap()
) {
    val cost: CostCalculator.PurchaseCost
        get() = CostCalculator.calculatePurchaseCost(
            quantity = quantity.toIntOrNull() ?: 0,
            unitPrice = Money.parse(unitPrice) ?: Money.ZERO,
            packagingCost = Money.parse(packagingCost) ?: Money.ZERO,
            shippingCost = Money.parse(shippingCost) ?: Money.ZERO,
            otherCost = Money.parse(otherCost) ?: Money.ZERO
        )
}

sealed interface PurchaseFormEvent {
    data class Message(val text: String, val isError: Boolean) : PurchaseFormEvent
    data object Saved : PurchaseFormEvent
}

class PurchaseFormViewModel(
    private val purchaseRepository: PurchaseRepository,
    private val productRepository: ProductRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(PurchaseFormState())
    val state: StateFlow<PurchaseFormState> = _state.asStateFlow()

    private val _events = Channel<PurchaseFormEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            productRepository.observeProducts().collect { products ->
                _state.update { it.copy(products = products) }
            }
        }
        val id = savedStateHandle.get<String>(Routes.ARG_PURCHASE_ID)?.toLongOrNull() ?: 0L
        if (id > 0L) loadPurchase(id)
    }

    private fun loadPurchase(id: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, id = id, isEditing = true) }
            val p = purchaseRepository.getPurchase(id)
            if (p == null) {
                _events.send(PurchaseFormEvent.Message("This purchase no longer exists.", true))
                return@launch
            }
            _state.update {
                it.copy(
                    id = p.id,
                    productId = p.productId,
                    productName = p.productName,
                    quantity = p.quantity.toString(),
                    unitPrice = Money(p.unitPrice).toEditText(),
                    supplier = p.supplier.orEmpty(),
                    purchaseDate = p.purchaseDate,
                    packagingCost = if (p.packagingCost > 0) Money(p.packagingCost).toEditText() else "",
                    shippingCost = if (p.shippingCost > 0) Money(p.shippingCost).toEditText() else "",
                    otherCost = if (p.otherCost > 0) Money(p.otherCost).toEditText() else "",
                    receiptPath = p.receiptPath,
                    notes = p.notes.orEmpty(),
                    previousQuantity = p.quantity,
                    isLoading = false
                )
            }
        }
    }

    fun onProductSelected(product: ProductEntity) = _state.update {
        it.copy(
            productId = product.id,
            productName = product.name,
            unitPrice = if (it.unitPrice.isBlank()) Money(product.purchasePrice).toEditText() else it.unitPrice,
            supplier = if (it.supplier.isBlank()) product.supplier.orEmpty() else it.supplier,
            errors = emptyMap()
        )
    }
    fun onQuantityChange(v: String) = _state.update { it.copy(quantity = v.filter(Char::isDigit), errors = emptyMap()) }
    fun onUnitPriceChange(v: String) = _state.update { it.copy(unitPrice = v, errors = emptyMap()) }
    fun onSupplierChange(v: String) = _state.update { it.copy(supplier = v, errors = emptyMap()) }
    fun onDateChange(v: Long) = _state.update { it.copy(purchaseDate = v, errors = emptyMap()) }
    fun onPackagingChange(v: String) = _state.update { it.copy(packagingCost = v, errors = emptyMap()) }
    fun onShippingChange(v: String) = _state.update { it.copy(shippingCost = v, errors = emptyMap()) }
    fun onOtherChange(v: String) = _state.update { it.copy(otherCost = v, errors = emptyMap()) }
    fun onReceiptChange(v: String?) = _state.update { it.copy(receiptPath = v, errors = emptyMap()) }
    fun onNotesChange(v: String) = _state.update { it.copy(notes = v, errors = emptyMap()) }

    fun save() {
        val s = _state.value
        val errors = validate(s)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            viewModelScope.launch {
                _events.send(PurchaseFormEvent.Message(errors.values.first(), true))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val entity = PurchaseEntity(
                id = s.id,
                productId = s.productId!!,
                productName = s.productName,
                quantity = s.quantity.toIntOrNull() ?: 0,
                unitPrice = Money.parse(s.unitPrice)?.paisa ?: 0L,
                supplier = s.supplier.trim().ifBlank { null },
                purchaseDate = s.purchaseDate,
                packagingCost = Money.parse(s.packagingCost)?.paisa ?: 0L,
                shippingCost = Money.parse(s.shippingCost)?.paisa ?: 0L,
                otherCost = Money.parse(s.otherCost)?.paisa ?: 0L,
                totalCost = 0L,
                receiptPath = s.receiptPath,
                notes = s.notes.trim().ifBlank { null }
            )
            try {
                purchaseRepository.save(entity, previousQuantity = s.previousQuantity)
                _events.send(PurchaseFormEvent.Saved)
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false) }
                _events.send(PurchaseFormEvent.Message("Could not save. Please try again.", true))
            }
        }
    }

    private fun validate(s: PurchaseFormState): Map<String, String> {
        val out = mutableMapOf<String, String>()
        if (s.productId == null) out["product"] = "Tap the Product field and choose a product."
        val qty = s.quantity.toIntOrNull()
        if (qty == null || qty <= 0) out["quantity"] = "Enter a quantity greater than zero."
        if (Money.parse(s.unitPrice) == null) out["unitPrice"] = "Enter a valid price, e.g. 1250.00."
        if (s.packagingCost.isNotBlank() && Money.parse(s.packagingCost) == null)
            out["packagingCost"] = "Enter a valid amount or leave empty."
        if (s.shippingCost.isNotBlank() && Money.parse(s.shippingCost) == null)
            out["shippingCost"] = "Enter a valid amount or leave empty."
        if (s.otherCost.isNotBlank() && Money.parse(s.otherCost) == null)
            out["otherCost"] = "Enter a valid amount or leave empty."
        return out
    }
}
