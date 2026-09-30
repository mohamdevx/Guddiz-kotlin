package com.guddiz.app.ui.pr

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.guddiz.app.data.local.entity.PrPackageEntity
import com.guddiz.app.data.local.entity.PrStatus
import com.guddiz.app.ui.AppViewModelProvider
import com.guddiz.app.ui.components.GuddizConfirmDialog
import com.guddiz.app.ui.components.GuddizEmptyState
import com.guddiz.app.ui.components.RowMenuButton
import com.guddiz.app.ui.components.RowMenuSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrListScreen(
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: PrListViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val packages by viewModel.packages.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var menuItem by remember { mutableStateOf<PrPackageEntity?>(null) }
    var confirmDelete by remember { mutableStateOf<PrPackageEntity?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { e ->
            when (e) { is PrListEvent.Message -> snackbar.showSnackbar(e.text) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PR / Marketing") },
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
                text = { Text("New PR Package") }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.onQueryChange(it) },
                placeholder = { Text("Search recipient or campaign…") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(Icons.Outlined.Clear, contentDescription = "Clear")
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )

            if (packages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    GuddizEmptyState(
                        icon = Icons.Default.Add,
                        title = "No PR packages yet",
                        message = "Tap “New PR Package” to send your first one."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(packages, key = { it.id }) { p ->
                        PrRow(p, onClick = { onOpen(p.id) }, onMenu = { menuItem = p })
                    }
                }
            }
        }
    }

    menuItem?.let { current ->
        RowMenuSheet(
            title = current.recipientName,
            subtitle = Money(current.totalCost).format() + "  •  " + PrStatus.fromStorage(current.status).label,
            onEdit = { onOpen(current.id) },
            onDuplicate = { viewModel.duplicate(current) },
            onDelete = { confirmDelete = current },
            onDismiss = { menuItem = null }
        )
    }

    confirmDelete?.let { target ->
        GuddizConfirmDialog(
            title = "Delete PR package?",
            message = "This will move to Recently deleted and restore the product stock. You can restore it for 30 days.",
            confirmText = "Delete",
            onConfirm = { confirmDelete = null; viewModel.softDelete(target) },
            onDismiss = { confirmDelete = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PrRow(p: PrPackageEntity, onClick: () -> Unit, onMenu: () -> Unit) {
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
                Text(p.recipientName, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text(PrStatus.fromStorage(p.status).label + "  •  " + DateUtils.format(p.date),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                p.campaignName?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(Money(p.totalCost).format(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary)
            RowMenuButton(onClick = onMenu)
        }
    }
}
