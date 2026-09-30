package com.guddiz.app.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guddiz.app.data.local.entity.ExpenseEntity
import com.guddiz.app.data.repository.ExpenseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ExpenseListEvent {
    data class Message(val text: String) : ExpenseListEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseListViewModel(private val repository: ExpenseRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _category = MutableStateFlow("")
    val category: StateFlow<String> = _category.asStateFlow()

    private val _events = Channel<ExpenseListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val expenses: StateFlow<List<ExpenseEntity>> =
        combine(_query, _category) { q, c -> Pair(q, c) }
            .flatMapLatest { p -> repository.observeExpenses(p.first, p.second) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(v: String) = _query.update { v }
    fun onCategoryChange(v: String) = _category.update { v }

    fun duplicate(expense: ExpenseEntity) {
        viewModelScope.launch {
            val id = repository.duplicate(expense.id)
            _events.send(ExpenseListEvent.Message(if (id != null) "Duplicated" else "Could not duplicate."))
        }
    }

    fun softDelete(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.softDelete(expense.id)
            _events.send(ExpenseListEvent.Message("Moved to Recently deleted"))
        }
    }
}
