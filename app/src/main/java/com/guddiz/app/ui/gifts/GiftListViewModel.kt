package com.guddiz.app.ui.gifts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guddiz.app.data.local.entity.GiftEntity
import com.guddiz.app.data.repository.GiftRepository
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

sealed interface GiftListEvent {
    data class Message(val text: String) : GiftListEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class GiftListViewModel(private val repository: GiftRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _events = Channel<GiftListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val gifts: StateFlow<List<GiftEntity>> = _query
        .flatMapLatest { repository.observeGifts(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(v: String) { _query.value = v }

    fun duplicate(gift: GiftEntity) {
        viewModelScope.launch {
            val id = repository.duplicate(gift.id)
            _events.send(GiftListEvent.Message(if (id != null) "Duplicated" else "Could not duplicate."))
        }
    }

    fun softDelete(gift: GiftEntity) {
        viewModelScope.launch {
            repository.softDelete(gift)
            _events.send(GiftListEvent.Message("Moved to Recently deleted"))
        }
    }
}
