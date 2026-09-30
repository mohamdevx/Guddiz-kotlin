package com.guddiz.app.ui.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.guddiz.app.data.local.entity.ProductEntity
import com.guddiz.app.data.local.entity.StockMovementType
import com.guddiz.app.ui.components.GuddizTextField

private val adjustmentOptions = listOf(
    StockMovementType.DAMAGED,
    StockMovementType.PERSONAL_USE,
    StockMovementType.SAMPLE,
    StockMovementType.ADJUSTMENT
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAdjustmentSheet(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onSubmit: (type: StockMovementType, units: Int, note: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedType by remember { mutableStateOf(StockMovementType.DAMAGED) }
    var units by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val isAdjustment = selectedType == StockMovementType.ADJUSTMENT
    val unitsLabel = if (isAdjustment) "Actual stock count" else "How many units?"
    val helperText = if (isAdjustment)
        "Stock will be set to this number (currently ${product.currentQuantity})."
    else
        "These units will be removed from stock."

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Adjust stock",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            Text(
                text = "Reason",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            adjustmentOptions.forEach { option ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedType == option,
                        onClick = {
                            selectedType = option
                            error = null
                        }
                    )
                    Text(option.label)
                }
            }

            GuddizTextField(
                value = units,
                onValueChange = { v ->
                    units = v.filter { it.isDigit() }
                    error = null
                },
                label = unitsLabel,
                helper = helperText,
                keyboardType = KeyboardType.Number,
                error = error
            )

            GuddizTextField(
                value = note,
                onValueChange = { note = it },
                label = "Note (optional)",
                singleLine = false
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(56.dp)
                ) { Text("Cancel") }

                Button(
                    onClick = {
                        val n = units.toIntOrNull()
                        if (n == null) {
                            error = "Enter a number."
                            return@Button
                        }
                        if (!isAdjustment && n <= 0) {
                            error = "Must be at least 1."
                            return@Button
                        }
                        onSubmit(selectedType, n, note.takeIf { it.isNotBlank() })
                    },
                    modifier = Modifier.weight(1f).height(56.dp)
                ) { Text("Save") }
            }
        }
    }
}
