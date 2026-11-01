package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.ui.theme.DebitGreen
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionItem(
    entry: TransactionEntry,
    currencySymbol: String,
    showPartyName: Boolean = false,
    runningBalance: Double? = null,
    totalPaidAgainstBill: Double = 0.0,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: (() -> Unit)? = null,
    onLongPressBill: (() -> Unit)? = null,
    onMarkReceivedClick: (() -> Unit)? = null,
    onUndoReceivedClick: (() -> Unit)? = null,
    onToggleCompleted: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val fmt = NumberFormat.getNumberInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()).apply {
        maximumFractionDigits = 0
    }
    val dateFormat = SimpleDateFormat("dd MMM yy • hh:mm a", Locale.getDefault())

    val isCreditBill = entry.type == TransactionType.YOU_GAVE
    val isPaymentReceived = entry.type == TransactionType.YOU_GOT
    val isYouGave = entry.type == TransactionType.YOU_GAVE || entry.type == TransactionType.CASH_OUT
    val isYouGot = entry.type == TransactionType.YOU_GOT || entry.type == TransactionType.CASH_IN

    val outstanding = (entry.amount - totalPaidAgainstBill).coerceAtLeast(0.0)

    val cardBorderColor = if (isSelected) PrimaryNavy else Color.Transparent

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE8EAF6) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, PrimaryNavy) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = {
                    if (isSelectionMode && isCreditBill) {
                        onToggleSelect?.invoke()
                    } else {
                        onEditClick?.invoke()
                    }
                },
                onLongClick = {
                    if (isCreditBill) {
                        onLongPressBill?.invoke()
                    }
                }
            )
            .testTag("transaction_item_${entry.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Main Transaction Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Multi-select Checkbox on the left
                if (isSelectionMode && isCreditBill) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect?.invoke() },
                        colors = CheckboxDefaults.colors(checkedColor = PrimaryNavy),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("select_bill_checkbox_${entry.id}")
                    )
                }

                // Left Column: Date & Running Bal + Particulars
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
                            text = "Bal. $currencySymbol${fmt.format(abs(runningBalance))}",
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

                    if (entry.linkedToEntryId != null) {
                        Text(
                            text = "Settles Bill #${entry.linkedToEntryId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = DebitGreen
                        )
                    }
                }

                // Right Column: Amount & Type & Edit/Delete actions
                Row(
                    modifier = Modifier.weight(1.2f),
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

                    if (onEditClick != null && !isSelectionMode) {
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.size(32.dp).testTag("edit_tx_button_${entry.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Entry",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (onDeleteClick != null && !isSelectionMode) {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(32.dp).testTag("delete_tx_button_${entry.id}")
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

            // Bottom Status & Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Status Chip for Credit Bills OR Payment indicator
                if (isCreditBill) {
                    val statusText: String
                    val statusBg: Color
                    val statusFg: Color
                    when {
                        totalPaidAgainstBill >= entry.amount -> {
                            statusText = "Received ✓"
                            statusBg = Color(0xFFE8F5E9)
                            statusFg = Color(0xFF2E7D32)
                        }
                        totalPaidAgainstBill > 0.0 -> {
                            statusText = "Partly received ₹${fmt.format(totalPaidAgainstBill)}"
                            statusBg = Color(0xFFFFF3E0)
                            statusFg = Color(0xFFE65100)
                        }
                        else -> {
                            statusText = "Pending"
                            statusBg = Color(0xFFFFEBEE)
                            statusFg = Color(0xFFC62828)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = statusBg,
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusFg,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Mark received action button if bill is pending or partly received
                        if (outstanding > 0.0 && onMarkReceivedClick != null && !isSelectionMode) {
                            FilledTonalButton(
                                onClick = onMarkReceivedClick,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFFE8F5E9),
                                    contentColor = Color(0xFF2E7D32)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("mark_received_button_${entry.id}")
                            ) {
                                Text(
                                    text = "Mark received",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Undo button if payments were received against this bill
                        if (totalPaidAgainstBill > 0.0 && onUndoReceivedClick != null && !isSelectionMode) {
                            TextButton(
                                onClick = onUndoReceivedClick,
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("undo_received_button_${entry.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = "Undo",
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Undo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFE65100),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // Non-bill entry (Payment)
                    Text(
                        text = "Mode: ${entry.paymentMode.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                // Right side: Plain "Completed" checkbox bound to isCompleted (No effect on money)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("completed_checkbox_row_${entry.id}")
                ) {
                    Checkbox(
                        checked = entry.isCompleted,
                        onCheckedChange = { onToggleCompleted?.invoke() },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1565C0)),
                        modifier = Modifier.size(24.dp).testTag("completed_checkbox_${entry.id}")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (entry.isCompleted) "Completed ✓" else "Completed",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (entry.isCompleted) FontWeight.Bold else FontWeight.Normal,
                        color = if (entry.isCompleted) Color(0xFF1565C0) else TextSecondary
                    )
                }
            }
        }
    }
}
