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
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PaymentMode
import com.example.data.model.TransactionEntry
import com.example.data.model.TransactionType
import com.example.ui.components.TransactionItem
import com.example.ui.dialogs.AddCashbookDialog
import com.example.ui.theme.CreditRed
import com.example.ui.theme.CreditRedBg
import com.example.ui.theme.DebitGreen
import com.example.ui.theme.DebitGreenBg
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CashbookScreen(
    cashbookEntries: List<TransactionEntry>,
    currencySymbol: String,
    onAddCashbookEntry: (
        amount: Double,
        isCashIn: Boolean,
        paymentMode: PaymentMode,
        description: String,
        partyName: String,
        billNumber: String
    ) -> Unit,
    onDeleteEntry: (TransactionEntry) -> Unit
) {
    val fmt = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }
    val totalCashIn = cashbookEntries.filter { it.type == TransactionType.CASH_IN }.sumOf { it.amount }
    val totalCashOut = cashbookEntries.filter { it.type == TransactionType.CASH_OUT }.sumOf { it.amount }
    val netCashInHand = totalCashIn - totalCashOut

    var showCashDialogIsIn by remember { mutableStateOf<Boolean?>(null) }

    Scaffold(
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showCashDialogIsIn = false },
                        colors = ButtonDefaults.buttonColors(containerColor = CreditRed),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("cash_out_button")
                    ) {
                        Icon(imageVector = Icons.Default.RemoveCircleOutline, contentDescription = "Cash Out")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CASH OUT (-)", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showCashDialogIsIn = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DebitGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("cash_in_button")
                    ) {
                        Icon(imageVector = Icons.Default.AddCircleOutline, contentDescription = "Cash In")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CASH IN (+)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF00695C), Color(0xFF1565C0))
                                    )
                                )
                                .padding(18.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Daily Cashbook & Drawer Balance",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFFE0F2F1)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$currencySymbol${fmt.format(netCashInHand)}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DebitGreenBg)
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Total Cash In (+)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DebitGreen
                                    )
                                    Text(
                                        text = "$currencySymbol${fmt.format(totalCashIn)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DebitGreen
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CreditRedBg)
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Total Cash Out (-)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CreditRed
                                    )
                                    Text(
                                        text = "$currencySymbol${fmt.format(totalCashOut)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CreditRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Cashbook Entries (${cashbookEntries.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (cashbookEntries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No cashbook entries yet. Tap CASH IN or CASH OUT below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(cashbookEntries, key = { it.id }) { entry ->
                    TransactionItem(
                        entry = entry,
                        currencySymbol = currencySymbol,
                        showPartyName = true,
                        onDeleteClick = { onDeleteEntry(entry) }
                    )
                }
            }
        }
    }

    if (showCashDialogIsIn != null) {
        AddCashbookDialog(
            initialIsCashIn = showCashDialogIsIn!!,
            currencySymbol = currencySymbol,
            onDismiss = { showCashDialogIsIn = null },
            onSave = { amt, isIn, mode, desc, party, bill ->
                onAddCashbookEntry(amt, isIn, mode, desc, party, bill)
                showCashDialogIsIn = null
            }
        )
    }
}
