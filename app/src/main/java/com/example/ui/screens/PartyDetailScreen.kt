package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.example.ui.components.TransactionItem
import com.example.ui.dialogs.AddTransactionDialog
import com.example.ui.theme.CreditRed
import com.example.ui.theme.DebitGreen
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.MonthlyReportPdfGenerator
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
        billNumber: String
    ) -> Unit,
    onToggleGaveTick: (TransactionEntry) -> Unit,
    onToggleTakenTick: (TransactionEntry) -> Unit,
    onDeleteTransaction: (TransactionEntry) -> Unit,
    onDeleteParty: (Party) -> Unit,
    onSendReminder: (Party) -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val currency = profile.currencySymbol.ifBlank { "₹" }
    val fmt = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }

    var showTxDialogType by remember { mutableStateOf<TransactionType?>(null) }

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
                            Text(
                                text = "View settings",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFBBDEFB)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("party_detail_back_button")
                    ) {
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryNavy
                )
            )
        },
        bottomBar = {
            // Big High-Contrast Touch Targets: YOU GAVE ₹ (Red) | YOU GOT ₹ (Green)
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
                    .widthIn(max = 600.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Top Banner with Blue Background (Exact match to Photo 2)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PrimaryNavy)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFECEFF6)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                // Balance Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when {
                                            isReceivable -> "You will get"
                                            isPayable -> "You will give"
                                            else -> "Settled"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "$currency ${fmt.format(abs(party.balance))}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isReceivable) CreditRed else if (isPayable) DebitGreen else TextPrimary
                                    )
                                }

                                HorizontalDivider(color = Color(0xFFD6DBE5))

                                // Set collection reminder banner
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSendReminder(party) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = "Calendar",
                                            tint = PrimaryNavy,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Set collection reminder",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimary
                                        )
                                    }
                                    Text(
                                        text = "SET DATE",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryNavy
                                    )
                                }
                            }
                        }
                    }
                }

                // 3 Quick Action Buttons: Report | Reminder | SMS (Exact match to Photo 2)
                item {
                    Surface(
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Report Button
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val pdf = MonthlyReportPdfGenerator.generateMonthlyReportPdf(
                                            context = context,
                                            profile = profile,
                                            parties = listOf(party),
                                            transactions = transactions,
                                            periodLabel = "Statement - ${party.name}"
                                        )
                                        if (pdf != null) {
                                            MonthlyReportPdfGenerator.sharePdfFile(
                                                context,
                                                pdf,
                                                "Statement - ${party.name}"
                                            )
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "Report",
                                    tint = PrimaryNavy,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Report",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            }

                            // Reminder (WhatsApp) Button
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSendReminder(party) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
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
                                text = "ENTRIES",
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
                    items(transactions, key = { it.id }) { entry ->
                        TransactionItem(
                            entry = entry,
                            currencySymbol = currency,
                            showPartyName = false,
                            runningBalance = party.balance,
                            onToggleGaveTick = { onToggleGaveTick(entry) },
                            onToggleTakenTick = { onToggleTakenTick(entry) },
                            onDeleteClick = { onDeleteTransaction(entry) },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

    if (showTxDialogType != null) {
        AddTransactionDialog(
            partyName = party.name,
            initialType = showTxDialogType!!,
            currencySymbol = currency,
            onDismiss = { showTxDialogType = null },
            onSave = { amt, type, mode, desc, bill ->
                onAddTransaction(amt, type, mode, desc, bill)
                showTxDialogType = null
            }
        )
    }
}
