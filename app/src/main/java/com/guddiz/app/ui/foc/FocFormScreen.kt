package com.guddiz.app.ui.foc

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
fun FocFormScreen(
    onDone: () -> Unit, onBack: () -> Unit,
    viewModel: FocFormViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { e ->
            when (e) {
                is FocFormEvent.Saved -> { snackbar.showSnackbar("FOC saved"); onDone() }
                is FocFormEvent.Message -> snackbar.showSnackbar(e.text)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit FOC" else "New FOC") },
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
            GuddizSectionCard("Product") {
                SearchableProductPicker(
                    label = "Product",
                    products = state.products,
                    selectedProductId = state.productId,
                    onSelect = viewModel::onProductSelected,
                    error = state.errors["product"]
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GuddizTextField(state.quantity, viewModel::onQuantity,
                        "Quantity", keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                        error = state.errors["quantity"])
                    GuddizTextField(state.unitPrice, viewModel::onUnitPrice,
                        "Unit price (Rs)", keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                        error = state.errors["unitPrice"])
                }
                CostRow("Line value", state.lineCost.format(), emphasise = true)
            }

            GuddizSectionCard("Who received it?") {
                GuddizTextField(state.recipient, viewModel::onRecipient,
                    "Recipient *", error = state.errors["recipient"])
                GuddizTextField(state.campaign, viewModel::onCampaign,
                    "Campaign / reason (optional)")
                GuddizDateField("Date", state.date, viewModel::onDate)
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
