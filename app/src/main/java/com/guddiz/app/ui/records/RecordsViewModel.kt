package com.guddiz.app.ui.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guddiz.app.data.model.RecordType
import com.guddiz.app.data.model.UnifiedRecord
import com.guddiz.app.data.repository.RecordsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class RecordsViewModel(
    private val repository: RecordsRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _activeTypes: MutableStateFlow<Set<RecordType>> =
        MutableStateFlow(RecordType.all)
    val activeTypes: StateFlow<Set<RecordType>> = _activeTypes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val records: StateFlow<List<UnifiedRecord>> =
        combine(_query.debounce(200L), _activeTypes) { q, t -> Pair(q, t) }
            .flatMapLatest { pair ->
                flow {
                    _isLoading.value = true
                    val results = repository.search(pair.first, pair.second)
                    _isLoading.value = false
                    emit(results)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(v: String) { _query.value = v }

    fun toggleType(type: RecordType) {
        val current = _activeTypes.value
        _activeTypes.value =
            if (current.size == RecordType.all.size) setOf(type)
            else if (current.contains(type)) {
                val next = current - type
                if (next.isEmpty()) RecordType.all else next
            } else current + type
    }

    fun selectAllTypes() { _activeTypes.value = RecordType.all }
}
