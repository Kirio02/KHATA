package com.example.ui.dialogs

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessProfile
import com.example.util.QrCodeUtil

@Composable
fun QrStandeeDialog(
    profile: BusinessProfile,
    onDismiss: () -> Unit,
    onSaveUpiId: (String) -> Unit
) {
    val context = LocalContext.current
    var upiIdInput by remember(profile.upiId) { mutableStateOf(profile.upiId) }
    var customAmount by remember { mutableStateOf("") }

    val upiUri = remember(upiIdInput, customAmount, profile.businessName) {
        QrCodeUtil.buildUpiUri(
            upiId = upiIdInput.ifBlank { "yourname@upi" },
            payeeName = profile.businessName.ifBlank { "My Store" },
            amount = customAmount.toDoubleOrNull()
        )
    }

    val qrBitmap = remember(upiUri) {
        QrCodeUtil.generateQrBitmap(upiUri, 512)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Merchant UPI QR Standee",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Branded Standee Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF0D47A1), Color(0xFF1565C0))
                            )
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = profile.businessName.ifBlank { "Kirio Merchant" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "SCAN & PAY WITH ANY UPI APP",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFBBDEFB)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Merchant UPI QR Code",
                                modifier = Modifier
                                    .size(200.dp)
                                    .padding(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified UPI",
                                tint = Color(0xFF69F0AE),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = upiIdInput,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = upiIdInput,
                    onValueChange = { upiIdInput = it },
                    label = { Text("Enter Your Merchant UPI ID") },
                    placeholder = { Text("e.g. storeowner@upi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = customAmount,
                    onValueChange = { customAmount = it },
                    label = { Text("Request Specific Amount (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveUpiId(upiIdInput.trim())
                    val shareText = buildString {
                        appendLine("Pay *${profile.businessName}* instantly via UPI:")
                        appendLine("UPI ID: $upiIdInput")
                        if (!customAmount.toDoubleOrNull()?.toString().isNullOrBlank()) {
                            appendLine("Amount: ${profile.currencySymbol}$customAmount")
                        }
                        appendLine("UPI Link: $upiUri")
                    }
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Payment Link"))
                }
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save & Share Link")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onSaveUpiId(upiIdInput.trim())
                    onDismiss()
                }
            ) {
                Text("Done")
            }
        }
    )
}
