package com.guddiz.app.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guddiz.app.core.util.DateUtils
import com.guddiz.app.data.repository.ReportsRepository
import com.guddiz.app.data.repository.ReportsSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ReportsPeriod(val label: String) {
    THIS_MONTH("This month"),
    LAST_MONTH("Last month"),
    THIS_YEAR("This year"),
    ALL_TIME("All time")
}

data class ReportsUiState(
    val period: ReportsPeriod = ReportsPeriod.THIS_MONTH,
    val summary: ReportsSummary? = null,
    val isLoading: Boolean = false
)

class ReportsViewModel(private val repository: ReportsRepository) : ViewModel() {

    private val _state = MutableStateFlow(ReportsUiState())
    val state: StateFlow<ReportsUiState> = _state.asStateFlow()

    init { load(ReportsPeriod.THIS_MONTH) }

    fun onPeriodChange(period: ReportsPeriod) { load(period) }

    fun refresh() { load(_state.value.period) }

    private fun load(period: ReportsPeriod) {
        viewModelScope.launch {
            _state.value = _state.value.copy(period = period, isLoading = true)
            val range = rangeFor(period)
            val summary = repository.loadSummary(range.first, range.second)
            _state.value = _state.value.copy(summary = summary, isLoading = false)
        }
    }

    private fun rangeFor(period: ReportsPeriod): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        when (period) {
            ReportsPeriod.THIS_MONTH -> {
                return DateUtils.startOfCurrentMonth() to DateUtils.endOfDay(now)
            }
            ReportsPeriod.LAST_MONTH -> {
                val start = Calendar.getInstance()
                start.set(Calendar.DAY_OF_MONTH, 1)
                start.set(Calendar.HOUR_OF_DAY, 0)
                start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0)
                start.set(Calendar.MILLISECOND, 0)
                start.add(Calendar.MONTH, -1)
                val from = start.timeInMillis

                val end = Calendar.getInstance()
                end.set(Calendar.DAY_OF_MONTH, 1)
                end.set(Calendar.HOUR_OF_DAY, 0)
                end.set(Calendar.MINUTE, 0)
                end.set(Calendar.SECOND, 0)
                end.set(Calendar.MILLISECOND, 0)
                val to = end.timeInMillis - 1

                return from to to
            }
            ReportsPeriod.THIS_YEAR -> {
                return DateUtils.startOfCurrentYear() to DateUtils.endOfDay(now)
            }
            ReportsPeriod.ALL_TIME -> {
                return 0L to DateUtils.endOfDay(now)
            }
        }
    }

    fun categoryLabel(stored: String): String = repository.categoryLabel(stored)
}
