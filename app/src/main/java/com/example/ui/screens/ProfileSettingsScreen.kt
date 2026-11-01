package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessProfile

@Composable
fun ProfileSettingsScreen(
    profile: BusinessProfile,
    onSaveProfile: (BusinessProfile) -> Unit,
    onOpenBusinessCard: () -> Unit,
    onOpenQrStandee: () -> Unit,
    onOpenCloudSync: () -> Unit,
    onOpenImportCsv: () -> Unit,
    onOpenLanguageSelector: () -> Unit,
    onLockAppNow: () -> Unit,
    onOpenAppGuide: () -> Unit = {}
) {
    var businessName by remember(profile) { mutableStateOf(profile.businessName) }
    var ownerName by remember(profile) { mutableStateOf(profile.ownerName) }
    var phone by remember(profile) { mutableStateOf(profile.phone) }
    var email by remember(profile) { mutableStateOf(profile.email) }
    var address by remember(profile) { mutableStateOf(profile.address) }
    var gstin by remember(profile) { mutableStateOf(profile.gstin) }
    var upiId by remember(profile) { mutableStateOf(profile.upiId) }
    var tagline by remember(profile) { mutableStateOf(profile.tagline) }
    var appPin by remember(profile) { mutableStateOf(profile.appPin) }
    var biometricEnabled by remember(profile) { mutableStateOf(profile.biometricEnabled) }
    var savedBanner by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "More & Store Profile",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Store Owner Details Input Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Store & Owner Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Fill in your store owner name, address, phone, and UPI ID below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it; savedBanner = false },
                        label = { Text("Business / Store Name") },
                        placeholder = { Text("Enter your store name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_business_name_input")
                    )

                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it; savedBanner = false },
                        label = { Text("Store Owner / Proprietor Name") },
                        placeholder = { Text("Enter owner full name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_owner_name_input")
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it; savedBanner = false },
                        label = { Text("Owner Phone / WhatsApp Number") },
                        placeholder = { Text("e.g. +91 98765 43210") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_phone_input")
                    )

                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { upiId = it; savedBanner = false },
                        label = { Text("Merchant UPI ID (for QR & Reminders)") },
                        placeholder = { Text("e.g. storename@upi") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_upi_input")
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it; savedBanner = false },
                        label = { Text("Store Address") },
                        placeholder = { Text("Enter shop / market address") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_address_input")
                    )

                    OutlinedTextField(
                        value = gstin,
                        onValueChange = { gstin = it.uppercase(); savedBanner = false },
                        label = { Text("GSTIN Number (Optional)") },
                        placeholder = { Text("e.g. 29ABCDE1234F1Z5") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; savedBanner = false },
                        label = { Text("Business Email (Optional)") },
                        placeholder = { Text("e.g. owner@store.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tagline,
                        onValueChange = { tagline = it; savedBanner = false },
                        label = { Text("Store Tagline") },
                        placeholder = { Text("e.g. Wholesale & Retail Store") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (savedBanner) {
                        Text(
                            text = "Store owner details saved!",
                            color = Color(0xFF2E7D32),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            onSaveProfile(
                                profile.copy(
                                    businessName = businessName.trim(),
                                    ownerName = ownerName.trim(),
                                    phone = phone.trim(),
                                    email = email.trim(),
                                    address = address.trim(),
                                    gstin = gstin.trim(),
                                    upiId = upiId.trim(),
                                    tagline = tagline.trim(),
                                    appPin = appPin.trim(),
                                    biometricEnabled = biometricEnabled,
                                    hasCompletedOnboarding = true
                                )
                            )
                            savedBanner = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_profile_button")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Save")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Store Profile", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Merchant Quick Utilities Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Business Tools & Help",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onOpenBusinessCard,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = "Visiting Card",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Visiting Card")
                        }
                        FilledTonalButton(
                            onClick = onOpenQrStandee,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = "QR Standee",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("QR Standee")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onOpenCloudSync,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Cloud Sync",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Backup / Sync")
                        }
                        FilledTonalButton(
                            onClick = onOpenImportCsv,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = "Import CSV",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import CSV")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onOpenLanguageSelector,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Language",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Language (${profile.languageCode.uppercase()})")
                        }
                        FilledTonalButton(
                            onClick = onOpenAppGuide,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_open_guide_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "App Guide",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("How to Use")
                        }
                    }
                }
            }

            // Security & App Lock Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "App Lock & PIN",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = appPin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                appPin = it
                                savedBanner = false
                            }
                        },
                        label = { Text("4-Digit App Lock PIN (Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_pin_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enable Biometric Unlock",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = {
                                biometricEnabled = it
                                savedBanner = false
                            }
                        )
                    }

                    if (appPin.length == 4) {
                        FilledTonalButton(
                            onClick = onLockAppNow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock App")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lock App Now")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
