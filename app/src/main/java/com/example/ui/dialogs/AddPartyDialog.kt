package com.example.ui.dialogs

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.model.BusinessProfile
import com.example.data.model.PartyType
import com.example.ui.theme.PrimaryNavy
import com.example.util.AppLanguage
import com.example.util.Localization
import com.example.util.PhoneUtil
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AddPartyDialog(
    initialType: PartyType = PartyType.CUSTOMER,
    profile: BusinessProfile = BusinessProfile(),
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
    val strings = remember(profile.languageCode) { Localization.getStrings(profile.languageCode) }

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var partyType by remember { mutableStateOf(initialType) }
    var address by remember { mutableStateOf("") }
    var gstin by remember { mutableStateOf("") }
    var openingBalanceText by remember { mutableStateOf("") }
    var isYouGaveOpening by remember { mutableStateOf(true) }
    var notifyOnWhatsApp by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

    // When phone has at least 10 digits, default notifyOnWhatsApp to ON
    LaunchedEffect(phone) {
        val normalized = PhoneUtil.normalize(phone)
        if (normalized.length >= 10 && !notifyOnWhatsApp) {
            notifyOnWhatsApp = true
        }
    }

    // Permission-free contact picker using ACTION_PICK on Phone.CONTENT_URI
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        runCatching {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val pickedName = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "" else ""
                    val pickedNumber = if (numIdx >= 0) cursor.getString(numIdx) ?: "" else ""
                    if (pickedName.isNotBlank()) {
                        name = pickedName
                        nameError = false
                    }
                    if (pickedNumber.isNotBlank()) {
                        phone = PhoneUtil.normalize(pickedNumber)
                        notifyOnWhatsApp = true
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (partyType == PartyType.CUSTOMER) strings.addCustomer else strings.addSupplier,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Contact Picker Button (Zero-Permission)
                OutlinedButton(
                    onClick = {
                        val pickIntent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                        contactPickerLauncher.launch(pickIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_from_contacts_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Contacts,
                        contentDescription = "Contacts",
                        tint = PrimaryNavy,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select from Contacts", style = MaterialTheme.typography.labelMedium)
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
                    label = { Text("Mobile Number") },
                    placeholder = { Text("e.g. 9876543210") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("party_phone_input")
                )

                // Switch: Notify on WhatsApp (default ON when phone entered)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.notifyWhatsApp,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Send welcome WhatsApp message after saving",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = notifyOnWhatsApp,
                        onCheckedChange = { notifyOnWhatsApp = it },
                        modifier = Modifier.testTag("notify_whatsapp_switch")
                    )
                }

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
                    label = { Text("Opening Balance (${profile.currencySymbol.ifBlank { "₹" }}) (Optional)") },
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
                    val normalizedPhone = PhoneUtil.normalize(phone)

                    // Execute save
                    onSave(
                        name.trim(),
                        normalizedPhone,
                        partyType,
                        address.trim(),
                        gstin.trim(),
                        "",
                        openingAmt,
                        isYouGaveOpening
                    )

                    // If notifyOnWhatsApp is ON and phone is provided, send notification
                    if (notifyOnWhatsApp && normalizedPhone.isNotBlank()) {
                        val currency = profile.currencySymbol.ifBlank { "₹" }
                        val fmt = NumberFormat.getNumberInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()).apply {
                            maximumFractionDigits = 0
                        }
                        val formattedBal = "$currency${fmt.format(openingAmt)}"
                        val businessName = profile.businessName.ifBlank { "Ghanshyam Kids Wear" }

                        val message = when (AppLanguage.fromCode(profile.languageCode)) {
                            AppLanguage.HINDI -> "नमस्ते $name जी, आपको $businessName के खाते में जोड़ दिया गया है। शुरुआती बैलेंस: $formattedBal। - $businessName"
                            AppLanguage.GUJARATI -> "નમસ્તે $name જી, તમને $businessName ના ખાતામાં ઉમેરવામાં આવ્યા છે. શરૂઆતની બાકી: $formattedBal. - $businessName"
                            else -> "Namaste $name ji, you have been added to $businessName's khata. Opening balance: $formattedBal. - $businessName"
                        }

                        val waNumber = PhoneUtil.toWhatsAppNumber(normalizedPhone)
                        val encoded = URLEncoder.encode(message, "UTF-8")
                        val waUri = Uri.parse("https://wa.me/$waNumber?text=$encoded")
                        val waIntent = Intent(Intent.ACTION_VIEW, waUri)

                        try {
                            context.startActivity(waIntent)
                        } catch (e: Exception) {
                            val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${PhoneUtil.toDisplay(normalizedPhone)}")).apply {
                                putExtra("sms_body", message)
                            }
                            runCatching { context.startActivity(smsIntent) }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                modifier = Modifier.testTag("save_party_button")
            ) {
                Text(strings.addParty, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
