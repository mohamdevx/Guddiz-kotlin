package com.guddiz.app.ui.sales

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guddiz.app.core.money.Money
import com.guddiz.app.core.util.DateUtils
import com.guddiz.app.data.local.entity.SaleChannel
import com.guddiz.app.data.local.entity.SaleEntity
import com.guddiz.app.ui.AppViewModelProvider
import com.guddiz.app.ui.components.GuddizConfirmDialog
import com.guddiz.app.ui.components.GuddizEmptyState
import com.guddiz.app.ui.components.RowMenuButton
import com.guddiz.app.ui.components.RowMenuSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleListScreen(
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: SaleListViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val sales by viewModel.sales.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val channel by viewModel.channel.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var menuItem by remember { mutableStateOf<SaleEntity?>(null) }
    var confirmDelete by remember { mutableStateOf<SaleEntity?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { e ->
            when (e) { is SaleListEvent.Message -> snackbar.showSnackbar(e.text) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sales") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Sale") }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.onQueryChange(it) },
                    placeholder = { Text("Search customer or product…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Outlined.Clear, contentDescription = "Clear")
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(selected = channel.isBlank(),
                            onClick = { viewModel.onChannelChange("") },
                            label = { Text("All") })
                    }
                    items(SaleChannel.entries.toList()) { c ->
                        FilterChip(
                            selected = channel == c.name,
                            onClick = { viewModel.onChannelChange(if (channel == c.name) "" else c.name) },
                            label = { Text(c.label) }
                        )
                    }
                }
            }

            if (sales.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    GuddizEmptyState(
                        icon = Icons.Default.Add,
                        title = "No sales yet",
                        message = "Tap “Add Sale” or use the Sold button on a product."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sales, key = { it.id }) { s ->
                        SaleRow(s, onClick = { onOpen(s.id) }, onMenu = { menuItem = s })
                    }
                }
            }
        }
    }

    menuItem?.let { current ->
        RowMenuSheet(
            title = current.customer?.takeIf { it.isNotBlank() }
                ?: current.itemSummary.takeIf { it.isNotBlank() }
                ?: "Walk-in sale",
            subtitle = Money(current.totalRevenue).format()
                + "  •  " + SaleChannel.fromStorage(current.channel).label,
            onEdit = { onOpen(current.id) },
            onDuplicate = { viewModel.duplicate(current) },
            onDelete = { confirmDelete = current },
            onDismiss = { menuItem = null }
        )
    }

    confirmDelete?.let { target ->
        GuddizConfirmDialog(
            title = "Delete sale?",
            message = "This will move to Recently deleted and restore the product stock. You can restore it for 30 days.",
            confirmText = "Delete",
            onConfirm = { confirmDelete = null; viewModel.softDelete(target) },
            onDismiss = { confirmDelete = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SaleRow(s: SaleEntity, onClick: () -> Unit, onMenu: () -> Unit) {
    val customerName = s.customer?.takeIf { it.isNotBlank() }
    val summary = s.itemSummary.takeIf { it.isNotBlank() } ?: "Sale recorded"
    val channelLabel = SaleChannel.fromStorage(s.channel).label

    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onMenu),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Title: customer if we have one, otherwise the products sold
                Text(
                    text = customerName ?: summary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )

                // If we showed the customer, also show what was sold
                if (customerName != null) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )
                }

                val details = buildString {
                    append(channelLabel)
                    if (s.itemCount > 0) {
                        append("  •  ")
                        append(s.itemCount)
                        append(if (s.itemCount == 1) " item" else " items")
                    }
                    append("  •  ")
                    append(DateUtils.format(s.saleDate))
                }
                Text(
                    text = details,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = Money(s.totalRevenue).format(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            RowMenuButton(onClick = onMenu)
        }
    }
}
