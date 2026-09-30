package com.guddiz.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.guddiz.app.ui.backup.BackupScreen
import com.guddiz.app.ui.dashboard.DashboardScreen
import com.guddiz.app.ui.expenses.ExpenseFormScreen
import com.guddiz.app.ui.expenses.ExpenseListScreen
import com.guddiz.app.ui.foc.FocFormScreen
import com.guddiz.app.ui.foc.FocListScreen
import com.guddiz.app.ui.gifts.GiftFormScreen
import com.guddiz.app.ui.gifts.GiftListScreen
import com.guddiz.app.ui.navigation.Routes
import com.guddiz.app.ui.navigation.TopLevelDestination
import com.guddiz.app.ui.pr.PrFormScreen
import com.guddiz.app.ui.pr.PrListScreen
import com.guddiz.app.ui.products.ProductDetailScreen
import com.guddiz.app.ui.products.ProductFilterMode
import com.guddiz.app.ui.products.ProductFormScreen
import com.guddiz.app.ui.products.ProductListScreen
import com.guddiz.app.ui.purchases.PurchaseFormScreen
import com.guddiz.app.ui.purchases.PurchaseListScreen
import com.guddiz.app.ui.records.RecordsScreen
import com.guddiz.app.ui.recyclebin.RecycleBinScreen
import com.guddiz.app.ui.reports.ReportsScreen
import com.guddiz.app.ui.sales.SaleFormScreen
import com.guddiz.app.ui.sales.SaleListScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun GuddizApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val showBottomBar = TopLevelDestination.entries.any { d ->
        currentDestination?.hierarchy?.any { it.route == d.route } == true
    }

    // ---------- Scroll-to-hide bottom navigation ----------
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    // Measured dynamically — will be updated by onSizeChanged below.
    // Default to 90dp as a safe starting point until the first layout.
    val barHeightState = remember { mutableStateOf(90.dp) }
    val barHeightPx = remember(barHeightState.value, density) {
        with(density) { barHeightState.value.toPx() }
    }
    val thresholdPx = remember(density) { with(density) { 12.dp.toPx() } }

    val isHidden = remember { mutableStateOf(false) }  // always false now
    val barOffset = remember { Animatable(0f) }

    // Accumulators per direction — reset when direction changes
    var downAccum = 0f
    var upAccum = 0f

    val showBottomBarState = rememberUpdatedState(showBottomBar)

    // Auto-show after 2 seconds of no scrolling while hidden (Instagram-style fallback)
    LaunchedEffect(isHidden.value) {
        if (isHidden.value && showBottomBarState.value) {
            delay(1500L)
            isHidden.value = false
        }
    }

    // Scroll-to-hide DISABLED. The bar is always visible.
    LaunchedEffect(isHidden.value) {
        // no-op
    }

    // Reset to visible on every navigation
    LaunchedEffect(currentDestination?.route) {
        isHidden.value = false
        barOffset.snapTo(0f)
        downAccum = 0f
        upAccum = 0f
    }

    // Scroll-to-hide disabled.
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset = Offset.Zero
        }
    }

    // Dynamic bottom padding so lists expand into the freed space
    val bottomPaddingDp = with(density) { barHeightPx.toDp() }

    Box(modifier = Modifier.fillMaxSize()) {

        Scaffold(
            modifier = Modifier.nestedScroll(nestedScrollConnection)
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.DASHBOARD,
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(bottom = bottomPaddingDp)
            ) {
                composable(Routes.DASHBOARD) {
                    DashboardScreen(onNavigate = { route ->
                        if (TopLevelDestination.entries.any { it.route == route }) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.id) {
                                    inclusive = false
                                    saveState = false
                                }
                                launchSingleTop = true
                                restoreState = false
                            }
                        } else {
                            navController.navigate(route)
                        }
                    })
                }
                composable(Routes.PRODUCTS) {
                    ProductListScreen(
                        filterMode = ProductFilterMode.ALL,
                        onAddProduct = { navController.navigate(Routes.productForm(0L)) },
                        onOpenProduct = { id -> navController.navigate(Routes.productDetail(id)) },
                        onEditProduct = { id -> navController.navigate(Routes.productForm(id)) }
                    )
                }
                composable(Routes.PRODUCTS_LOW_STOCK) {
                    ProductListScreen(
                        filterMode = ProductFilterMode.LOW_STOCK,
                        onAddProduct = { navController.navigate(Routes.productForm(0L)) },
                        onOpenProduct = { id -> navController.navigate(Routes.productDetail(id)) },
                        onEditProduct = { id -> navController.navigate(Routes.productForm(id)) },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Routes.PRODUCTS_EXPIRING) {
                    ProductListScreen(
                        filterMode = ProductFilterMode.EXPIRING_SOON,
                        onAddProduct = { navController.navigate(Routes.productForm(0L)) },
                        onOpenProduct = { id -> navController.navigate(Routes.productDetail(id)) },
                        onEditProduct = { id -> navController.navigate(Routes.productForm(id)) },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Routes.PRODUCT_FORM) {
                    ProductFormScreen(
                        onDone = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Routes.PRODUCT_DETAIL) {
                    ProductDetailScreen(
                        productId = 0L,
                        onEdit = {
                            val id = navController.currentBackStackEntry
                                ?.arguments?.getString(Routes.ARG_PRODUCT_ID)?.toLongOrNull() ?: 0L
                            navController.navigate(Routes.productForm(id))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Routes.SALES) {
                    SaleListScreen(
                        onAdd = { navController.navigate(Routes.saleForm(0L)) },
                        onOpen = { id -> navController.navigate(Routes.saleForm(id)) },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Routes.SALE_FORM) {
                    SaleFormScreen(
                        onDone = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Routes.PURCHASES) {
                    PurchaseListScreen(
                        onAdd = { navController.navigate(Routes.purchaseForm(0L)) },
                        onOpen = { id -> navController.navigate(Routes.purchaseForm(id)) },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Routes.PURCHASE_FORM) {
                    PurchaseFormScreen(
                        onDone = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Routes.EXPENSES) {
                    ExpenseListScreen(
                        onAdd = { navController.navigate(Routes.expenseForm(0L)) },
                        onOpen = { id -> navController.navigate(Routes.expenseForm(id)) },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Routes.EXPENSE_FORM) {
                    ExpenseFormScreen(
                        onDone = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Routes.PR) {
                    PrListScreen(
                        onAdd = { navController.navigate(Routes.prForm(0L)) },
                        onOpen = { id -> navController.navigate(Routes.prForm(id)) },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Routes.PR_FORM) {
                    PrFormScreen(
                        onDone = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Routes.GIFTS) {
                    GiftListScreen(
                        onAdd = { navController.navigate(Routes.giftForm(0L)) },
                        onOpen = { id -> navController.navigate(Routes.giftForm(id)) },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Routes.GIFT_FORM) {
                    GiftFormScreen(
                        onDone = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Routes.FOC) {
                    FocListScreen(
                        onAdd = { navController.navigate(Routes.focForm(0L)) },
                        onOpen = { id -> navController.navigate(Routes.focForm(id)) },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Routes.FOC_FORM) {
                    FocFormScreen(
                        onDone = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Routes.RECORDS) {
                    RecordsScreen(
                        onOpenSale = { id -> navController.navigate(Routes.saleForm(id)) },
                        onOpenPurchase = { id -> navController.navigate(Routes.purchaseForm(id)) },
                        onOpenExpense = { id -> navController.navigate(Routes.expenseForm(id)) },
                        onOpenPr = { id -> navController.navigate(Routes.prForm(id)) },
                        onOpenGift = { id -> navController.navigate(Routes.giftForm(id)) },
                        onOpenFoc = { id -> navController.navigate(Routes.focForm(id)) }
                    )
                }
                composable(Routes.REPORTS) { ReportsScreen() }
                composable(Routes.BACKUP) {
                    BackupScreen(
                        onOpenRecycleBin = { navController.navigate(Routes.RECYCLE_BIN) }
                    )
                }
                composable(Routes.RECYCLE_BIN) {
                    RecycleBinScreen(onBack = { navController.popBackStack() })
                }
            }
        }

        if (showBottomBar) {
            NavigationBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        val hDp = with(density) { size.height.toDp() }
                        if (hDp != barHeightState.value) {
                            barHeightState.value = hDp
                        }
                    }
                    .offset { IntOffset(x = 0, y = barOffset.value.roundToInt()) }
            ) {
                TopLevelDestination.entries.forEach { d ->
                    val selected = currentDestination?.hierarchy
                        ?.any { it.route == d.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            // Always: go to the tab cleanly.
                            // Pop everything above the graph root, then navigate.
                            navController.navigate(d.route) {
                                popUpTo(navController.graph.id) {
                                    inclusive = false
                                    saveState = false
                                }
                                launchSingleTop = true
                                restoreState = false
                            }
                        },
                        icon = { Icon(d.icon, contentDescription = d.label) },
                        label = { Text(d.label) }
                    )
                }
            }
        }
    }
}
