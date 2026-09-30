package com.guddiz.app.ui.expenses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guddiz.app.core.money.Money
import com.guddiz.app.core.util.DateUtils
import com.guddiz.app.data.local.entity.ExpenseCategory
import com.guddiz.app.data.local.entity.ExpenseEntity
import com.guddiz.app.data.repository.ExpenseRepository
import com.guddiz.app.ui.navigation.Routes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExpenseFormState(
    val id: Long = 0L,
    val category: ExpenseCategory = ExpenseCategory.PACKAGING,
    val amount: String = "",
    val expenseDate: Long = DateUtils.today(),
    val vendor: String = "",
    val description: String = "",
    val receiptPath: String? = null,
    val notes: String = "",
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errors: Map<String, String> = emptyMap()
)

sealed interface ExpenseFormEvent {
    data class Message(val text: String, val isError: Boolean) : ExpenseFormEvent
    data object Saved : ExpenseFormEvent
}

class ExpenseFormViewModel(
    private val repository: ExpenseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(ExpenseFormState())
    val state: StateFlow<ExpenseFormState> = _state.asStateFlow()

    private val _events = Channel<ExpenseFormEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        val id = savedStateHandle.get<String>(Routes.ARG_EXPENSE_ID)?.toLongOrNull() ?: 0L
        if (id > 0L) loadExpense(id)
    }

    private fun loadExpense(id: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, id = id, isEditing = true) }
            val e = repository.getExpense(id)
            if (e == null) {
                _events.send(ExpenseFormEvent.Message("This expense no longer exists.", true))
                return@launch
            }
            _state.update {
                it.copy(
                    id = e.id,
                    category = ExpenseCategory.fromStorage(e.category),
                    amount = Money(e.amount).toEditText(),
                    expenseDate = e.expenseDate,
                    vendor = e.vendor.orEmpty(),
                    description = e.description,
                    receiptPath = e.receiptPath,
                    notes = e.notes.orEmpty(),
                    isLoading = false
                )
            }
        }
    }

    fun onCategoryChange(v: ExpenseCategory) = _state.update { it.copy(category = v, errors = emptyMap()) }
    fun onAmountChange(v: String) = _state.update { it.copy(amount = v, errors = emptyMap()) }
    fun onDateChange(v: Long) = _state.update { it.copy(expenseDate = v, errors = emptyMap()) }
    fun onVendorChange(v: String) = _state.update { it.copy(vendor = v, errors = emptyMap()) }
    fun onDescriptionChange(v: String) = _state.update { it.copy(description = v, errors = emptyMap()) }
    fun onReceiptChange(v: String?) = _state.update { it.copy(receiptPath = v, errors = emptyMap()) }
    fun onNotesChange(v: String) = _state.update { it.copy(notes = v, errors = emptyMap()) }

    fun save() {
        val s = _state.value
        val errors = validate(s)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            viewModelScope.launch {
                _events.send(ExpenseFormEvent.Message(errors.values.first(), true))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val entity = ExpenseEntity(
                id = s.id,
                category = s.category.name,
                amount = Money.parse(s.amount)?.paisa ?: 0L,
                expenseDate = s.expenseDate,
                vendor = s.vendor.trim().ifBlank { null },
                description = s.description.trim(),
                receiptPath = s.receiptPath,
                notes = s.notes.trim().ifBlank { null }
            )
            try {
                repository.save(entity)
                _events.send(ExpenseFormEvent.Saved)
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false) }
                _events.send(ExpenseFormEvent.Message("Could not save. Please try again.", true))
            }
        }
    }

    private fun validate(s: ExpenseFormState): Map<String, String> = buildMap {
        if (s.description.isBlank()) put("description", "Please describe the expense.")
        val money = Money.parse(s.amount)
        when {
            money == null -> put("amount", "Enter a valid amount.")
            money.isNegative -> put("amount", "Amount cannot be negative.")
            money.isZero -> put("amount", "Amount must be greater than zero.")
        }
    }
}
