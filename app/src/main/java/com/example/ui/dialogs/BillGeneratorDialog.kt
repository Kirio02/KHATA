package com.example.ui.dialogs

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessProfile
import com.example.data.model.Party
import com.example.ui.theme.PrimaryNavy
import java.text.NumberFormat
import java.util.Locale

data class SimpleBillItem(
    val name: String,
    val amount: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillGeneratorDialog(
    profile: BusinessProfile,
    parties: List<Party>,
    onDismiss: () -> Unit,
    onRecordBillToKhata: (
        partyId: Long?,
        amount: Double,
        billNumber: String,
        itemsSummary: String,
        isCreditSale: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val currency = profile.currencySymbol.ifBlank { "₹" }
    val fmt = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }

    var selectedParty by remember { mutableStateOf(parties.firstOrNull()) }
    var partyExpanded by remember { mutableStateOf(false) }
    var billNumber by remember { mutableStateOf("BILL-${(100..999).random()}") }
    var isCreditSale by remember { mutableStateOf(true) }

    // Simplified Quick Bill items
    val items = remember {
        mutableStateListOf(
            SimpleBillItem("General Grocery Items", 450.0)
        )
    }

    var quickItemName by remember { mutableStateOf("") }
    var quickItemAmount by remember { mutableStateOf("") }

    val grandTotal = items.sumOf { it.amount }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Simple Quick Bill",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Party Selector
                if (parties.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = partyExpanded,
                        onExpandedChange = { partyExpanded = !partyExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedParty?.name ?: "Walk-in Customer",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Customer Name") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = partyExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = partyExpanded,
                            onDismissRequest = { partyExpanded = false }
                        ) {
                            parties.forEach { party ->
                                DropMenuItem(party.name) {
                                    selectedParty = party
                                    partyExpanded = false
                                }
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isCreditSale,
                        onClick = { isCreditSale = true },
                        label = { Text("Credit (Add to Khata)") }
                    )
                    FilterChip(
                        selected = !isCreditSale,
                        onClick = { isCreditSale = false },
                        label = { Text("Cash Received") }
                    )
                }

                Text(
                    text = "Items in Bill (${items.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFECEFF1))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$currency${fmt.format(item.amount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { items.removeAt(index) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Super Simple 1-step Add Item row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = quickItemName,
                        onValueChange = { quickItemName = it },
                        label = { Text("Item Name") },
                        placeholder = { Text("e.g. Milk & Bread") },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                    )
                    OutlinedTextField(
                        value = quickItemAmount,
                        onValueChange = { quickItemAmount = it },
                        label = { Text("Price (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val amt = quickItemAmount.toDoubleOrNull() ?: 0.0
                            if (quickItemName.isNotBlank() && amt > 0.0) {
                                items.add(SimpleBillItem(quickItemName.trim(), amt))
                                quickItemName = ""
                                quickItemAmount = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                    }
                }

                // Total Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFE3F2FD))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Bill Amount:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$currency${fmt.format(grandTotal)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryNavy
                        )
                    }
                }

                FilledTonalButton(
                    onClick = {
                        val shareText = buildString {
                            appendLine("🧾 *BILL from ${profile.businessName}*")
                            appendLine("Bill No: $billNumber")
                            appendLine("Customer: ${selectedParty?.name ?: "Cash Sale"}")
                            appendLine("--------------------------------")
                            items.forEach {
                                appendLine("• ${it.name}: $currency${fmt.format(it.amount)}")
                            }
                            appendLine("--------------------------------")
                            appendLine("*Total Amount: $currency${fmt.format(grandTotal)}*")
                            if (profile.upiId.isNotBlank()) appendLine("Pay via UPI: ${profile.upiId}")
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Bill"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Bill via WhatsApp / SMS")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (grandTotal > 0.0) {
                        val summary = items.joinToString(", ") { it.name }
                        onRecordBillToKhata(
                            selectedParty?.id,
                            grandTotal,
                            billNumber,
                            summary,
                            isCreditSale
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                enabled = items.isNotEmpty(),
                modifier = Modifier.testTag("save_bill_button")
            ) {
                Text("Save Bill", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DropMenuItem(text: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text) },
        onClick = onClick
    )
}
