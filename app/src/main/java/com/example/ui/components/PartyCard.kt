package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.ui.theme.CreditRed
import com.example.ui.theme.DebitGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun PartyCard(
    party: Party,
    currencySymbol: String,
    onClick: () -> Unit,
    onRemindClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fmt = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
    }
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val initials = party.name.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "M" }

    val isReceivable = party.balance > 0.0 // You will get
    val isPayable = party.balance < 0.0    // You will give
    val amountColor = when {
        isReceivable -> CreditRed  // In Khatabook, red amount indicates customer debt (You will get)
        isPayable -> DebitGreen
        else -> TextSecondary
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("party_card_${party.id}")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circle with initials (like "M2" in user's photo)
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEDE7F6),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4527A0)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Party Name & time elapsed
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = party.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = dateFormat.format(Date(party.updatedAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right Column: Big readable Amount + REMIND > (Matches user's uploaded photo exactly)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹ ${fmt.format(abs(party.balance))}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    )
                    if (isReceivable) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "REMIND >",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1565C0),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(onClick = onRemindClick)
                                .padding(2.dp)
                                .testTag("remind_party_${party.id}")
                        )
                    } else if (isPayable) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "You will give",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    } else {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Settled",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 72.dp)
            )
        }
    }
}
