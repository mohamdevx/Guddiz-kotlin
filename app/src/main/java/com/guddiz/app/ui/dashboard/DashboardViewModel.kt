package com.guddiz.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guddiz.app.data.model.DailyRevenue
import com.guddiz.app.data.repository.DashboardRepository
import com.guddiz.app.data.repository.DashboardStats
import com.guddiz.app.data.repository.SaleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(
    repository: DashboardRepository,
    saleRepository: SaleRepository
) : ViewModel() {

    val stats: StateFlow<DashboardStats> = repository.stats
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardStats()
        )

    val weeklyRevenue: StateFlow<List<DailyRevenue>> =
        saleRepository.observeLast7DaysRevenue()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )
}
