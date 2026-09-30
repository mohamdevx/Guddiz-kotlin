package com.guddiz.app.ui.pr

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guddiz.app.core.calculation.CostCalculator
import com.guddiz.app.core.money.Money
import com.guddiz.app.core.util.DateUtils
import com.guddiz.app.data.local.entity.PrPackageEntity
import com.guddiz.app.data.local.entity.PrPackageItemEntity
import com.guddiz.app.data.local.entity.PrRecipientType
import com.guddiz.app.data.local.entity.PrStatus
import com.guddiz.app.data.local.entity.ProductEntity
import com.guddiz.app.data.repository.PrPackageRepository
import com.guddiz.app.data.repository.ProductRepository
import com.guddiz.app.ui.components.LineDraft
import com.guddiz.app.ui.navigation.Routes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class PrFormState(
    val id: Long = 0L,
    val recipientName: String = "",
    val recipientType: PrRecipientType = PrRecipientType.INFLUENCER,
    val contactInfo: String = "",
    val campaignName: String = "",
    val status: PrStatus = PrStatus.PLANNED,
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

sealed interface PrFormEvent {
    data class Message(val text: String, val isError: Boolean) : PrFormEvent
    data object Saved : PrFormEvent
}

class PrFormViewModel(
    private val repository: PrPackageRepository,
    private val productRepository: ProductRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state: MutableStateFlow<PrFormState> = MutableStateFlow(PrFormState())
    val state: StateFlow<PrFormState> = _state.asStateFlow()

    private val _events = Channel<PrFormEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            productRepository.observeProducts().collect { products ->
                val current = _state.value
                _state.value = current.copy(products = products)
            }
        }
        val id = savedStateHandle.get<String>(Routes.ARG_PR_ID)?.toLongOrNull() ?: 0L
        if (id > 0L) load(id)
    }

    private fun load(id: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, id = id, isEditing = true)
            val p = repository.getPackage(id)
            if (p == null) {
                _events.send(PrFormEvent.Message("PR package not found.", true))
                return@launch
            }
            val items = repository.getItems(id)
            _state.value = _state.value.copy(
                id = p.id,
                recipientName = p.recipientName,
                recipientType = PrRecipientType.fromStorage(p.recipientType),
                contactInfo = p.contactInfo.orEmpty(),
                campaignName = p.campaignName.orEmpty(),
                status = PrStatus.fromStorage(p.status),
                date = p.date,
                packagingCost = if (p.packagingCost > 0) Money(p.packagingCost).toEditText() else "",
                courierCost = if (p.courierCost > 0) Money(p.courierCost).toEditText() else "",
                otherCost = if (p.otherCost > 0) Money(p.otherCost).toEditText() else "",
                receiptPath = p.receiptPath,
                notes = p.notes.orEmpty(),
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

    fun onRecipientName(v: String) { _state.value = _state.value.copy(recipientName = v) }
    fun onRecipientType(v: PrRecipientType) { _state.value = _state.value.copy(recipientType = v) }
    fun onContact(v: String) { _state.value = _state.value.copy(contactInfo = v) }
    fun onCampaign(v: String) { _state.value = _state.value.copy(campaignName = v) }
    fun onStatus(v: PrStatus) { _state.value = _state.value.copy(status = v) }
    fun onDate(v: Long) { _state.value = _state.value.copy(date = v) }
    fun onPackaging(v: String) { _state.value = _state.value.copy(packagingCost = v) }
    fun onCourier(v: String) { _state.value = _state.value.copy(courierCost = v) }
    fun onOther(v: String) { _state.value = _state.value.copy(otherCost = v) }
    fun onReceipt(v: String?) { _state.value = _state.value.copy(receiptPath = v) }
    fun onNotes(v: String) { _state.value = _state.value.copy(notes = v) }

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
                _events.send(PrFormEvent.Message(errors.values.first(), true))
            }
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)

            val validLines = s.lines.filter { it.productId != null }
            val items = validLines.map {
                PrPackageItemEntity(
                    packageId = 0L,
                    productId = it.productId!!,
                    productName = it.productName,
                    quantity = it.quantity.toIntOrNull() ?: 0,
                    unitPrice = Money.parse(it.unitPrice)?.paisa ?: 0L,
                    lineCost = 0L
                )
            }

            val entity = PrPackageEntity(
                id = s.id,
                recipientName = s.recipientName.trim(),
                recipientType = s.recipientType.name,
                contactInfo = s.contactInfo.trim().ifBlank { null },
                campaignName = s.campaignName.trim().ifBlank { null },
                status = s.status.name,
                date = s.date,
                productCost = 0L,
                packagingCost = Money.parse(s.packagingCost)?.paisa ?: 0L,
                courierCost = Money.parse(s.courierCost)?.paisa ?: 0L,
                otherCost = Money.parse(s.otherCost)?.paisa ?: 0L,
                totalCost = 0L,
                receiptPath = s.receiptPath,
                notes = s.notes.trim().ifBlank { null }
            )

            try {
                repository.save(entity, items)
                _events.send(PrFormEvent.Saved)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false)
                _events.send(PrFormEvent.Message("Could not save. Please try again.", true))
            }
        }
    }

    private fun validate(s: PrFormState): Map<String, String> {
        val out = mutableMapOf<String, String>()
        if (s.recipientName.isBlank()) out["recipientName"] = "Please enter the recipient name."
        val validLines = s.lines.filter { it.productId != null }
        if (validLines.isEmpty()) out["lines"] = "Add at least one product."
        if (validLines.any { (it.quantity.toIntOrNull() ?: 0) <= 0 })
            out["lines"] = "Every product needs a quantity greater than zero."
        if (validLines.any { Money.parse(it.unitPrice) == null })
            out["lines"] = "Every product needs a valid unit price."
        return out
    }
}
