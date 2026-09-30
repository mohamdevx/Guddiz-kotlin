package com.guddiz.app.ui.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guddiz.app.core.money.Money
import com.guddiz.app.core.util.DateUtils
import com.guddiz.app.data.model.RecordType
import com.guddiz.app.data.model.UnifiedRecord
import com.guddiz.app.ui.AppViewModelProvider
import com.guddiz.app.ui.components.GuddizEmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    onOpenSale: (Long) -> Unit,
    onOpenPurchase: (Long) -> Unit,
    onOpenExpense: (Long) -> Unit,
    onOpenPr: (Long) -> Unit,
    onOpenGift: (Long) -> Unit,
    onOpenFoc: (Long) -> Unit,
    viewModel: RecordsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val activeTypes by viewModel.activeTypes.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Records") },
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
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.onQueryChange(it) },
                placeholder = { Text("Search products, people, campaigns…") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Outlined.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = activeTypes.size == RecordType.all.size,
                        onClick = { viewModel.selectAllTypes() },
                        label = { Text("All") }
                    )
                }
                items(RecordType.entries.toList()) { type ->
                    FilterChip(
                        selected = activeTypes.size < RecordType.all.size && type in activeTypes,
                        onClick = { viewModel.toggleType(type) },
                        label = { Text(type.pluralLabel) }
                    )
                }
            }

            Spacer(Modifier.padding(top = 4.dp))

            when {
                isLoading && records.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                records.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (query.isBlank()) {
                            GuddizEmptyState(
                                icon = Icons.Outlined.Receipt,
                                title = "No records yet",
                                message = "As you add sales, purchases, expenses and giveaways, they'll appear here."
                            )
                        } else {
                            GuddizEmptyState(
                                icon = Icons.Outlined.Search,
                                title = "Nothing found",
                                message = "Try a different name, supplier, or campaign."
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(records, key = { r -> r.type.name + "_" + r.id }) { r ->
                            RecordRow(
                                record = r,
                                onClick = {
                                    when (r.type) {
                                        RecordType.SALE -> onOpenSale(r.id)
                                        RecordType.PURCHASE -> onOpenPurchase(r.id)
                                        RecordType.EXPENSE -> onOpenExpense(r.id)
                                        RecordType.PR -> onOpenPr(r.id)
                                        RecordType.GIFT -> onOpenGift(r.id)
                                        RecordType.FOC -> onOpenFoc(r.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordRow(record: UnifiedRecord, onClick: () -> Unit) {
    val icon = iconFor(record.type)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = record.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = record.subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = record.type.label + "  •  " + DateUtils.format(record.date),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = Money(record.amountPaisa).format(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun iconFor(type: RecordType): ImageVector = when (type) {
    RecordType.SALE -> Icons.Outlined.ShoppingCart
    RecordType.PURCHASE -> Icons.Outlined.Inventory2
    RecordType.EXPENSE -> Icons.Outlined.Receipt
    RecordType.PR -> Icons.Outlined.VolunteerActivism
    RecordType.GIFT -> Icons.Outlined.CardGiftcard
    RecordType.FOC -> Icons.Outlined.Redeem
}
