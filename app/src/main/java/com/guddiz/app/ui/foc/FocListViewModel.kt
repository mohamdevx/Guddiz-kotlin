package com.guddiz.app.ui.foc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guddiz.app.data.local.entity.FocEntity
import com.guddiz.app.data.repository.FocRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface FocListEvent {
    data class Message(val text: String) : FocListEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class FocListViewModel(private val repository: FocRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _events = Channel<FocListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val records: StateFlow<List<FocEntity>> = _query
        .flatMapLatest { repository.observeFoc(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(v: String) { _query.value = v }

    fun duplicate(foc: FocEntity) {
        viewModelScope.launch {
            val id = repository.duplicate(foc.id)
            _events.send(FocListEvent.Message(if (id != null) "Duplicated" else "Could not duplicate."))
        }
    }

    fun softDelete(foc: FocEntity) {
        viewModelScope.launch {
            repository.softDelete(foc)
            _events.send(FocListEvent.Message("Moved to Recently deleted"))
        }
    }
}
