package com.guddiz.app.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guddiz.app.data.local.entity.SaleEntity
import com.guddiz.app.data.repository.SaleRepository
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

sealed interface SaleListEvent {
    data class Message(val text: String) : SaleListEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class SaleListViewModel(private val repository: SaleRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _channel = MutableStateFlow("")
    val channel: StateFlow<String> = _channel.asStateFlow()

    private val _events = Channel<SaleListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val sales: StateFlow<List<SaleEntity>> =
        combine(_query, _channel) { q, c -> Pair(q, c) }
            .flatMapLatest { p -> repository.observeSales(p.first, p.second) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(v: String) = _query.update { v }
    fun onChannelChange(v: String) = _channel.update { v }

    fun duplicate(sale: SaleEntity) {
        viewModelScope.launch {
            val id = repository.duplicate(sale.id)
            _events.send(SaleListEvent.Message(if (id != null) "Duplicated" else "Could not duplicate."))
        }
    }

    fun softDelete(sale: SaleEntity) {
        viewModelScope.launch {
            repository.softDelete(sale)
            _events.send(SaleListEvent.Message("Moved to Recently deleted"))
        }
    }
}
