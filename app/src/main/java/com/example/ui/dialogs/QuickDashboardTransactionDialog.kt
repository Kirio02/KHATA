package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import com.example.data.model.Party
import com.example.data.model.PaymentMode
import com.example.data.model.TransactionType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickDashboardTransactionDialog(
    parties: List<Party>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (
        partyId: Long,
        amount: Double,
        type: TransactionType,
        paymentMode: PaymentMode,
        description: String
    ) -> Unit
) {
    var selectedParty by remember { mutableStateOf(parties.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf("") }
    var txType by remember { mutableStateOf(TransactionType.YOU_GAVE) }
    var paymentMode by remember { mutableStateOf(PaymentMode.CASH) }
    var description by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Quick Khata Entry",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (parties.isEmpty()) {
                    Text(
                        text = "Please add at least one Party first.",
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedParty?.name ?: "Select Party",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Party") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            parties.forEach { party ->
                                DropdownMenuItem(
                                    text = { Text("${party.name} (${party.type.name})") },
                                    onClick = {
                                        selectedParty = party
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = txType == TransactionType.YOU_GAVE,
                            onClick = { txType = TransactionType.YOU_GAVE },
                            label = { Text("You Gave ($currencySymbol)") }
                        )
                        FilterChip(
                            selected = txType == TransactionType.YOU_GOT,
                            onClick = { txType = TransactionType.YOU_GOT },
                            label = { Text("You Got ($currencySymbol)") }
                        )
                    }

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it
                            errorText = null
                        },
                        label = { Text("Amount ($currencySymbol) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_tx_amount_input")
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Remarks / Items") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
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

                    if (errorText != null) {
                        Text(
                            text = errorText!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val party = selectedParty
                    val amt = amountText.toDoubleOrNull()
                    if (party == null) {
                        errorText = "Select a party"
                        return@Button
                    }
                    if (amt == null || amt <= 0.0) {
                        errorText = "Enter a valid amount"
                        return@Button
                    }
                    onSave(
                        party.id,
                        amt,
                        txType,
                        paymentMode,
                        description.ifBlank { "Quick Entry" }
                    )
                },
                enabled = parties.isNotEmpty(),
                modifier = Modifier.testTag("save_quick_tx_button")
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
