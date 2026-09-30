package com.guddiz.app.ui.expenses

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
import com.guddiz.app.data.local.entity.ExpenseCategory
import com.guddiz.app.ui.AppViewModelProvider
import com.guddiz.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: ExpenseFormViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { e ->
            when (e) {
                is ExpenseFormEvent.Saved -> { snackbar.showSnackbar("Expense saved"); onDone() }
                is ExpenseFormEvent.Message -> snackbar.showSnackbar(e.text)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit Expense" else "New Expense") },
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
            GuddizSectionCard("Expense details") {
                GuddizDropdown(
                    label = "Category",
                    options = ExpenseCategory.entries.toList(),
                    selected = state.category,
                    optionLabel = { it.label },
                    onSelect = viewModel::onCategoryChange
                )
                GuddizTextField(state.amount, viewModel::onAmountChange,
                    "Amount (Rs)", keyboardType = KeyboardType.Decimal,
                    error = state.errors["amount"])
                GuddizDateField("Date", state.expenseDate, viewModel::onDateChange)
                GuddizTextField(state.vendor, viewModel::onVendorChange, "Vendor / person (optional)")
                GuddizTextField(state.description, viewModel::onDescriptionChange,
                    "Description *", error = state.errors["description"])
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
