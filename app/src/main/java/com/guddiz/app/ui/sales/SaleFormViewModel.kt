package com.guddiz.app.ui.sales

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guddiz.app.core.calculation.CostCalculator
import com.guddiz.app.core.money.Money
import com.guddiz.app.core.util.DateUtils
import com.guddiz.app.data.local.entity.ProductEntity
import com.guddiz.app.data.local.entity.SaleChannel
import com.guddiz.app.data.local.entity.SaleEntity
import com.guddiz.app.data.local.entity.SaleItemEntity
import com.guddiz.app.data.repository.ProductRepository
import com.guddiz.app.data.repository.SaleRepository
import com.guddiz.app.ui.components.LineDraft
import com.guddiz.app.ui.navigation.Routes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class SaleFormState(
    val id: Long = 0L,
    val saleDate: Long = DateUtils.today(),
    val channel: SaleChannel = SaleChannel.WALK_IN,
    val customer: String = "",
    val notes: String = "",
    val lines: List<LineDraft> = listOf(LineDraft()),
    val products: List<ProductEntity> = emptyList(),
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errors: Map<String, String> = emptyMap()
) {
    val totalRevenue: Money
        get() = Money.sum(lines.map {
            Money.parse(it.unitPrice)?.let { p -> p * (it.quantity.toIntOrNull() ?: 0) }
                ?: Money.ZERO
        })

    val totalCost: Money
        get() = Money.sum(lines.map { line ->
            val product = products.firstOrNull { it.id == line.productId }
            val unitCost = product?.let { Money(it.purchasePrice) } ?: Money.ZERO
            unitCost * (line.quantity.toIntOrNull() ?: 0)
        })

    val profit: Money get() = totalRevenue - totalCost
}

sealed interface SaleFormEvent {
    data class Message(val text: String, val isError: Boolean) : SaleFormEvent
    data object Saved : SaleFormEvent
}

class SaleFormViewModel(
    private val repository: SaleRepository,
    private val productRepository: ProductRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(SaleFormState())
    val state: StateFlow<SaleFormState> = _state.asStateFlow()

    private val _events = Channel<SaleFormEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            productRepository.observeProducts().collect { products ->
                _state.value = _state.value.copy(products = products)
            }
        }
        val id = savedStateHandle.get<String>(Routes.ARG_SALE_ID)?.toLongOrNull() ?: 0L
        if (id > 0L) load(id)
    }

    private fun load(id: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, id = id, isEditing = true)
            val s = repository.getSale(id)
            if (s == null) {
                _events.send(SaleFormEvent.Message("Sale not found.", true))
                return@launch
            }
            val items = repository.getItems(id)
            _state.value = _state.value.copy(
                id = s.id,
                saleDate = s.saleDate,
                channel = SaleChannel.fromStorage(s.channel),
                customer = s.customer.orEmpty(),
                notes = s.notes.orEmpty(),
                lines = if (items.isEmpty()) listOf(LineDraft()) else items.map {
                    LineDraft(
                        productId = it.productId,
                        productName = it.productName,
                        unitPrice = Money(it.unitPrice).toEditText(),
                        quantity = it.quantity.toString()
                    )
                },
                isLoading = false
            )
        }
    }

    fun onDateChange(v: Long) { _state.value = _state.value.copy(saleDate = v) }
    fun onChannelChange(v: SaleChannel) { _state.value = _state.value.copy(channel = v) }
    fun onCustomerChange(v: String) { _state.value = _state.value.copy(customer = v) }
    fun onNotesChange(v: String) { _state.value = _state.value.copy(notes = v) }

    fun onLineChange(index: Int, line: LineDraft) {
        val cur = _state.value
        val list = cur.lines.toMutableList()
        if (index in list.indices) list[index] = line
        _state.value = cur.copy(lines = list)
    }

    fun onAddLine() {
        val cur = _state.value
        _state.value = cur.copy(lines = cur.lines + LineDraft())
    }

    fun onRemoveLine(index: Int) {
        val cur = _state.value
        val list = cur.lines.toMutableList()
        if (index in list.indices && list.size > 1) list.removeAt(index)
        _state.value = cur.copy(lines = list)
    }

    fun save() {
        val s = _state.value
        val errors = validate(s)
        if (errors.isNotEmpty()) {
            _state.value = s.copy(errors = errors)
            viewModelScope.launch {
                _events.send(SaleFormEvent.Message(errors.values.first(), true))
            }
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)

            val validLines = s.lines.filter { it.productId != null }
            val items = validLines.map { line ->
                val product = s.products.firstOrNull { it.id == line.productId }
                val unitCost = product?.purchasePrice ?: 0L
                val qty = line.quantity.toIntOrNull() ?: 0
                val unitPrice = Money.parse(line.unitPrice)?.paisa ?: 0L
                SaleItemEntity(
                    saleId = 0L,
                    productId = line.productId!!,
                    productName = line.productName,
                    quantity = qty,
                    unitPrice = unitPrice,
                    unitCost = unitCost,
                    lineRevenue = 0L,
                    lineCost = 0L
                )
            }

            val entity = SaleEntity(
                id = s.id,
                saleDate = s.saleDate,
                channel = s.channel.name,
                customer = s.customer.trim().ifBlank { null },
                totalRevenue = 0L,
                totalCost = 0L,
                notes = s.notes.trim().ifBlank { null }
            )

            try {
                repository.save(entity, items)
                _events.send(SaleFormEvent.Saved)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false)
                _events.send(SaleFormEvent.Message("Could not save. Please try again.", true))
            }
        }
    }

    private fun validate(s: SaleFormState): Map<String, String> {
        val out = mutableMapOf<String, String>()
        val validLines = s.lines.filter { it.productId != null }
        if (validLines.isEmpty()) out["lines"] = "Add at least one product."
        if (validLines.any { (it.quantity.toIntOrNull() ?: 0) <= 0 })
            out["lines"] = "Every product needs a quantity greater than zero."
        if (validLines.any { Money.parse(it.unitPrice) == null })
            out["lines"] = "Every product needs a valid sale price."
        return out
    }
}
