package com.guddiz.app.ui.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guddiz.app.data.local.entity.ProductEntity
import com.guddiz.app.data.repository.ProductRepository
import com.guddiz.app.ui.navigation.Routes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProductDetailViewModel(
    private val repository: ProductRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val productId: Long =
        savedStateHandle.get<String>(Routes.ARG_PRODUCT_ID)?.toLongOrNull() ?: 0L

    val product: StateFlow<ProductEntity?> = repository.observeProduct(productId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun delete(onDeleted: () -> Unit) {
        val current = product.value ?: return
        viewModelScope.launch {
            repository.softDelete(current.id)
            onDeleted()
        }
    }
}
