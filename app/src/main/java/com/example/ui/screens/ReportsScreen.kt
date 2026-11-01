package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessProfile
import com.example.data.model.Party
import com.example.data.model.PaymentMode
import com.example.data.model.TransactionEntry
import com.example.data.model.TransactionType
import com.example.ui.components.TransactionItem
import com.example.ui.theme.CreditRed
import com.example.ui.theme.DebitGreen
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

@Composable
fun ReportsScreen(
    profile: BusinessProfile,
    parties: List<Party>,
    transactions: List<TransactionEntry>,
    onDownloadPdfClick: () -> Unit,
    onDeleteTransaction: (TransactionEntry) -> Unit
) {
    val currency = profile.currencySymbol.ifBlank { "₹" }
    val fmt = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }

    var selectedTypeFilter by remember { mutableStateOf<TransactionType?>(null) }

    val filteredTransactions = remember(transactions, selectedTypeFilter) {
        if (selectedTypeFilter == null) transactions
        else transactions.filter { it.type == selectedTypeFilter }
    }

    val totalReceivable = parties.filter { it.balance > 0 }.sumOf { it.balance }
    val totalPayable = parties.filter { it.balance < 0 }.sumOf { abs(it.balance) }
    val totalVolume = (totalReceivable + totalPayable).coerceAtLeast(1.0)
    val receivableFraction = (totalReceivable / totalVolume).toFloat().coerceIn(0f, 1f)

    // Mode breakdown
    val upiTotal = transactions.filter { it.paymentMode == PaymentMode.UPI }.sumOf { it.amount }
    val cashTotal = transactions.filter { it.paymentMode == PaymentMode.CASH }.sumOf { it.amount }
    val bankTotal = transactions.filter { it.paymentMode == PaymentMode.BANK_TRANSFER }.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Analytics & Statements",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${transactions.size} total entries across ${parties.size} parties",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onDownloadPdfClick,
                    modifier = Modifier.testTag("reports_export_pdf_button")
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "Export PDF")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export PDF")
                }
            }
        }

        // Visual Ratio Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Receivables vs Payables Health",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { receivableFraction },
                        color = DebitGreen,
                        trackColor = CreditRed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(50))
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("You Will Get", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "$currency${fmt.format(totalReceivable)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = DebitGreen
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("You Will Give", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "$currency${fmt.format(totalPayable)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = CreditRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Payment Mode Breakdown",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ModeStatBox("CASH", "$currency${fmt.format(cashTotal)}", Modifier.weight(1f))
                        ModeStatBox("UPI QR", "$currency${fmt.format(upiTotal)}", Modifier.weight(1f))
                        ModeStatBox("BANK", "$currency${fmt.format(bankTotal)}", Modifier.weight(1f))
                    }
                }
            }
        }

        // Filter Chips
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = MaterialTheme.colorScheme.primary
                )
                FilterChip(
                    selected = selectedTypeFilter == null,
                    onClick = { selectedTypeFilter = null },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = selectedTypeFilter == TransactionType.YOU_GAVE,
                    onClick = { selectedTypeFilter = TransactionType.YOU_GAVE },
                    label = { Text("Gave") }
                )
                FilterChip(
                    selected = selectedTypeFilter == TransactionType.YOU_GOT,
                    onClick = { selectedTypeFilter = TransactionType.YOU_GOT },
                    label = { Text("Got") }
                )
                FilterChip(
                    selected = selectedTypeFilter == TransactionType.CASH_IN,
                    onClick = { selectedTypeFilter = TransactionType.CASH_IN },
                    label = { Text("Cash In") }
                )
            }
        }

        items(filteredTransactions, key = { it.id }) { tx ->
            TransactionItem(
                entry = tx,
                currencySymbol = currency,
                showPartyName = true,
                onDeleteClick = { onDeleteTransaction(tx) }
            )
        }
    }
}

@Composable
private fun ModeStatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
