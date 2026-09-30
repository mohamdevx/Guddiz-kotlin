package com.guddiz.app.ui.foc

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guddiz.app.core.calculation.CostCalculator
import com.guddiz.app.core.money.Money
import com.guddiz.app.core.util.DateUtils
import com.guddiz.app.data.local.entity.FocEntity
import com.guddiz.app.data.local.entity.ProductEntity
import com.guddiz.app.data.repository.FocRepository
import com.guddiz.app.data.repository.ProductRepository
import com.guddiz.app.ui.navigation.Routes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FocFormState(
    val id: Long = 0L,
    val productId: Long? = null,
    val productName: String = "",
    val quantity: String = "1",
    val unitPrice: String = "",
    val recipient: String = "",
    val campaign: String = "",
    val date: Long = DateUtils.today(),
    val receiptPath: String? = null,
    val notes: String = "",
    val products: List<ProductEntity> = emptyList(),
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errors: Map<String, String> = emptyMap()
) {
    val lineCost: Money
        get() {
            val line = CostCalculator.Line(
                quantity = quantity.toIntOrNull() ?: 0,
                unitPrice = Money.parse(unitPrice) ?: Money.ZERO
            )
            return line.lineCost
        }
}

sealed interface FocFormEvent {
    data class Message(val text: String, val isError: Boolean) : FocFormEvent
    data object Saved : FocFormEvent
}

class FocFormViewModel(
    private val repository: FocRepository,
    private val productRepository: ProductRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(FocFormState())
    val state: StateFlow<FocFormState> = _state.asStateFlow()

    private val _events = Channel<FocFormEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            productRepository.observeProducts().collect { products ->
                _state.update { it.copy(products = products) }
            }
        }
        val id = savedStateHandle.get<String>(Routes.ARG_FOC_ID)?.toLongOrNull() ?: 0L
        if (id > 0L) load(id)
    }

    private fun load(id: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, id = id, isEditing = true) }
            val f = repository.getFoc(id)
            if (f == null) {
                _events.send(FocFormEvent.Message("FOC record not found.", true))
                return@launch
            }
            _state.update {
                it.copy(
                    id = f.id,
                    productId = f.productId,
                    productName = f.productName,
                    quantity = f.quantity.toString(),
                    unitPrice = Money(f.unitPrice).toEditText(),
                    recipient = f.recipient,
                    campaign = f.campaign.orEmpty(),
                    date = f.date,
                    receiptPath = f.receiptPath,
                    notes = f.notes.orEmpty(),
                    isLoading = false
                )
            }
        }
    }

    fun onProductSelected(p: ProductEntity) = _state.update {
        it.copy(
            productId = p.id,
            productName = p.name,
            unitPrice = if (it.unitPrice.isBlank()) Money(p.purchasePrice).toEditText() else it.unitPrice
        )
    }
    fun onQuantity(v: String) = _state.update { it.copy(quantity = v.filter(Char::isDigit)) }
    fun onUnitPrice(v: String) = _state.update { it.copy(unitPrice = v) }
    fun onRecipient(v: String) = _state.update { it.copy(recipient = v) }
    fun onCampaign(v: String) = _state.update { it.copy(campaign = v) }
    fun onDate(v: Long) = _state.update { it.copy(date = v) }
    fun onReceipt(v: String?) = _state.update { it.copy(receiptPath = v) }
    fun onNotes(v: String) = _state.update { it.copy(notes = v) }

    fun save() {
        val s = _state.value
        val errors = validate(s)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            viewModelScope.launch {
                _events.send(FocFormEvent.Message(errors.values.first(), true))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val entity = FocEntity(
                id = s.id,
                productId = s.productId!!,
                productName = s.productName,
                quantity = s.quantity.toIntOrNull() ?: 0,
                unitPrice = Money.parse(s.unitPrice)?.paisa ?: 0L,
                lineCost = 0L,
                recipient = s.recipient.trim(),
                campaign = s.campaign.trim().ifBlank { null },
                date = s.date,
                receiptPath = s.receiptPath,
                notes = s.notes.trim().ifBlank { null }
            )
            try {
                repository.save(entity)
                _events.send(FocFormEvent.Saved)
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false) }
                _events.send(FocFormEvent.Message("Could not save: " + (e.message ?: "unknown"), true))
            }
        }
    }

    private fun validate(s: FocFormState): Map<String, String> {
        val out = mutableMapOf<String, String>()
        if (s.productId == null) out["product"] = "Tap the Product field and choose a product."
        val qty = s.quantity.toIntOrNull()
        if (qty == null || qty <= 0) out["quantity"] = "Enter a quantity greater than zero."
        if (Money.parse(s.unitPrice) == null) out["unitPrice"] = "Enter a valid unit price, e.g. 500."
        if (s.recipient.isBlank()) out["recipient"] = "Enter the recipient name."
        return out
    }
}
