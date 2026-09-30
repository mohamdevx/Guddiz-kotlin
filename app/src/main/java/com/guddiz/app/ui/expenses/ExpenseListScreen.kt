package com.guddiz.app.ui.expenses

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
import com.guddiz.app.data.local.entity.ExpenseCategory
import com.guddiz.app.data.local.entity.ExpenseEntity
import com.guddiz.app.ui.AppViewModelProvider
import com.guddiz.app.ui.components.GuddizConfirmDialog
import com.guddiz.app.ui.components.GuddizEmptyState
import com.guddiz.app.ui.components.RowMenuButton
import com.guddiz.app.ui.components.RowMenuSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: ExpenseListViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val category by viewModel.category.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var menuItem by remember { mutableStateOf<ExpenseEntity?>(null) }
    var confirmDelete by remember { mutableStateOf<ExpenseEntity?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { e ->
            when (e) { is ExpenseListEvent.Message -> snackbar.showSnackbar(e.text) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expenses") },
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
                text = { Text("Add Expense") }
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
                    placeholder = { Text("Search description or vendor…") },
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
                        FilterChip(selected = category.isBlank(),
                            onClick = { viewModel.onCategoryChange("") },
                            label = { Text("All") })
                    }
                    items(ExpenseCategory.entries.toList()) { c ->
                        FilterChip(
                            selected = category == c.name,
                            onClick = { viewModel.onCategoryChange(if (category == c.name) "" else c.name) },
                            label = { Text(c.label) }
                        )
                    }
                }
            }

            if (expenses.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    GuddizEmptyState(
                        icon = Icons.Default.Add,
                        title = "No expenses yet",
                        message = "Tap “Add Expense” to record your first expense."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(expenses, key = { it.id }) { e ->
                        ExpenseRow(
                            expense = e,
                            onClick = { onOpen(e.id) },
                            onMenu = { menuItem = e }
                        )
                    }
                }
            }
        }
    }

    menuItem?.let { current ->
        RowMenuSheet(
            title = current.description,
            subtitle = Money(current.amount).format() + "  •  " + ExpenseCategory.fromStorage(current.category).label,
            onEdit = { onOpen(current.id) },
            onDuplicate = { viewModel.duplicate(current) },
            onDelete = { confirmDelete = current },
            onDismiss = { menuItem = null }
        )
    }

    confirmDelete?.let { target ->
        GuddizConfirmDialog(
            title = "Delete expense?",
            message = "This will move to Recently deleted. You can restore it for 30 days.",
            confirmText = "Delete",
            onConfirm = { confirmDelete = null; viewModel.softDelete(target) },
            onDismiss = { confirmDelete = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExpenseRow(
    expense: ExpenseEntity,
    onClick: () -> Unit,
    onMenu: () -> Unit
) {
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
                Text(expense.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text(ExpenseCategory.fromStorage(expense.category).label + "  •  " + DateUtils.format(expense.expenseDate),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                expense.vendor?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(Money(expense.amount).format(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error)
            RowMenuButton(onClick = onMenu)
        }
    }
}
