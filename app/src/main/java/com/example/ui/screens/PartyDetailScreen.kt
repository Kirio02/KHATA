package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessProfile
import com.example.data.model.Party
import com.example.data.model.PaymentMode
import com.example.data.model.TransactionEntry
import com.example.data.model.TransactionType
import com.example.data.repository.computeRunningBalances
import com.example.ui.components.TransactionItem
import com.example.ui.dialogs.AddTransactionDialog
import com.example.ui.dialogs.EditTransactionDialog
import com.example.ui.dialogs.MarkReceivedDialog
import com.example.ui.dialogs.StatementShareDialog
import com.example.ui.theme.CreditRed
import com.example.ui.theme.DebitGreen
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.Localization
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyDetailScreen(
    party: Party,
    transactions: List<TransactionEntry>,
    profile: BusinessProfile,
    onBack: () -> Unit,
    onAddTransaction: (
        amount: Double,
        type: TransactionType,
        paymentMode: PaymentMode,
        description: String,
        billNumber: String,
        addToCashbook: Boolean
    ) -> Unit,
    onMarkReceived: ((bill: TransactionEntry, amount: Double, paymentMode: PaymentMode, timestamp: Long, addToCashbook: Boolean) -> Unit)? = null,
    onUndoReceived: ((bill: TransactionEntry) -> Unit)? = null,
    onMultiBillReceived: ((billIds: List<Long>, paymentMode: PaymentMode, addToCashbook: Boolean) -> Unit)? = null,
    onToggleCompleted: ((TransactionEntry) -> Unit)? = null,
    onEditTransaction: ((TransactionEntry, Double, String, Long, PaymentMode, TransactionType, String) -> Unit)? = null,
    onDeleteTransaction: (TransactionEntry) -> Unit,
    onDeleteParty: (Party) -> Unit,
    onSendReminder: (Party) -> Unit
) {
    val context = LocalContext.current
    val strings = remember(profile.languageCode) { Localization.getStrings(profile.languageCode) }
    val currency = profile.currencySymbol.ifBlank { "₹" }
    val fmt = NumberFormat.getNumberInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()).apply {
        maximumFractionDigits = 0
    }

    var showTxDialogType by remember { mutableStateOf<TransactionType?>(null) }
    var editingEntry by remember { mutableStateOf<TransactionEntry?>(null) }
    var billToMarkReceived by remember { mutableStateOf<TransactionEntry?>(null) }
    var showStatementDialog by remember { mutableStateOf(false) }

    // Multi-select state
    var selectedBillIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val isSelectionMode = selectedBillIds.isNotEmpty()

    BackHandler(enabled = isSelectionMode) {
        selectedBillIds = emptySet()
    }
    BackHandler(enabled = !isSelectionMode, onBack = onBack)

    val runningBalances = remember(transactions) { computeRunningBalances(transactions) }

    // Calculate total paid amounts per bill
    val billPaidAmounts = remember(transactions) {
        val map = mutableMapOf<Long, Double>()
        for (tx in transactions) {
            if (tx.type == TransactionType.YOU_GOT && tx.linkedToEntryId != null) {
                val curr = map[tx.linkedToEntryId] ?: 0.0
                map[tx.linkedToEntryId] = curr + tx.amount
            }
        }
        map
    }

    val isReceivable = party.balance > 0.0
    val isPayable = party.balance < 0.0

    val initials = party.name.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "M" }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedBillIds.size} ${strings.selectedBills}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { selectedBillIds = emptySet() }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    },
                    actions = {
                        Button(
                            onClick = {
                                onMultiBillReceived?.invoke(selectedBillIds.toList(), PaymentMode.CASH, true)
                                selectedBillIds = emptySet()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DebitGreen),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("mark_all_selected_received_button")
                        ) {
                            Text(
                                text = "Mark received (${selectedBillIds.size})",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryNavy)
                )
            } else {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEDE7F6),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = initials,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4527A0)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = party.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (party.phone.isNotBlank()) {
                                    Text(
                                        text = party.phone,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFBBDEFB)
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        if (party.phone.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${party.phone}"))
                                    runCatching { context.startActivity(dial) }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call Party",
                                    tint = Color.White
                                )
                            }
                        }
                        IconButton(onClick = { onDeleteParty(party) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Party",
                                tint = Color(0xFFFFCDD2)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryNavy)
                )
            }
        },
        bottomBar = {
            if (!isSelectionMode) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showTxDialogType = TransactionType.YOU_GAVE },
                            colors = ButtonDefaults.buttonColors(containerColor = CreditRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("you_gave_button")
                        ) {
                            Text(
                                text = "YOU GAVE $currency",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }

                        Button(
                            onClick = { showTxDialogType = TransactionType.YOU_GOT },
                            colors = ButtonDefaults.buttonColors(containerColor = DebitGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("you_got_button")
                        ) {
                            Text(
                                text = "YOU GOT $currency",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (isSelectionMode) 0.dp else 4.dp)
            ) {
                // Header Balance Card & Quick Action Icons
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = when {
                                            isReceivable -> strings.youWillGet
                                            isPayable -> strings.youWillGive
                                            else -> strings.settled
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isReceivable -> DebitGreen
                                            isPayable -> CreditRed
                                            else -> TextSecondary
                                        }
                                    )
                                    Text(
                                        text = "$currency ${fmt.format(abs(party.balance))}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = when {
                                            isReceivable -> DebitGreen
                                            isPayable -> CreditRed
                                            else -> TextPrimary
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 3 Iconic Action Buttons: Report (PDF/WhatsApp) | Reminder | SMS
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Statement / Report Button
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showStatementDialog = true }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .testTag("party_report_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = "Report",
                                        tint = PrimaryNavy,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Statement",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                }

                                // Reminder (WhatsApp direct chat) Button
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onSendReminder(party) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .testTag("party_reminder_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = "Reminder",
                                        tint = Color(0xFF25D366),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Reminder",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                }

                                // SMS Button
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            val smsUri = Uri.parse("smsto:${party.phone}")
                                            val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                                                putExtra(
                                                    "sms_body",
                                                    "Dear ${party.name}, an amount of $currency${fmt.format(abs(party.balance))} is due at ${profile.businessName}. Please clear at earliest."
                                                )
                                            }
                                            runCatching { context.startActivity(smsIntent) }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .testTag("party_sms_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "SMS",
                                        tint = PrimaryNavy,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "SMS",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Table Header Row: ENTRIES | YOU GAVE | YOU GOT
                item {
                    Surface(
                        color = Color(0xFFEEEEEE),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ENTRIES (Long-press to select)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                modifier = Modifier.weight(1.1f)
                            )
                            Text(
                                text = "YOU GAVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CreditRed,
                                modifier = Modifier.weight(0.7f)
                            )
                            Text(
                                text = "YOU GOT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = DebitGreen,
                                modifier = Modifier.weight(0.7f)
                            )
                        }
                    }
                }

                if (transactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No entries yet. Tap 'YOU GAVE' or 'YOU GOT' below.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    // Ledger browser: displays newest first
                    val displayEntries = transactions.sortedWith(compareByDescending<TransactionEntry> { it.timestamp }.thenByDescending { it.id })

                    items(displayEntries, key = { it.id }) { entry ->
                        val paidAgainstThis = billPaidAmounts[entry.id] ?: 0.0

                        TransactionItem(
                            entry = entry,
                            currencySymbol = currency,
                            showPartyName = false,
                            runningBalance = runningBalances[entry.id],
                            totalPaidAgainstBill = paidAgainstThis,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedBillIds.contains(entry.id),
                            onToggleSelect = {
                                selectedBillIds = if (selectedBillIds.contains(entry.id)) {
                                    selectedBillIds - entry.id
                                } else {
                                    selectedBillIds + entry.id
                                }
                            },
                            onLongPressBill = {
                                if (entry.type == TransactionType.YOU_GAVE) {
                                    selectedBillIds = selectedBillIds + entry.id
                                }
                            },
                            onMarkReceivedClick = { billToMarkReceived = entry },
                            onUndoReceivedClick = { onUndoReceived?.invoke(entry) },
                            onToggleCompleted = { onToggleCompleted?.invoke(entry) },
                            onEditClick = { editingEntry = entry },
                            onDeleteClick = { onDeleteTransaction(entry) },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

    // Add Transaction Dialog
    if (showTxDialogType != null) {
        AddTransactionDialog(
            partyName = party.name,
            initialType = showTxDialogType!!,
            currencySymbol = currency,
            onDismiss = { showTxDialogType = null },
            onSave = { amt, type, mode, desc, bill, addToCashbook ->
                onAddTransaction(amt, type, mode, desc, bill, addToCashbook)
                showTxDialogType = null
            }
        )
    }

    // Edit Transaction Dialog
    if (editingEntry != null) {
        EditTransactionDialog(
            entry = editingEntry!!,
            currencySymbol = currency,
            onDismiss = { editingEntry = null },
            onSave = { updatedEntry, amt, desc, time, mode, type, bill ->
                onEditTransaction?.invoke(updatedEntry, amt, desc, time, mode, type, bill)
                editingEntry = null
            }
        )
    }

    // Mark Received Dialog
    if (billToMarkReceived != null) {
        val totalPaid = billPaidAmounts[billToMarkReceived!!.id] ?: 0.0
        val outstanding = (billToMarkReceived!!.amount - totalPaid).coerceAtLeast(0.0)

        MarkReceivedDialog(
            bill = billToMarkReceived!!,
            outstandingAmount = outstanding,
            currencySymbol = currency,
            onDismiss = { billToMarkReceived = null },
            onConfirm = { amt, mode, time, addToCashbook ->
                onMarkReceived?.invoke(billToMarkReceived!!, amt, mode, time, addToCashbook)
                billToMarkReceived = null
            }
        )
    }

    // Statement Share Dialog (Full / Pending bills + Date range)
    if (showStatementDialog) {
        StatementShareDialog(
            party = party,
            profile = profile,
            allTransactions = transactions,
            onDismiss = { showStatementDialog = false }
        )
    }
}
