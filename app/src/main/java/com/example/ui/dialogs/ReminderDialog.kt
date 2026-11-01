package com.example.ui.dialogs

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessProfile
import com.example.data.model.Party
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

@Composable
fun ReminderDialog(
    party: Party,
    profile: BusinessProfile,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currency = profile.currencySymbol.ifBlank { "₹" }
    val fmt = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }
    val amountFormatted = "$currency${fmt.format(abs(party.balance))}"

    val defaultMessage = remember(party, profile) {
        buildString {
            append("Namaste ${party.name} ji, ")
            append("a polite reminder from *${profile.businessName}* that your Khata balance of *$amountFormatted* is pending. ")
            if (profile.upiId.isNotBlank()) {
                append("You can pay conveniently via UPI to *${profile.upiId}*. ")
            }
            append("Thank you for your business!")
        }
    }

    var reminderMessage by remember { mutableStateOf(defaultMessage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Send Payment Reminder",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Party: ${party.name} • Pending: $amountFormatted",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = reminderMessage,
                    onValueChange = { reminderMessage = it },
                    label = { Text("Customize Reminder Message") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("smsto:${party.phone}")
                                putExtra("sms_body", reminderMessage)
                            }
                            runCatching { context.startActivity(smsIntent) }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Sms, contentDescription = "SMS")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SMS")
                    }

                    if (party.phone.isNotBlank()) {
                        FilledTonalButton(
                            onClick = {
                                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${party.phone}")
                                }
                                runCatching { context.startActivity(dialIntent) }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = "Call")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Call")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, reminderMessage)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Send WhatsApp Reminder"))
                    onDismiss()
                }
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "WhatsApp")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share on WhatsApp")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
