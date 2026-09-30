package com.guddiz.app.ui.gifts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guddiz.app.core.calculation.CostCalculator
import com.guddiz.app.core.money.Money
import com.guddiz.app.core.util.DateUtils
import com.guddiz.app.data.local.entity.GiftEntity
import com.guddiz.app.data.local.entity.GiftItemEntity
import com.guddiz.app.data.local.entity.ProductEntity
import com.guddiz.app.data.repository.GiftRepository
import com.guddiz.app.data.repository.ProductRepository
import com.guddiz.app.ui.components.LineDraft
import com.guddiz.app.ui.navigation.Routes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GiftFormState(
    val id: Long = 0L,
    val recipient: String = "",
    val occasion: String = "",
    val date: Long = DateUtils.today(),
    val packagingCost: String = "",
    val courierCost: String = "",
    val otherCost: String = "",
    val receiptPath: String? = null,
    val notes: String = "",
    val lines: List<LineDraft> = listOf(LineDraft()),
    val products: List<ProductEntity> = emptyList(),
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errors: Map<String, String> = emptyMap()
) {
    val cost: CostCalculator.PackageCost
        get() = CostCalculator.calculatePackageCost(
            lines = lines.map {
                CostCalculator.Line(
                    quantity = it.quantity.toIntOrNull() ?: 0,
                    unitPrice = Money.parse(it.unitPrice) ?: Money.ZERO
                )
            },
            packagingCost = Money.parse(packagingCost) ?: Money.ZERO,
            courierCost = Money.parse(courierCost) ?: Money.ZERO,
            otherCost = Money.parse(otherCost) ?: Money.ZERO
        )
}

sealed interface GiftFormEvent {
    data class Message(val text: String, val isError: Boolean) : GiftFormEvent
    data object Saved : GiftFormEvent
}

class GiftFormViewModel(
    private val repository: GiftRepository,
    private val productRepository: ProductRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(GiftFormState())
    val state: StateFlow<GiftFormState> = _state.asStateFlow()

    private val _events = Channel<GiftFormEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            productRepository.observeProducts().collect { products ->
                _state.update { it.copy(products = products) }
            }
        }
        val id = savedStateHandle.get<String>(Routes.ARG_GIFT_ID)?.toLongOrNull() ?: 0L
        if (id > 0L) load(id)
    }

    private fun load(id: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, id = id, isEditing = true) }
            val g = repository.getGift(id)
            if (g == null) {
                _events.send(GiftFormEvent.Message("Gift not found.", true)); return@launch
            }
            val items = repository.getItems(id)
            _state.update {
                it.copy(
                    id = g.id,
                    recipient = g.recipient,
                    occasion = g.occasion,
                    date = g.date,
                    packagingCost = if (g.packagingCost > 0) Money(g.packagingCost).toEditText() else "",
                    courierCost = if (g.courierCost > 0) Money(g.courierCost).toEditText() else "",
                    otherCost = if (g.otherCost > 0) Money(g.otherCost).toEditText() else "",
                    receiptPath = g.receiptPath,
                    notes = g.notes.orEmpty(),
                    lines = if (items.isEmpty()) listOf(LineDraft()) else items.map {
                        LineDraft(it.productId, it.productName,
                            Money(it.unitPrice).toEditText(), it.quantity.toString())
                    },
                    isLoading = false
                )
            }
        }
    }

    fun onRecipient(v: String) = _state.update { it.copy(recipient = v) }
    fun onOccasion(v: String) = _state.update { it.copy(occasion = v) }
    fun onDate(v: Long) = _state.update { it.copy(date = v) }
    fun onPackaging(v: String) = _state.update { it.copy(packagingCost = v) }
    fun onCourier(v: String) = _state.update { it.copy(courierCost = v) }
    fun onOther(v: String) = _state.update { it.copy(otherCost = v) }
    fun onReceipt(v: String?) = _state.update { it.copy(receiptPath = v) }
    fun onNotes(v: String) = _state.update { it.copy(notes = v) }

    fun onLineChange(i: Int, l: LineDraft) = _state.update {
        val nl = it.lines.toMutableList(); if (i in nl.indices) nl[i] = l
        it.copy(lines = nl)
    }
    fun onAddLine() = _state.update { it.copy(lines = it.lines + LineDraft()) }
    fun onRemoveLine(i: Int) = _state.update {
        val nl = it.lines.toMutableList()
        if (i in nl.indices && nl.size > 1) nl.removeAt(i)
        it.copy(lines = nl)
    }

    fun save() {
        val s = _state.value
        val errors = validate(s)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            viewModelScope.launch {
                _events.send(GiftFormEvent.Message(errors.values.first(), true))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val validLines = s.lines.filter { it.productId != null }
            val items = validLines.map {
                GiftItemEntity(
                    giftId = 0L, productId = it.productId!!, productName = it.productName,
                    quantity = it.quantity.toIntOrNull() ?: 0,
                    unitPrice = Money.parse(it.unitPrice)?.paisa ?: 0L,
                    lineCost = 0L
                )
            }
            val entity = GiftEntity(
                id = s.id, recipient = s.recipient.trim(), occasion = s.occasion.trim(),
                date = s.date, productCost = 0L,
                packagingCost = Money.parse(s.packagingCost)?.paisa ?: 0L,
                courierCost = Money.parse(s.courierCost)?.paisa ?: 0L,
                otherCost = Money.parse(s.otherCost)?.paisa ?: 0L,
                totalCost = 0L, receiptPath = s.receiptPath,
                notes = s.notes.trim().ifBlank { null }
            )
            try {
                repository.save(entity, items)
                _events.send(GiftFormEvent.Saved)
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false) }
                _events.send(GiftFormEvent.Message("Could not save. Please try again.", true))
            }
        }
    }

    private fun validate(s: GiftFormState): Map<String, String> = buildMap {
        if (s.recipient.isBlank()) put("recipient", "Please enter the recipient.")
        if (s.occasion.isBlank()) put("occasion", "Please enter the occasion.")
        val valid = s.lines.filter { it.productId != null }
        if (valid.isEmpty()) put("lines", "Add at least one product.")
        if (valid.any { (it.quantity.toIntOrNull() ?: 0) <= 0 })
            put("lines", "Every product needs a quantity greater than zero.")
    }
}
