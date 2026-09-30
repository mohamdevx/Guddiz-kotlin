package com.guddiz.app.ui.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guddiz.app.core.util.DateUtils
import com.guddiz.app.data.local.entity.StockMovementType
import com.guddiz.app.ui.AppViewModelProvider
import com.guddiz.app.ui.components.GuddizSectionCard

@Composable
fun StockMovementSection(
    productId: Long,
    modifier: Modifier = Modifier,
    viewModel: StockMovementViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val movements by viewModel.observeForProduct(productId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    if (movements.isEmpty()) return

    GuddizSectionCard("Stock history", modifier) {
        movements.take(20).forEach { m ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = StockMovementType.fromStorage(m.type).label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = DateUtils.format(m.date),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = (if (m.delta >= 0) "+" else "") + m.delta,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (m.delta >= 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
