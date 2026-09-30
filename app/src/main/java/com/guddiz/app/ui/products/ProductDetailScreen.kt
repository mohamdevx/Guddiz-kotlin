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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guddiz.app.core.money.Money
import com.guddiz.app.core.util.DateUtils
import com.guddiz.app.core.util.Haptics
import com.guddiz.app.data.local.entity.ProductCategory
import com.guddiz.app.data.local.entity.ProductEntity
import com.guddiz.app.ui.AppViewModelProvider
import com.guddiz.app.ui.components.GuddizConfirmDialog
import com.guddiz.app.ui.components.GuddizEmptyState
import com.guddiz.app.ui.components.GuddizSectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: Long,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProductDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
    adjustViewModel: StockAdjustmentViewModel = viewModel(factory = AppViewModelProvider.Factory),
    quickRecordViewModel: ProductQuickRecordViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val product by viewModel.product.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAdjustSheet by remember { mutableStateOf(false) }
    var quickType by remember { mutableStateOf<QuickRecordType?>(null) }

    LaunchedEffect(Unit) {
        adjustViewModel.events.collect { e ->
            when (e) {
                is StockAdjustmentEvent.Saved -> {
                    showAdjustSheet = false
                    Haptics.success(context)
                    snackbar.showSnackbar("Stock updated")
                }
                is StockAdjustmentEvent.Message -> {
                    Haptics.error(context)
                    snackbar.showSnackbar(e.text)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        quickRecordViewModel.events.collect { e ->
            when (e) {
                is QuickRecordEvent.Saved -> {
                    quickType = null
                    Haptics.success(context)
                    snackbar.showSnackbar("Saved")
                }
                is QuickRecordEvent.Message -> {
                    Haptics.error(context)
                    snackbar.showSnackbar(e.text)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(product?.name ?: "Product") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (product != null) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { innerPadding ->
        val p = product
        if (p == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                GuddizEmptyState(
                    icon = Icons.Default.Delete,
                    title = "Product not found",
                    message = "This product may have been deleted."
                )
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
            GuddizSectionCard("Overview") {
                Text(
                    p.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Qty ${p.currentQuantity} in stock",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ---------- Quick actions (this is the star of the show) ----------
            GuddizSectionCard("Quick actions") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { quickType = QuickRecordType.SALE },
                        modifier = Modifier.weight(1f).height(64.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Outlined.ShoppingCart, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Sold")
                    }
                    Button(
                        onClick = { quickType = QuickRecordType.PURCHASE },
                        modifier = Modifier.weight(1f).height(64.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Icon(Icons.Outlined.AddShoppingCart, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Bought")
                    }
                }
                OutlinedButton(
                    onClick = { showAdjustSheet = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Outlined.Tune, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Adjust stock")
                }
            }

            GuddizSectionCard("Details") {
                DetailRow("Category", ProductCategory.fromStorage(p.category).label)
                DetailRow("SKU", p.sku ?: "—")
                DetailRow("Code", p.productCode ?: "—")
                DetailRow("Brand", p.brand ?: "—")
                DetailRow("Supplier", p.supplier ?: "—")
                DetailRow("Batch", p.batchNumber ?: "—")
                DetailRow("Expiry", DateUtils.format(p.expiryDate))
            }

            GuddizSectionCard("Pricing") {
                DetailRow("Purchase price", Money(p.purchasePrice).format())
                DetailRow("PRS / retail", Money(p.prs).format())
                DetailRow("Selling price", Money(p.sellingPrice).format())
                val profit = Money(p.sellingPrice) - Money(p.purchasePrice)
                DetailRow(
                    "Profit per unit",
                    profit.format(),
                    valueColor = if (profit.isNegative) MaterialTheme.colorScheme.error
                                 else MaterialTheme.colorScheme.primary
                )
            }

            GuddizSectionCard("Stock") {
                DetailRow("Current quantity", p.currentQuantity.toString())
                DetailRow("Minimum level", p.minStockLevel.toString())
            }

            if (!p.notes.isNullOrBlank()) {
                GuddizSectionCard("Notes") {
                    Text(p.notes!!, style = MaterialTheme.typography.bodyMedium)
                }
            }

            StockMovementSection(productId = p.id)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Delete")
                }
                Button(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Edit")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showAdjustSheet && product != null) {
        StockAdjustmentSheet(
            product = product!!,
            onDismiss = { showAdjustSheet = false },
            onSubmit = { type, units, note ->
                adjustViewModel.submit(type, units, note)
            }
        )
    }

    val currentProduct = product
    val currentQuick = quickType
    if (currentProduct != null && currentQuick != null) {
        ProductQuickRecordSheet(
            type = currentQuick,
            product = currentProduct,
            onDismiss = { quickType = null },
            onSubmit = { qty, price ->
                when (currentQuick) {
                    QuickRecordType.SALE ->
                        quickRecordViewModel.recordSale(currentProduct, qty, price)
                    QuickRecordType.PURCHASE ->
                        quickRecordViewModel.recordPurchase(currentProduct, qty, price)
                }
            }
        )
    }

    if (showDeleteDialog) {
        GuddizConfirmDialog(
            title = "Delete product?",
            message = "This will permanently remove the product. This cannot be undone.",
            confirmText = "Delete",
            onConfirm = {
                Haptics.destructive(context)
                showDeleteDialog = false
                viewModel.delete(onDeleted = onBack)
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium, color = valueColor)
    }
}
