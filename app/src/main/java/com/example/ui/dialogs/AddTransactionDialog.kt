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
import com.example.data.model.TransactionType
import com.example.ui.theme.CreditRed
import com.example.ui.theme.DebitGreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTransactionDialog(
    partyName: String,
    initialType: TransactionType,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (
        amount: Double,
        type: TransactionType,
        paymentMode: PaymentMode,
        description: String,
        billNumber: String
    ) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var txType by remember { mutableStateOf(initialType) }
    var paymentMode by remember { mutableStateOf(PaymentMode.CASH) }
    var description by remember { mutableStateOf("") }
    var billNumber by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf(false) }

    val isYouGave = txType == TransactionType.YOU_GAVE
    val primaryColor = if (isYouGave) CreditRed else DebitGreen

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isYouGave) "You Gave $currencySymbol to $partyName"
                else "You Got $currencySymbol from $partyName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isYouGave,
                        onClick = { txType = TransactionType.YOU_GAVE },
                        label = { Text("You Gave ($currencySymbol)") }
                    )
                    FilterChip(
                        selected = !isYouGave,
                        onClick = { txType = TransactionType.YOU_GOT },
                        label = { Text("You Got ($currencySymbol)") }
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
                        .testTag("tx_amount_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Item Details / Remarks") },
                    placeholder = { Text("e.g., 10kg Sugar, Part Payment") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_description_input")
                )

                OutlinedTextField(
                    value = billNumber,
                    onValueChange = { billNumber = it },
                    label = { Text("Bill / Invoice No. (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Payment Mode",
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
                        txType,
                        paymentMode,
                        description.ifBlank { if (isYouGave) "Credit Entry" else "Payment Received" },
                        billNumber
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                modifier = Modifier.testTag("save_tx_button")
            ) {
                Text("Save Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
