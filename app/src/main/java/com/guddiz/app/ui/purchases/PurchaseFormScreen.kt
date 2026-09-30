package com.guddiz.app.ui.purchases

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guddiz.app.ui.AppViewModelProvider
import com.guddiz.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseFormScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: PurchaseFormViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { e ->
            when (e) {
                is PurchaseFormEvent.Saved -> { snackbar.showSnackbar("Purchase saved"); onDone() }
                is PurchaseFormEvent.Message -> snackbar.showSnackbar(e.text)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit Purchase" else "New Purchase") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
            GuddizSectionCard("What did you buy?") {
                SearchableProductPicker(
                    label = "Product",
                    products = state.products,
                    selectedProductId = state.productId,
                    onSelect = viewModel::onProductSelected,
                    error = state.errors["product"]
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GuddizTextField(
                        value = state.quantity,
                        onValueChange = viewModel::onQuantityChange,
                        label = "Quantity",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                        error = state.errors["quantity"]
                    )
                    GuddizTextField(
                        value = state.unitPrice,
                        onValueChange = viewModel::onUnitPriceChange,
                        label = "Unit price (Rs)",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                        error = state.errors["unitPrice"]
                    )
                }
                GuddizTextField(state.supplier, viewModel::onSupplierChange, "Supplier (optional)")
                GuddizDateField("Purchase date", state.purchaseDate, viewModel::onDateChange)
            }

            GuddizSectionCard("Extra costs (optional)") {
                GuddizTextField(state.packagingCost, viewModel::onPackagingChange,
                    "Packaging cost (Rs)", keyboardType = KeyboardType.Decimal,
                    error = state.errors["packagingCost"])
                GuddizTextField(state.shippingCost, viewModel::onShippingChange,
                    "Shipping / courier (Rs)", keyboardType = KeyboardType.Decimal,
                    error = state.errors["shippingCost"])
                GuddizTextField(state.otherCost, viewModel::onOtherChange,
                    "Other cost (Rs)", keyboardType = KeyboardType.Decimal,
                    error = state.errors["otherCost"])
            }

            GuddizSectionCard("Total cost") {
                CostRow("Product cost", state.cost.productCost.format())
                CostRow("Packaging", state.cost.packagingCost.format())
                CostRow("Shipping", state.cost.shippingCost.format())
                CostRow("Other", state.cost.otherCost.format())
                HorizontalDivider()
                CostRow("Total", state.cost.totalCost.format(), emphasise = true)
            }

            GuddizSectionCard("Receipt") {
                ReceiptPicker(state.receiptPath, viewModel::onReceiptChange)
            }

            GuddizSectionCard("Notes") {
                GuddizTextField(state.notes, viewModel::onNotesChange,
                    "Notes (optional)", singleLine = false)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(56.dp)) {
                    Text("Cancel")
                }
                Button(onClick = viewModel::save, enabled = !state.isSaving,
                    modifier = Modifier.weight(1f).height(56.dp)) {
                    Text(if (state.isSaving) "Saving…" else "Save")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CostRow(label: String, value: String, emphasise: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasise) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasise) FontWeight.Bold else FontWeight.Medium,
            color = if (emphasise) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface)
    }
}
