package com.guddiz.app.ui.gifts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guddiz.app.ui.AppViewModelProvider
import com.guddiz.app.ui.components.*
import com.guddiz.app.ui.pr.CostRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiftFormScreen(
    onDone: () -> Unit, onBack: () -> Unit,
    viewModel: GiftFormViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { e ->
            when (e) {
                is GiftFormEvent.Saved -> { snackbar.showSnackbar("Gift saved"); onDone() }
                is GiftFormEvent.Message -> snackbar.showSnackbar(e.text)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit Gift" else "New Gift") },
                navigationIcon = { IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            Modifier.fillMaxSize().padding(innerPadding)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GuddizSectionCard("Who is it for?") {
                GuddizTextField(state.recipient, viewModel::onRecipient,
                    "Recipient *", error = state.errors["recipient"])
                GuddizTextField(state.occasion, viewModel::onOccasion,
                    "Occasion / reason *", error = state.errors["occasion"])
                GuddizDateField("Date", state.date, viewModel::onDate)
            }

            GuddizSectionCard("Products") {
                state.errors["lines"]?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium)
                }
                LineItemEditor(
                    lines = state.lines, products = state.products,
                    onLineChange = viewModel::onLineChange,
                    onAddLine = viewModel::onAddLine,
                    onRemoveLine = viewModel::onRemoveLine
                )
            }

            GuddizSectionCard("Extra costs (optional)") {
                GuddizTextField(state.packagingCost, viewModel::onPackaging,
                    "Packaging cost (Rs)", keyboardType = KeyboardType.Decimal)
                GuddizTextField(state.courierCost, viewModel::onCourier,
                    "Courier cost (Rs)", keyboardType = KeyboardType.Decimal)
                GuddizTextField(state.otherCost, viewModel::onOther,
                    "Other cost (Rs)", keyboardType = KeyboardType.Decimal)
            }

            GuddizSectionCard("Total cost") {
                CostRow("Product cost", state.cost.productCost.format())
                CostRow("Packaging", state.cost.packagingCost.format())
                CostRow("Courier", state.cost.courierCost.format())
                CostRow("Other", state.cost.otherCost.format())
                HorizontalDivider()
                CostRow("Total", state.cost.totalCost.format(), emphasise = true)
            }

            GuddizSectionCard("Receipt") {
                ReceiptPicker(state.receiptPath, viewModel::onReceipt)
            }

            GuddizSectionCard("Notes") {
                GuddizTextField(state.notes, viewModel::onNotes,
                    "Notes (optional)", singleLine = false)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(56.dp)) {
                    Text("Cancel") }
                Button(onClick = viewModel::save, enabled = !state.isSaving,
                    modifier = Modifier.weight(1f).height(56.dp)) {
                    Text(if (state.isSaving) "Saving…" else "Save") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
