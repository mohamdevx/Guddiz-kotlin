package com.guddiz.app.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guddiz.app.core.util.DateUtils
import com.guddiz.app.data.local.entity.ProductEntity
import com.guddiz.app.data.repository.ProductRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ProductFilterMode(val title: String) {
    ALL("Products"),
    LOW_STOCK("Low on stock"),
    EXPIRING_SOON("Expiring soon")
}

sealed interface ProductListEvent {
    data class Message(val text: String) : ProductListEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProductListViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _filterMode = MutableStateFlow(ProductFilterMode.ALL)
    val filterMode: StateFlow<ProductFilterMode> = _filterMode.asStateFlow()

    private val _events = Channel<ProductListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val products: StateFlow<List<ProductEntity>> =
        combine(_searchQuery, _selectedCategory, _filterMode) { q, c, f ->
            Triple(q, c, f)
        }
            .flatMapLatest { triple ->
                val q = triple.first
                val c = triple.second
                val f = triple.third
                repository.observeProducts(q, c).map { list -> applyFilter(list, f) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun applyFilter(
        list: List<ProductEntity>,
        mode: ProductFilterMode
    ): List<ProductEntity> = when (mode) {
        ProductFilterMode.ALL -> list
        ProductFilterMode.LOW_STOCK -> list.filter {
            it.minStockLevel > 0 && it.currentQuantity <= it.minStockLevel
        }
        ProductFilterMode.EXPIRING_SOON -> list.filter {
            DateUtils.isExpiringSoon(it.expiryDate)
        }
    }

    fun onSearchChange(v: String) { _searchQuery.value = v }
    fun onCategoryChange(v: String) { _selectedCategory.value = v }
    fun setFilterMode(mode: ProductFilterMode) { _filterMode.value = mode }

    fun duplicate(product: ProductEntity) {
        viewModelScope.launch {
            val newId = repository.duplicate(product.id)
            if (newId != null) {
                _events.send(ProductListEvent.Message("Duplicated"))
            } else {
                _events.send(ProductListEvent.Message("Could not duplicate."))
            }
        }
    }

    fun softDelete(product: ProductEntity) {
        viewModelScope.launch {
            repository.softDelete(product.id)
            _events.send(ProductListEvent.Message("Moved to Recently deleted"))
        }
    }
}
