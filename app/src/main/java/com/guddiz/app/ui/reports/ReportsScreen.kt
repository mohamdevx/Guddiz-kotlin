package com.guddiz.app.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guddiz.app.core.money.Money
import com.guddiz.app.data.local.dao.TopProduct
import com.guddiz.app.data.repository.ReportsSummary
import com.guddiz.app.ui.AppViewModelProvider
import com.guddiz.app.ui.components.GuddizSectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ReportsPeriod.entries.toList()) { period ->
                    FilterChip(
                        selected = state.period == period,
                        onClick = { viewModel.onPeriodChange(period) },
                        label = { Text(period.label) }
                    )
                }
            }

            val summary = state.summary
            if (state.isLoading || summary == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { SalesSection(summary) }
                if (summary.topProducts.isNotEmpty()) {
                    item { TopProductsSection(summary.topProducts) }
                }
                item { BusinessSection(summary) }
                item { GiveawaysSection(summary) }
                item { StockSection(summary) }
                if (summary.expensesByCategory.isNotEmpty()) {
                    item { ExpenseBreakdownSection(summary, viewModel) }
                }
            }
        }
    }
}

@Composable
private fun SalesSection(s: ReportsSummary) {
    GuddizSectionCard("Sales") {
        SummaryRow("Total revenue", s.totalSales.format(), "${s.salesCount} sales")
        SummaryRow("Cost of goods", s.totalCostOfGoods.format())
        SummaryRow(
            "Profit",
            s.totalProfit.format(),
            valueColor = if (s.totalProfit.isNegative) MaterialTheme.colorScheme.error
                         else MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun TopProductsSection(products: List<TopProduct>) {
    GuddizSectionCard("Top selling products") {
        products.forEachIndexed { index, p ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${index + 1}.  ${p.productName}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Text(
                    "× ${p.qty}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    Money(p.revenue).format(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun BusinessSection(s: ReportsSummary) {
    GuddizSectionCard("Purchases & Expenses") {
        SummaryRow("Total purchases", s.totalPurchases.format(), "${s.purchaseCount} entries")
        SummaryRow("Total expenses", s.totalExpenses.format(), "${s.expenseCount} entries")
        SummaryRow("Packaging cost", s.totalPackaging.format())
        SummaryRow("Shipping cost", s.totalShipping.format())
    }
}

@Composable
private fun GiveawaysSection(s: ReportsSummary) {
    GuddizSectionCard("Giveaways") {
        SummaryRow("PR packages", s.totalPrCost.format(), "${s.prCount} sent")
        SummaryRow("Gifts", s.totalGiftCost.format(), "${s.giftCount} given")
        SummaryRow("Free of cost", s.totalFocCost.format(), "${s.focCount} items")
    }
}

@Composable
private fun StockSection(s: ReportsSummary) {
    GuddizSectionCard("Stock snapshot") {
        SummaryRow("Total quantity", s.totalStockQuantity.toString())
        SummaryRow("Stock value", s.totalStockValue.format())
    }
}

@Composable
private fun ExpenseBreakdownSection(s: ReportsSummary, vm: ReportsViewModel) {
    GuddizSectionCard("Expenses by category") {
        s.expensesByCategory.forEach { pair ->
            SummaryRow(vm.categoryLabel(pair.first), pair.second.format())
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    helper: String? = null,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = valueColor
            )
        }
        if (helper != null) {
            Text(
                text = helper,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
