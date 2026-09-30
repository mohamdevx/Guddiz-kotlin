package com.guddiz.app.ui.purchases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guddiz.app.data.local.entity.PurchaseEntity
import com.guddiz.app.data.repository.PurchaseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface PurchaseListEvent {
    data class Message(val text: String) : PurchaseListEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class PurchaseListViewModel(private val repository: PurchaseRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _events = Channel<PurchaseListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val purchases: StateFlow<List<PurchaseEntity>> = _query
        .flatMapLatest { repository.observePurchases(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(v: String) = _query.update { v }

    fun duplicate(purchase: PurchaseEntity) {
        viewModelScope.launch {
            val id = repository.duplicate(purchase.id)
            _events.send(PurchaseListEvent.Message(if (id != null) "Duplicated" else "Could not duplicate."))
        }
    }

    fun softDelete(purchase: PurchaseEntity) {
        viewModelScope.launch {
            repository.softDelete(purchase)
            _events.send(PurchaseListEvent.Message("Moved to Recently deleted"))
        }
    }
}
