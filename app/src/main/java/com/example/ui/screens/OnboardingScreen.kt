package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessProfile

@Composable
fun OnboardingScreen(
    initialProfile: BusinessProfile,
    onCompleteSetup: (BusinessProfile) -> Unit,
    onSkip: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }

    var businessName by remember(initialProfile) { mutableStateOf(initialProfile.businessName) }
    var ownerName by remember(initialProfile) { mutableStateOf(initialProfile.ownerName) }
    var phone by remember(initialProfile) { mutableStateOf(initialProfile.phone) }
    var address by remember(initialProfile) { mutableStateOf(initialProfile.address) }
    var upiId by remember(initialProfile) { mutableStateOf(initialProfile.upiId) }
    var gstin by remember(initialProfile) { mutableStateOf(initialProfile.gstin) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 540.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Top Bar with step indicator and Skip button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1565C0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = "Kirio Logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = if (step == 0) "Welcome to Kirio Khata" else "Store Owner Setup",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(
                            onClick = onSkip,
                            modifier = Modifier.testTag("onboarding_skip_button")
                        ) {
                            Text(
                                text = "Skip",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Step Dots
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF1565C0))
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (step == 1) Color(0xFF1565C0)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }

                    if (step == 0) {
                        // Step 0: App Intro & Instructions
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1565C0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Simple Digital Khata & Ledger",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Manage customer credit (Udhaar), supplier payables, daily cashbook, and UPI collections in one clean, reliable ledger.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFE3F2FD)
                                )
                            }
                        }

                        Text(
                            text = "How to Use Kirio (Quick Guide)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        InstructionStepCard(
                            stepNumber = "1",
                            icon = Icons.Default.Book,
                            title = "Add Customers & Suppliers",
                            description = "Tap '+ Add Customer' on the Khata screen to add a party with their phone number and optional opening balance."
                        )

                        InstructionStepCard(
                            stepNumber = "2",
                            icon = Icons.Default.CheckCircle,
                            title = "Record 'You Gave' & 'You Got'",
                            description = "Open any party and tap RED 'YOU GAVE' when giving goods/credit, or GREEN 'YOU GOT' when receiving payment."
                        )

                        InstructionStepCard(
                            stepNumber = "3",
                            icon = Icons.Default.NotificationsActive,
                            title = "Send WhatsApp & SMS Reminders",
                            description = "Tap 'Remind' on any customer balance to send a polite payment reminder with your UPI ID link."
                        )

                        InstructionStepCard(
                            stepNumber = "4",
                            icon = Icons.Default.AccountBalanceWallet,
                            title = "Daily Cashbook & PDF Statements",
                            description = "Track daily Cash In / Cash Out in the Cashbook tab and export clean PDF statements anytime."
                        )
                    } else {
                        // Step 1: Prompt Owner to Input Store Details
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode2,
                                        contentDescription = "Store Info",
                                        tint = Color(0xFF1565C0)
                                    )
                                    Column {
                                        Text(
                                            text = "Enter Your Store & Owner Details",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Used on your Khata header, PDF reports, bills, and UPI QR standee.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                HorizontalDivider()

                                OutlinedTextField(
                                    value = businessName,
                                    onValueChange = { businessName = it },
                                    label = { Text("Store / Business Name *") },
                                    placeholder = { Text("e.g. Sharma General Store") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_business_name_input")
                                )

                                OutlinedTextField(
                                    value = ownerName,
                                    onValueChange = { ownerName = it },
                                    label = { Text("Store Owner Full Name") },
                                    placeholder = { Text("e.g. Rajesh Sharma") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_owner_name_input")
                                )

                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { phone = it },
                                    label = { Text("Owner Phone / WhatsApp Number") },
                                    placeholder = { Text("e.g. +91 98765 43210") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_phone_input")
                                )

                                OutlinedTextField(
                                    value = upiId,
                                    onValueChange = { upiId = it },
                                    label = { Text("Merchant UPI ID (for QR & Reminders)") },
                                    placeholder = { Text("e.g. myshop@okaxis or 9876543210@upi") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_upi_input")
                                )

                                OutlinedTextField(
                                    value = address,
                                    onValueChange = { address = it },
                                    label = { Text("Shop / Store Address") },
                                    placeholder = { Text("e.g. Main Market Road, City") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_address_input")
                                )

                                OutlinedTextField(
                                    value = gstin,
                                    onValueChange = { gstin = it.uppercase() },
                                    label = { Text("GSTIN (Optional)") },
                                    placeholder = { Text("e.g. 29ABCDE1234F1Z5") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_gstin_input")
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Buttons
                if (step == 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSkip,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Skip for Now")
                        }
                        Button(
                            onClick = { step = 1 },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("onboarding_next_button")
                        ) {
                            Text("Set Up Store Details", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { step = 0 },
                            modifier = Modifier
                                .weight(0.8f)
                                .height(48.dp)
                        ) {
                            Text("Back")
                        }
                        Button(
                            onClick = {
                                onCompleteSetup(
                                    initialProfile.copy(
                                        businessName = businessName.trim(),
                                        ownerName = ownerName.trim(),
                                        phone = phone.trim(),
                                        address = address.trim(),
                                        upiId = upiId.trim(),
                                        gstin = gstin.trim(),
                                        hasCompletedOnboarding = true
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                            modifier = Modifier
                                .weight(1.4f)
                                .height(48.dp)
                                .testTag("onboarding_save_start_button")
                        ) {
                            Text("Save & Start Khata", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstructionStepCard(
    stepNumber: String,
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNumber,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1565C0),
                    textAlign = TextAlign.Center
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color(0xFF1565C0),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
