package com.example.ui.dialogs

import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.PartyType
import com.example.ui.theme.PrimaryNavy

@Composable
fun AddPartyDialog(
    initialType: PartyType = PartyType.CUSTOMER,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        phone: String,
        type: PartyType,
        address: String,
        gstin: String,
        notes: String,
        openingBalance: Double,
        isYouGaveOpening: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var partyType by remember { mutableStateOf(initialType) }
    var address by remember { mutableStateOf("") }
    var gstin by remember { mutableStateOf("") }
    var openingBalanceText by remember { mutableStateOf("") }
    var isYouGaveOpening by remember { mutableStateOf(true) }
    var nameError by remember { mutableStateOf(false) }

    // Contact Picker Launcher
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { contactUri ->
        contactUri?.let { uri ->
            runCatching {
                val projection = arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.HAS_PHONE_NUMBER
                )
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                        val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                        val pickedName = cursor.getString(nameIndex) ?: ""
                        name = pickedName
                        nameError = false

                        val hasPhone = cursor.getInt(hasPhoneIndex) > 0
                        val contactId = cursor.getString(idIndex)

                        if (hasPhone) {
                            context.contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                                arrayOf(contactId),
                                null
                            )?.use { phoneCursor ->
                                if (phoneCursor.moveToFirst()) {
                                    val number = phoneCursor.getString(0) ?: ""
                                    phone = number.replace("[^0-9+]".toRegex(), "")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (partyType == PartyType.CUSTOMER) "Add New Customer" else "Add New Supplier",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Quick Import Buttons: Contacts & WhatsApp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { contactPickerLauncher.launch(null) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_from_contacts_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Contacts,
                            contentDescription = "Contacts",
                            tint = PrimaryNavy,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add from Contacts", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Hello! Please join my Kirio Khata store ledger.")
                                setPackage("com.whatsapp")
                            }
                            runCatching { context.startActivity(intent) }.onFailure {
                                val fallback = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Hello from Kirio Khata!")
                                }
                                context.startActivity(Intent.createChooser(fallback, "Share via"))
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_whatsapp_party_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "WhatsApp",
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WhatsApp", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = partyType == PartyType.CUSTOMER,
                        onClick = { partyType = PartyType.CUSTOMER },
                        label = { Text("Customer") },
                        modifier = Modifier.testTag("party_type_customer")
                    )
                    FilterChip(
                        selected = partyType == PartyType.SUPPLIER,
                        onClick = { partyType = PartyType.SUPPLIER },
                        label = { Text("Supplier") },
                        modifier = Modifier.testTag("party_type_supplier")
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("Customer / Supplier Name *") },
                    placeholder = { Text("e.g. Ramesh Kumar") },
                    isError = nameError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("party_name_input")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Number (WhatsApp/SMS)") },
                    placeholder = { Text("e.g. +91 98765 43210") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("party_phone_input")
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address / City (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = gstin,
                    onValueChange = { gstin = it.uppercase() },
                    label = { Text("GSTIN (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = openingBalanceText,
                    onValueChange = { openingBalanceText = it },
                    label = { Text("Opening Balance (₹) (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("party_opening_balance_input")
                )

                if ((openingBalanceText.toDoubleOrNull() ?: 0.0) > 0.0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = isYouGaveOpening,
                            onClick = { isYouGaveOpening = true },
                            label = { Text("You Gave (To Collect)") }
                        )
                        FilterChip(
                            selected = !isYouGaveOpening,
                            onClick = { isYouGaveOpening = false },
                            label = { Text("You Got (To Pay)") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    val openingAmt = openingBalanceText.toDoubleOrNull() ?: 0.0
                    onSave(
                        name,
                        phone,
                        partyType,
                        address,
                        gstin,
                        "",
                        openingAmt,
                        isYouGaveOpening
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                modifier = Modifier.testTag("save_party_button")
            ) {
                Text("Save Party", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
