package com.guddiz.app.ui.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guddiz.app.core.util.Haptics
import com.guddiz.app.data.local.entity.ProductCategory
import com.guddiz.app.ui.AppViewModelProvider
import com.guddiz.app.ui.components.GuddizDropdown
import com.guddiz.app.ui.components.GuddizSectionCard
import com.guddiz.app.ui.components.GuddizTextField
import com.guddiz.app.ui.components.ReceiptPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProductFormViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProductFormEvent.Saved -> {
                    Haptics.success(context)
                    snackbarHostState.showSnackbar("Product saved")
                    onDone()
                }
                is ProductFormEvent.Message -> {
                    Haptics.error(context)
                    snackbarHostState.showSnackbar(event.text)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit Product" else "New Product") },
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GuddizSectionCard("Product photo") {
                ReceiptPicker(
                    currentPath = state.photoPath,
                    onPathChanged = viewModel::onPhotoChange,
                    heading = "Photo",
                    helperText = "Take a photo or pick one from your gallery."
                )
            }

            GuddizSectionCard("Basic information") {
                GuddizTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = "Product name *",
                    error = state.errors["name"]
                )
                GuddizDropdown(
                    label = "Category",
                    options = ProductCategory.entries.toList(),
                    selected = state.category,
                    optionLabel = { it.label },
                    onSelect = viewModel::onCategoryChange
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GuddizTextField(
                        value = state.sku,
                        onValueChange = viewModel::onSkuChange,
                        label = "SKU (optional)",
                        modifier = Modifier.weight(1f),
                        error = state.errors["sku"]
                    )
                    GuddizTextField(
                        value = state.productCode,
                        onValueChange = viewModel::onProductCodeChange,
                        label = "Code (optional)",
                        modifier = Modifier.weight(1f)
                    )
                }
                GuddizTextField(
                    value = state.brand,
                    onValueChange = viewModel::onBrandChange,
                    label = "Brand (optional)"
                )
                GuddizTextField(
                    value = state.supplier,
                    onValueChange = viewModel::onSupplierChange,
                    label = "Supplier (optional)"
                )
            }

            GuddizSectionCard("Pricing") {
                GuddizTextField(
                    value = state.purchasePrice,
                    onValueChange = viewModel::onPurchasePriceChange,
                    label = "Purchase price (Rs)",
                    keyboardType = KeyboardType.Decimal,
                    error = state.errors["purchasePrice"]
                )
                GuddizTextField(
                    value = state.prs,
                    onValueChange = viewModel::onPrsChange,
                    label = "PRS / retail price (Rs)",
                    keyboardType = KeyboardType.Decimal,
                    error = state.errors["prs"]
                )
                GuddizTextField(
                    value = state.sellingPrice,
                    onValueChange = viewModel::onSellingPriceChange,
                    label = "Selling price (Rs)",
                    keyboardType = KeyboardType.Decimal,
                    error = state.errors["sellingPrice"]
                )
            }

            GuddizSectionCard("Stock") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GuddizTextField(
                        value = state.currentQuantity,
                        onValueChange = viewModel::onQuantityChange,
                        label = "Current quantity",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    GuddizTextField(
                        value = state.minStockLevel,
                        onValueChange = viewModel::onMinStockChange,
                        label = "Min stock level",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                }
                GuddizTextField(
                    value = state.batchNumber,
                    onValueChange = viewModel::onBatchChange,
                    label = "Batch number (optional)"
                )
            }

            GuddizSectionCard("Notes") {
                GuddizTextField(
                    value = state.notes,
                    onValueChange = viewModel::onNotesChange,
                    label = "Notes (optional)",
                    singleLine = false,
                    imeAction = ImeAction.Default
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f).height(56.dp)
                ) { Text("Cancel") }
                Button(
                    onClick = viewModel::save,
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (state.isSaving) "Saving…" else "Save")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
