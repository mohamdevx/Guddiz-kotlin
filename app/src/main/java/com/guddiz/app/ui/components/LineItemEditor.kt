package com.guddiz.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.guddiz.app.core.money.Money
import com.guddiz.app.data.local.entity.ProductEntity

/**
 * Reusable widget for PR packages and Gifts.
 * Each line = one product + quantity. Cost per line is derived, never typed in.
 */
data class LineDraft(
    val productId: Long? = null,
    val productName: String = "",
    val unitPrice: String = "",
    val quantity: String = "1"
)

@Composable
fun LineItemEditor(
    lines: List<LineDraft>,
    products: List<ProductEntity>,
    onLineChange: (index: Int, LineDraft) -> Unit,
    onAddLine: () -> Unit,
    onRemoveLine: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        lines.forEachIndexed { idx, line ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SearchableProductPicker(
                        label = "Product",
                        products = products,
                        selectedProductId = line.productId,
                        onSelect = { p ->
                            val newUnit = if (line.unitPrice.isBlank())
                                Money(p.sellingPrice).toEditText() else line.unitPrice
                            onLineChange(idx, line.copy(
                                productId = p.id,
                                productName = p.name,
                                unitPrice = newUnit
                            ))
                        }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = line.quantity,
                            onValueChange = { v ->
                                val filtered = v.filter { c -> c.isDigit() }
                                onLineChange(idx, line.copy(quantity = filtered))
                            },
                            label = { Text("Qty") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = line.unitPrice,
                            onValueChange = { v -> onLineChange(idx, line.copy(unitPrice = v)) },
                            label = { Text("Unit price") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(2f)
                        )
                    }
                    val qty = line.quantity.toIntOrNull() ?: 0
                    val price = Money.parse(line.unitPrice) ?: Money.ZERO
                    val lineCost = price * qty
                    Text(
                        text = "Line: " + lineCost.format(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(onClick = { onRemoveLine(idx) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove line")
                }
            }
        }

        OutlinedButton(
            onClick = onAddLine,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Add product")
        }
    }
}
