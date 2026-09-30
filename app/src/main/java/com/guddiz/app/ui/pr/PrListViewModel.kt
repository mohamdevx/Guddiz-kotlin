package com.guddiz.app.ui.pr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guddiz.app.data.local.entity.PrPackageEntity
import com.guddiz.app.data.repository.PrPackageRepository
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

sealed interface PrListEvent {
    data class Message(val text: String) : PrListEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class PrListViewModel(private val repository: PrPackageRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _events = Channel<PrListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val packages: StateFlow<List<PrPackageEntity>> = _query
        .flatMapLatest { repository.observePackages(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(v: String) { _query.value = v }

    fun duplicate(pkg: PrPackageEntity) {
        viewModelScope.launch {
            val id = repository.duplicate(pkg.id)
            _events.send(PrListEvent.Message(if (id != null) "Duplicated" else "Could not duplicate."))
        }
    }

    fun softDelete(pkg: PrPackageEntity) {
        viewModelScope.launch {
            repository.softDelete(pkg)
            _events.send(PrListEvent.Message("Moved to Recently deleted"))
        }
    }
}
