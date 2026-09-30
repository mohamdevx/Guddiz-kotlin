package com.guddiz.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    DASHBOARD(Routes.DASHBOARD, "Home", Icons.Outlined.Home),
    PRODUCTS(Routes.PRODUCTS, "Products", Icons.Outlined.Inventory2),
    RECORDS(Routes.RECORDS, "Records", Icons.AutoMirrored.Outlined.ReceiptLong),
    REPORTS(Routes.REPORTS, "Reports", Icons.Outlined.BarChart),
    BACKUP(Routes.BACKUP, "Backup", Icons.Outlined.Backup)
}
