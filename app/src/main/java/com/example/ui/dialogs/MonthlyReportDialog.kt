package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessProfile
import com.example.data.model.Party
import com.example.data.model.TransactionEntry
import com.example.util.MonthlyReportPdfGenerator

@Composable
fun MonthlyReportDialog(
    profile: BusinessProfile,
    parties: List<Party>,
    transactions: List<TransactionEntry>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedRange by remember { mutableStateOf("Current Month Statement") }
    var generatedInfo by remember { mutableStateOf<String?>(null) }

    val ranges = listOf(
        "Current Month Statement",
        "Last 30 Days Full Ledger",
        "Complete Financial Year Statement"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Download & Share PDF Report",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select statement range to generate a clean A4 PDF report for your accountant or customers:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ranges.forEach { range ->
                    FilterChip(
                        selected = selectedRange == range,
                        onClick = { selectedRange = range },
                        label = { Text(range) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (generatedInfo != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = generatedInfo!!,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val file = MonthlyReportPdfGenerator.generateMonthlyReportPdf(
                        context = context,
                        profile = profile,
                        parties = parties,
                        transactions = transactions,
                        periodLabel = selectedRange
                    )
                    if (file != null) {
                        generatedInfo = "PDF saved (${file.name})! Opening share sheet…"
                        MonthlyReportPdfGenerator.sharePdfFile(context, file, selectedRange)
                    } else {
                        generatedInfo = "Could not generate PDF."
                    }
                }
            ) {
                Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "PDF")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Generate & Share PDF")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
