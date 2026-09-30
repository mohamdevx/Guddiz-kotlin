package com.guddiz.app.ui.products

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
import androidx.compose.material.icons.outlined.Inventory2
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guddiz.app.core.util.DateUtils
import com.guddiz.app.core.util.Haptics
import com.guddiz.app.data.local.entity.ProductCategory
import com.guddiz.app.data.local.entity.ProductEntity
import com.guddiz.app.ui.AppViewModelProvider
import com.guddiz.app.ui.components.GuddizConfirmDialog
import com.guddiz.app.ui.components.GuddizEmptyState
import com.guddiz.app.ui.components.RowMenuButton
import com.guddiz.app.ui.components.RowMenuSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    onAddProduct: () -> Unit,
    onOpenProduct: (Long) -> Unit,
    onEditProduct: (Long) -> Unit = onOpenProduct,
    filterMode: ProductFilterMode = ProductFilterMode.ALL,
    onBack: (() -> Unit)? = null,
    viewModel: ProductListViewModel = viewModel(factory = AppViewModelProvider.Factory),
    quickRecordViewModel: ProductQuickRecordViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    LaunchedEffect(filterMode) { viewModel.setFilterMode(filterMode) }

    val products by viewModel.products.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var menuProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var confirmDelete by remember { mutableStateOf<ProductEntity?>(null) }
    var quickProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var quickType by remember { mutableStateOf<QuickRecordType?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { e ->
            when (e) {
                is ProductListEvent.Message -> snackbar.showSnackbar(e.text)
            }
        }
    }

    LaunchedEffect(Unit) {
        quickRecordViewModel.events.collect { e ->
            when (e) {
                is QuickRecordEvent.Saved -> {
                    quickProduct = null
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
                title = { Text(filterMode.title) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddProduct,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Product") }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SearchAndFilter(
                query = searchQuery,
                onQueryChange = { viewModel.onSearchChange(it) },
                selectedCategory = selectedCategory,
                onCategoryChange = { viewModel.onCategoryChange(it) }
            )

            if (products.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (searchQuery.isBlank() && selectedCategory.isBlank()) {
                        GuddizEmptyState(
                            icon = Icons.Outlined.Inventory2,
                            title = when (filterMode) {
                                ProductFilterMode.ALL -> "No products yet"
                                ProductFilterMode.LOW_STOCK -> "Nothing low on stock"
                                ProductFilterMode.EXPIRING_SOON -> "Nothing expiring soon"
                            },
                            message = when (filterMode) {
                                ProductFilterMode.ALL -> "Tap “Add Product” to create your first product."
                                ProductFilterMode.LOW_STOCK -> "All products have enough stock."
                                ProductFilterMode.EXPIRING_SOON -> "No products are expiring soon."
                            }
                        )
                    } else {
                        GuddizEmptyState(
                            icon = Icons.Outlined.Search,
                            title = "Nothing found",
                            message = "Try a different name, SKU or category."
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductRow(
                            product = product,
                            onClick = { onOpenProduct(product.id) },
                            onMenu = { menuProduct = product }
                        )
                    }
                }
            }
        }
    }

    val currentMenu = menuProduct
    if (currentMenu != null) {
        RowMenuSheet(
            title = currentMenu.name,
            subtitle = ProductCategory.fromStorage(currentMenu.category).label,
            onQuickSale = {
                quickProduct = currentMenu
                quickType = QuickRecordType.SALE
                menuProduct = null
            },
            onQuickPurchase = {
                quickProduct = currentMenu
                quickType = QuickRecordType.PURCHASE
                menuProduct = null
            },
            onEdit = { onEditProduct(currentMenu.id) },
            onDuplicate = { viewModel.duplicate(currentMenu) },
            onDelete = { confirmDelete = currentMenu },
            onDismiss = { menuProduct = null }
        )
    }

    if (confirmDelete != null) {
        val target = confirmDelete!!
        GuddizConfirmDialog(
            title = "Delete product?",
            message = "“${target.name}” will move to Recently deleted. You can restore it for 30 days.",
            confirmText = "Delete",
            onConfirm = {
                Haptics.destructive(context)
                confirmDelete = null
                viewModel.softDelete(target)
            },
            onDismiss = { confirmDelete = null }
        )
    }

    val qp = quickProduct
    val qt = quickType
    if (qp != null && qt != null) {
        ProductQuickRecordSheet(
            type = qt,
            product = qp,
            onDismiss = { quickProduct = null; quickType = null },
            onSubmit = { qty, price ->
                when (qt) {
                    QuickRecordType.SALE -> quickRecordViewModel.recordSale(qp, qty, price)
                    QuickRecordType.PURCHASE -> quickRecordViewModel.recordPurchase(qp, qty, price)
                }
            }
        )
    }
}

@Composable
private fun SearchAndFilter(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search name, SKU, supplier…") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Outlined.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedCategory.isBlank(),
                    onClick = { onCategoryChange("") },
                    label = { Text("All") }
                )
            }
            items(ProductCategory.entries.toList()) { category ->
                FilterChip(
                    selected = selectedCategory == category.name,
                    onClick = {
                        onCategoryChange(
                            if (selectedCategory == category.name) "" else category.name
                        )
                    },
                    label = { Text(category.label) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProductRow(
    product: ProductEntity,
    onClick: () -> Unit,
    onMenu: () -> Unit
) {
    val category = ProductCategory.fromStorage(product.category)
    val lowStock = product.minStockLevel > 0 &&
        product.currentQuantity <= product.minStockLevel
    val expired = DateUtils.isExpired(product.expiryDate)
    val expiringSoon = DateUtils.isExpiringSoon(product.expiryDate)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onMenu),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = category.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val brand = product.brand
                    if (brand != null && brand.isNotBlank()) {
                        Text(
                            text = brand,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                when {
                    expired -> Text(
                        text = "Expired",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    expiringSoon -> Text(
                        text = "Expiring soon",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    lowStock -> Text(
                        text = "Low stock",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Text(
                text = "Qty " + product.currentQuantity,
                style = MaterialTheme.typography.labelLarge,
                color = if (lowStock) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(4.dp))
            RowMenuButton(onClick = onMenu)
        }
    }
}
