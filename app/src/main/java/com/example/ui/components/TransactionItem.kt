package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.TransactionEntry
import com.example.data.model.TransactionType
import com.example.ui.theme.CreditRed
import com.example.ui.theme.CreditRedBg
import com.example.ui.theme.DebitGreen
import com.example.ui.theme.DebitGreenBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionItem(
    entry: TransactionEntry,
    currencySymbol: String,
    showPartyName: Boolean = false,
    runningBalance: Double? = null,
    onToggleGaveTick: (() -> Unit)? = null,
    onToggleTakenTick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val fmt = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
    }
    val dateFormat = SimpleDateFormat("dd MMM yy • hh:mm a", Locale.getDefault())

    val isYouGave = entry.type == TransactionType.YOU_GAVE || entry.type == TransactionType.CASH_OUT
    val isYouGot = entry.type == TransactionType.YOU_GOT || entry.type == TransactionType.CASH_IN

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("transaction_item_${entry.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Main Transaction Row (2-column ledger table layout like user's photo)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Left Column: Date & Bal + Note
                Column(modifier = Modifier.weight(1.1f)) {
                    Text(
                        text = dateFormat.format(Date(entry.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    if (runningBalance != null) {
                        Text(
                            text = "Bal. $currencySymbol${fmt.format(kotlin.math.abs(runningBalance))}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (runningBalance > 0) DebitGreen else if (runningBalance < 0) CreditRed else TextSecondary
                        )
                    }

                    if (showPartyName && entry.partyNameSnapshot.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = entry.partyNameSnapshot,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    if (entry.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = entry.description,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Normal,
                            color = TextPrimary
                        )
                    }

                    if (entry.billNumber.isNotBlank()) {
                        Text(
                            text = "Bill #${entry.billNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF1565C0)
                        )
                    }
                }

                // Middle / Right Columns: YOU GAVE & YOU GOT
                Row(
                    modifier = Modifier.weight(1.3f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.Top
                ) {
                    if (isYouGave) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "₹ ${fmt.format(entry.amount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = CreditRed
                            )
                            Text(
                                text = "YOU GAVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CreditRed
                            )
                        }
                    } else if (isYouGot) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "₹ ${fmt.format(entry.amount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = DebitGreen
                            )
                            Text(
                                text = "YOU GOT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = DebitGreen
                            )
                        }
                    }

                    if (onDeleteClick != null) {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

            // User Requirement #3: Manual Ticks for Payment GIVEN / TAKEN in 1 section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gave Tick
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(enabled = onToggleGaveTick != null) { onToggleGaveTick?.invoke() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (entry.isConfirmedGave) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircleOutline,
                        contentDescription = "Gave Confirmed",
                        tint = if (entry.isConfirmedGave) CreditRed else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (entry.isConfirmedGave) "Gave: Done ✓" else "Tick if Gave",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (entry.isConfirmedGave) FontWeight.Bold else FontWeight.Normal,
                        color = if (entry.isConfirmedGave) CreditRed else TextSecondary
                    )
                }

                // Taken Tick
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(enabled = onToggleTakenTick != null) { onToggleTakenTick?.invoke() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (entry.isConfirmedTaken) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircleOutline,
                        contentDescription = "Taken Confirmed",
                        tint = if (entry.isConfirmedTaken) DebitGreen else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (entry.isConfirmedTaken) "Taken: Done ✓" else "Tick if Taken",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (entry.isConfirmedTaken) FontWeight.Bold else FontWeight.Normal,
                        color = if (entry.isConfirmedTaken) DebitGreen else TextSecondary
                    )
                }
            }
        }
    }
}
