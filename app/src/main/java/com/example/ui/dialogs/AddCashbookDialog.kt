package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.PaymentMode
import com.example.ui.theme.CreditRed
import com.example.ui.theme.DebitGreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddCashbookDialog(
    initialIsCashIn: Boolean,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (
        amount: Double,
        isCashIn: Boolean,
        paymentMode: PaymentMode,
        description: String,
        partyName: String,
        billNumber: String
    ) -> Unit
) {
    var isCashIn by remember { mutableStateOf(initialIsCashIn) }
    var amountText by remember { mutableStateOf("") }
    var partyName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var billNumber by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf(PaymentMode.CASH) }
    var amountError by remember { mutableStateOf(false) }

    val accentColor = if (isCashIn) DebitGreen else CreditRed

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isCashIn) "Cash In (+ $currencySymbol)" else "Cash Out (- $currencySymbol)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isCashIn,
                        onClick = { isCashIn = true },
                        label = { Text("Cash In (+)") }
                    )
                    FilterChip(
                        selected = !isCashIn,
                        onClick = { isCashIn = false },
                        label = { Text("Cash Out (-)") }
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        amountError = false
                    },
                    label = { Text("Amount ($currencySymbol) *") },
                    isError = amountError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cashbook_amount_input")
                )

                OutlinedTextField(
                    value = partyName,
                    onValueChange = { partyName = it },
                    label = { Text(if (isCashIn) "Source / Customer" else "Expense Category / Vendor") },
                    placeholder = { Text(if (isCashIn) "e.g., Counter Sale" else "e.g., Shop Rent, Transport") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Remarks / Particulars") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cashbook_desc_input")
                )

                OutlinedTextField(
                    value = billNumber,
                    onValueChange = { billNumber = it },
                    label = { Text("Voucher / Bill No. (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Mode",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PaymentMode.entries.forEach { mode ->
                        FilterChip(
                            selected = paymentMode == mode,
                            onClick = { paymentMode = mode },
                            label = { Text(mode.name.replace("_", " ")) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt <= 0.0) {
                        amountError = true
                        return@Button
                    }
                    onSave(
                        amt,
                        isCashIn,
                        paymentMode,
                        description.ifBlank { if (isCashIn) "Cash Counter Sale" else "Store Expense" },
                        partyName,
                        billNumber
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                modifier = Modifier.testTag("save_cashbook_button")
            ) {
                Text(if (isCashIn) "Save Cash In" else "Save Cash Out")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
