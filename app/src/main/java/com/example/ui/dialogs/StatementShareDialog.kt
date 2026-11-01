package com.example.ui.dialogs

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessProfile
import com.example.data.model.Party
import com.example.data.model.TransactionEntry
import com.example.data.model.TransactionType
import com.example.ui.theme.PrimaryNavy
import com.example.util.Localization
import com.example.util.MonthlyReportPdfGenerator
import com.example.util.PhoneUtil
import java.net.URLEncoder
import java.util.Calendar

enum class StatementFilterType {
    FULL_STATEMENT,
    PENDING_BILLS_ONLY
}

enum class StatementDateRange {
    THIS_MONTH,
    LAST_3_MONTHS,
    ALL_TIME
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatementShareDialog(
    party: Party,
    profile: BusinessProfile,
    allTransactions: List<TransactionEntry>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val strings = remember(profile.languageCode) { Localization.getStrings(profile.languageCode) }

    var filterType by remember { mutableStateOf(StatementFilterType.FULL_STATEMENT) }
    var dateRange by remember { mutableStateOf(StatementDateRange.ALL_TIME) }

    // Compute bill paid amounts to accurately find pending bills
    val billPaidAmounts = remember(allTransactions) {
        val map = mutableMapOf<Long, Double>()
        for (tx in allTransactions) {
            if (tx.type == TransactionType.YOU_GOT && tx.linkedToEntryId != null) {
                val curr = map[tx.linkedToEntryId] ?: 0.0
                map[tx.linkedToEntryId] = curr + tx.amount
            }
        }
        map
    }

    // Filter transactions based on selection
    val filteredEntries = remember(filterType, dateRange, allTransactions) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfThisMonth = cal.timeInMillis
        val startOf3MonthsAgo = now - (90L * 24 * 60 * 60 * 1000)

        allTransactions.filter { entry ->
            // Date filter
            val dateMatches = when (dateRange) {
                StatementDateRange.THIS_MONTH -> entry.timestamp >= startOfThisMonth
                StatementDateRange.LAST_3_MONTHS -> entry.timestamp >= startOf3MonthsAgo
                StatementDateRange.ALL_TIME -> true
            }
            if (!dateMatches) return@filter false

            // Content filter
            when (filterType) {
                StatementFilterType.FULL_STATEMENT -> true
                StatementFilterType.PENDING_BILLS_ONLY -> {
                    if (entry.type != TransactionType.YOU_GAVE) {
                        false
                    } else {
                        val paid = billPaidAmounts[entry.id] ?: 0.0
                        (entry.amount - paid) > 0.001
                    }
                }
            }
        }
    }

    val rangeLabel = when (dateRange) {
        StatementDateRange.THIS_MONTH -> strings.thisMonth
        StatementDateRange.LAST_3_MONTHS -> strings.last3Months
        StatementDateRange.ALL_TIME -> strings.allTime
    }

    val typeLabel = when (filterType) {
        StatementFilterType.FULL_STATEMENT -> strings.fullStatement
        StatementFilterType.PENDING_BILLS_ONLY -> strings.pendingBillsOnly
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Share Khata Statement",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Party: ${party.name} (${filteredEntries.size} entries selected)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryNavy
                )

                // Statement Type Choice
                Column {
                    Text(
                        text = "Statement Type",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = filterType == StatementFilterType.FULL_STATEMENT,
                            onClick = { filterType = StatementFilterType.FULL_STATEMENT },
                            label = { Text(strings.fullStatement) },
                            modifier = Modifier.testTag("filter_full_statement")
                        )
                        FilterChip(
                            selected = filterType == StatementFilterType.PENDING_BILLS_ONLY,
                            onClick = { filterType = StatementFilterType.PENDING_BILLS_ONLY },
                            label = { Text(strings.pendingBillsOnly) },
                            modifier = Modifier.testTag("filter_pending_bills")
                        )
                    }
                }

                // Date Range Choice
                Column {
                    Text(
                        text = "Date Period",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = dateRange == StatementDateRange.THIS_MONTH,
                            onClick = { dateRange = StatementDateRange.THIS_MONTH },
                            label = { Text(strings.thisMonth) }
                        )
                        FilterChip(
                            selected = dateRange == StatementDateRange.LAST_3_MONTHS,
                            onClick = { dateRange = StatementDateRange.LAST_3_MONTHS },
                            label = { Text(strings.last3Months) }
                        )
                        FilterChip(
                            selected = dateRange == StatementDateRange.ALL_TIME,
                            onClick = { dateRange = StatementDateRange.ALL_TIME },
                            label = { Text(strings.allTime) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // PDF Share button
                Button(
                    onClick = {
                        val pdf = MonthlyReportPdfGenerator.generatePartyStatementPdf(
                            context = context,
                            profile = profile,
                            party = party,
                            entries = filteredEntries,
                            periodLabel = "$typeLabel ($rangeLabel)"
                        )
                        if (pdf != null) {
                            MonthlyReportPdfGenerator.sharePdfFile(
                                context,
                                pdf,
                                "Statement - ${party.name}"
                            )
                        }
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                    modifier = Modifier.testTag("share_pdf_statement_button")
                ) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "PDF")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF")
                }

                // WhatsApp / Text Share button
                OutlinedButton(
                    onClick = {
                        val statementText = MonthlyReportPdfGenerator.generateTextStatement(
                            profile = profile,
                            party = party,
                            entries = filteredEntries,
                            dateRangeLabel = "$typeLabel ($rangeLabel)"
                        )
                        val waNumber = PhoneUtil.toWhatsAppNumber(party.phone)
                        val encoded = URLEncoder.encode(statementText, "UTF-8")
                        val waUri = Uri.parse("https://wa.me/$waNumber?text=$encoded")
                        val waIntent = Intent(Intent.ACTION_VIEW, waUri)
                        try {
                            context.startActivity(waIntent)
                        } catch (e: Exception) {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, statementText)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Statement"))
                        }
                        onDismiss()
                    },
                    modifier = Modifier.testTag("share_text_statement_button")
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "WhatsApp")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
