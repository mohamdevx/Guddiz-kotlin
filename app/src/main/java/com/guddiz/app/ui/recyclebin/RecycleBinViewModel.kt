package com.guddiz.app.ui.recyclebin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guddiz.app.data.model.DeletedItem
import com.guddiz.app.data.repository.RecycleBinRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface RecycleBinEvent {
    data class Message(val text: String) : RecycleBinEvent
}

class RecycleBinViewModel(
    private val repository: RecycleBinRepository
) : ViewModel() {

    private val _items = MutableStateFlow<List<DeletedItem>>(emptyList())
    val items: StateFlow<List<DeletedItem>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _events = Channel<RecycleBinEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.purgeOld(30)
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _items.value = repository.loadAll()
            _isLoading.value = false
        }
    }

    fun restore(item: DeletedItem) {
        viewModelScope.launch {
            try {
                repository.restore(item)
                _events.send(RecycleBinEvent.Message("Restored"))
                refresh()
            } catch (e: Exception) {
                _events.send(RecycleBinEvent.Message("Could not restore."))
            }
        }
    }

    fun hardDelete(item: DeletedItem) {
        viewModelScope.launch {
            try {
                repository.hardDelete(item)
                _events.send(RecycleBinEvent.Message("Deleted permanently"))
                refresh()
            } catch (e: Exception) {
                _events.send(RecycleBinEvent.Message("Could not delete."))
            }
        }
    }
}
