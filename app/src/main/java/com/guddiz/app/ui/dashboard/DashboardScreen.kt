package com.guddiz.app.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guddiz.app.core.util.DateUtils
import com.guddiz.app.data.repository.DashboardRepository
import com.guddiz.app.data.repository.DashboardStats
import com.guddiz.app.ui.AppViewModelProvider
import com.guddiz.app.ui.components.WeeklySalesChart
import com.guddiz.app.ui.navigation.Routes

private val SuccessGreen = Color(0xFF2E7D32)
private val WarningAmber = Color(0xFFB26A00)

private enum class DeltaGoodDirection { UP, DOWN }

private data class StatCardData(
    val label: String,
    val value: String,
    val icon: ImageVector,
    val deltaPercent: Int? = null,
    val deltaGoodDirection: DeltaGoodDirection = DeltaGoodDirection.UP,
    val route: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val weeklyRevenue by viewModel.weeklyRevenue.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Guddiz",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            DateUtils.format(System.currentTimeMillis()),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
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
                .padding(horizontal = 12.dp)
                .padding(top = 4.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // --- Alerts (only if there is something to warn about) ---
            if (stats.lowStockCount > 0) {
                AlertChip(
                    icon = Icons.Outlined.ErrorOutline,
                    text = "${stats.lowStockCount} products low on stock",
                    container = MaterialTheme.colorScheme.errorContainer,
                    onContent = MaterialTheme.colorScheme.onErrorContainer,
                    onClick = { onNavigate(Routes.PRODUCTS_LOW_STOCK) }
                )
            }
            if (stats.expiringSoonCount > 0) {
                AlertChip(
                    icon = Icons.Outlined.Schedule,
                    text = "${stats.expiringSoonCount} products expiring soon",
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                    onContent = MaterialTheme.colorScheme.onTertiaryContainer,
                    onClick = { onNavigate(Routes.PRODUCTS_EXPIRING) }
                )
            }

            // --- Chart (compact) ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                WeeklySalesChart(
                    data = weeklyRevenue,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            // --- Grid of stats (compact, 2 columns) ---
            StatsGrid(stats, onNavigate)

            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun AlertChip(
    icon: ImageVector,
    text: String,
    container: Color,
    onContent: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = onContent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = onContent
            )
        }
    }
}

@Composable
private fun StatsGrid(stats: DashboardStats, onNavigate: (String) -> Unit) {
    val salesDelta = DashboardRepository.percentDelta(
        stats.currentMonthRevenue,
        stats.lastMonthRevenue
    )
    val expenseDelta = DashboardRepository.percentDelta(
        stats.currentMonthExpenses,
        stats.lastMonthExpenses
    )

    val cards = listOf(
        StatCardData("Products", stats.totalProducts.toString(),
            Icons.Outlined.Inventory2, route = Routes.PRODUCTS),
        StatCardData("Stock Qty", stats.totalStockQuantity.toString(),
            Icons.Outlined.LocalShipping, route = Routes.PRODUCTS),
        StatCardData("Stock Value", stats.totalPurchaseValue.format(),
            Icons.Outlined.Receipt, route = Routes.PRODUCTS),
        StatCardData("Sales", stats.totalSales.format(),
            Icons.Outlined.ShoppingCart, route = Routes.SALES),
        StatCardData("Expenses", stats.totalExpenses.format(),
            Icons.Outlined.Receipt, route = Routes.EXPENSES),
        StatCardData("PR / Marketing", stats.totalPrCost.format(),
            Icons.Outlined.VolunteerActivism, route = Routes.PR),
        StatCardData("Gifts", stats.totalGiftCost.format(),
            Icons.Outlined.CardGiftcard, route = Routes.GIFTS),
        StatCardData("FOC", stats.totalFocCost.format(),
            Icons.Outlined.Redeem, route = Routes.FOC),
        StatCardData("Earned This Month", stats.currentMonthRevenue.format(),
            Icons.Outlined.ShoppingCart, deltaPercent = salesDelta,
            deltaGoodDirection = DeltaGoodDirection.UP, route = Routes.SALES),
        StatCardData("Spent This Month", stats.currentMonthExpenses.format(),
            Icons.Outlined.BarChart, deltaPercent = expenseDelta,
            deltaGoodDirection = DeltaGoodDirection.DOWN, route = Routes.EXPENSES)
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        cards.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { card ->
                    CompactStatCard(
                        data = card,
                        modifier = Modifier.weight(1f),
                        onClick = card.route?.let { r -> { onNavigate(r) } }
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CompactStatCard(
    data: StatCardData,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val cardModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else modifier

    Card(
        modifier = cardModifier.height(82.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                data.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    data.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    data.value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                val delta = data.deltaPercent
                if (delta != null) {
                    DeltaMini(delta, data.deltaGoodDirection)
                }
            }
        }
    }
}

@Composable
private fun DeltaMini(delta: Int, direction: DeltaGoodDirection) {
    val isGood = when (direction) {
        DeltaGoodDirection.UP -> delta >= 0
        DeltaGoodDirection.DOWN -> delta <= 0
    }
    val color = when {
        delta == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
        isGood -> SuccessGreen
        else -> WarningAmber
    }
    val arrow = when {
        delta > 0 -> "↑"
        delta < 0 -> "↓"
        else -> "→"
    }
    Text(
        text = "$arrow ${if (delta > 0) "+" else ""}${delta}%",
        style = MaterialTheme.typography.labelMedium,
        color = color,
        fontWeight = FontWeight.Medium
    )
}
