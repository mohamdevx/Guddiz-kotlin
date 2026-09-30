package com.guddiz.app.ui.products

import androidx.lifecycle.ViewModel
import com.guddiz.app.data.local.entity.StockMovementEntity
import com.guddiz.app.data.repository.StockMovementRepository
import kotlinx.coroutines.flow.Flow

class StockMovementViewModel(private val repository: StockMovementRepository) : ViewModel() {
    fun observeForProduct(productId: Long): Flow<List<StockMovementEntity>> =
        repository.observeForProduct(productId)
}
