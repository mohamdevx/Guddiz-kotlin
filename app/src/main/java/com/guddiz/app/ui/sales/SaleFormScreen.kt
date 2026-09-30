package com.guddiz.app.ui.sales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guddiz.app.data.local.entity.SaleChannel
import com.guddiz.app.ui.AppViewModelProvider
import com.guddiz.app.ui.components.GuddizDateField
import com.guddiz.app.ui.components.GuddizDropdown
import com.guddiz.app.ui.components.GuddizSectionCard
import com.guddiz.app.ui.components.GuddizTextField
import com.guddiz.app.ui.components.LineItemEditor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleFormScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: SaleFormViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { e ->
            when (e) {
                is SaleFormEvent.Saved -> { snackbar.showSnackbar("Sale saved"); onDone() }
                is SaleFormEvent.Message -> snackbar.showSnackbar(e.text)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit Sale" else "New Sale") },
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
        snackbarHost = { SnackbarHost(snackbar) }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            Modifier.fillMaxSize().padding(innerPadding)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GuddizSectionCard("Sale details") {
                GuddizDateField("Date", state.saleDate, viewModel::onDateChange)
                GuddizDropdown(
                    label = "Channel",
                    options = SaleChannel.entries.toList(),
                    selected = state.channel,
                    optionLabel = { it.label },
                    onSelect = viewModel::onChannelChange
                )
                GuddizTextField(
                    state.customer,
                    viewModel::onCustomerChange,
                    "Customer name (optional)"
                )
            }

            GuddizSectionCard("Products sold") {
                state.errors["lines"]?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium)
                }
                LineItemEditor(
                    lines = state.lines,
                    products = state.products,
                    onLineChange = viewModel::onLineChange,
                    onAddLine = viewModel::onAddLine,
                    onRemoveLine = viewModel::onRemoveLine
                )
            }

            GuddizSectionCard("Summary") {
                SaleRow2("Revenue", state.totalRevenue.format(), emphasise = true)
                SaleRow2("Cost of goods", state.totalCost.format())
                HorizontalDivider()
                SaleRow2("Profit", state.profit.format(), emphasise = true, positive = !state.profit.isNegative)
            }

            GuddizSectionCard("Notes") {
                GuddizTextField(
                    state.notes,
                    viewModel::onNotesChange,
                    "Notes (optional)",
                    singleLine = false
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(56.dp)) {
                    Text("Cancel")
                }
                Button(
                    onClick = viewModel::save,
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Text(if (state.isSaving) "Saving…" else "Save")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SaleRow2(
    label: String,
    value: String,
    emphasise: Boolean = false,
    positive: Boolean = true
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasise) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasise) FontWeight.Bold else FontWeight.Medium,
            color = when {
                emphasise && positive -> MaterialTheme.colorScheme.primary
                emphasise && !positive -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
