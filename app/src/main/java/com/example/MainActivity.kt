package com.example

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.data.model.PaymentMode
import com.example.data.model.TransactionType
import com.example.ui.components.AppLockScreen
import com.example.ui.dialogs.AddPartyDialog
import com.example.ui.dialogs.BillGeneratorDialog
import com.example.ui.dialogs.BusinessCardDialog
import com.example.ui.dialogs.CloudSyncDialog
import com.example.ui.dialogs.KhatabookImportDialog
import com.example.ui.dialogs.LanguageSelectionDialog
import com.example.ui.dialogs.MonthlyReportDialog
import com.example.ui.dialogs.QrStandeeDialog
import com.example.ui.dialogs.ReminderDialog
import com.example.ui.dialogs.StoreOwnerSetupDialog
import com.example.ui.screens.CashbookScreen
import com.example.ui.screens.ChatbotScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PartyDetailScreen
import com.example.ui.screens.ProfileSettingsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryNavy
import com.example.ui.viewmodel.KhataViewModel
import com.example.util.BiometricAuthManager
import com.example.util.Localization
import kotlinx.coroutines.launch

enum class KirioTab {
    PARTIES,  // Matches "Parties" tab in user photo
    BILLS,    // Matches "Bills" tab in user photo
    ITEMS,    // Matches "Items / AI Munshi" tab in user photo
    MORE      // Matches "More" tab in user photo
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                KirioMainApp()
            }
        }
    }
}

@Composable
fun KirioMainApp(viewModel: KhataViewModel = viewModel()) {
    val profile by viewModel.businessProfile.collectAsState()
    val allParties by viewModel.allParties.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val cashbookTransactions by viewModel.cashbookTransactions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    val syncStatusMessage by viewModel.syncStatusMessage.collectAsState()

    val strings = remember(profile.languageCode) {
        Localization.getStrings(profile.languageCode)
    }

    var currentTab by remember { mutableStateOf(KirioTab.PARTIES) }
    var selectedPartyId by remember { mutableStateOf<Long?>(null) }
    var selectedPartyTypeInTab by remember { mutableStateOf(PartyType.CUSTOMER) }

    // Manual override to view Onboarding / App Guide anytime
    var showManualOnboardingGuide by remember { mutableStateOf(false) }

    // Dialog states
    var showAddPartyDialogType by remember { mutableStateOf<PartyType?>(null) }
    var showBillGeneratorDialog by remember { mutableStateOf(false) }
    var showQrStandeeDialog by remember { mutableStateOf(false) }
    var showPdfReportDialog by remember { mutableStateOf(false) }
    var showBusinessCardDialog by remember { mutableStateOf(false) }
    var showCloudSyncDialog by remember { mutableStateOf(false) }
    var showImportCsvDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showStoreOwnerDialog by remember { mutableStateOf(false) }
    var showCashbookModal by remember { mutableStateOf(false) }
    var reminderTargetParty by remember { mutableStateOf<Party?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val json = viewModel.getBackupJsonString()
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(json.toByteArray(Charsets.UTF_8))
                }
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Backup file saved successfully!")
                }
            }.onFailure { err ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Failed to export backup: ${err.localizedMessage}")
                }
            }
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                }
                if (!json.isNullOrBlank()) {
                    viewModel.restoreFromBackupJsonString(json) { _, msg ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(msg)
                        }
                    }
                }
            }.onFailure { err ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Failed to read backup file: ${err.localizedMessage}")
                }
            }
        }
    }

    // App Lock Check
    if (BiometricAuthManager.isPinConfigured(profile.appPin) && !isAppUnlocked) {
        AppLockScreen(
            businessName = profile.businessName.ifBlank { "Kirio Khata" },
            savedPin = profile.appPin,
            biometricEnabled = profile.biometricEnabled,
            onUnlocked = { viewModel.unlockApp() },
            onMigrateLegacyPin = { hashed ->
                viewModel.saveBusinessProfile(profile.copy(appPin = hashed))
            }
        )
        return
    }

    // First-Time Startup Intro, Instructions & Store Owner Setup Screen
    if (!profile.hasCompletedOnboarding || showManualOnboardingGuide) {
        BackHandler(enabled = showManualOnboardingGuide) {
            showManualOnboardingGuide = false
        }
        OnboardingScreen(
            initialProfile = profile,
            onCompleteSetup = { updatedProfile ->
                viewModel.saveBusinessProfile(updatedProfile)
                showManualOnboardingGuide = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Store owner profile saved!")
                }
            },
            onSkip = {
                viewModel.skipOnboarding()
                showManualOnboardingGuide = false
            }
        )
        return
    }

    // Active Party Detail Screen
    val activeParty = remember(allParties, selectedPartyId) {
        allParties.find { it.id == selectedPartyId }
    }

    if (activeParty != null) {
        val partyTxs by viewModel.getTransactionsForParty(activeParty.id)
            .collectAsState(initial = emptyList())

        PartyDetailScreen(
            party = activeParty,
            transactions = partyTxs,
            profile = profile,
            onBack = { selectedPartyId = null },
            onAddTransaction = { amt, type, mode, desc, bill, addToCashbook ->
                viewModel.addPartyTransaction(
                    partyId = activeParty.id,
                    amount = amt,
                    type = type,
                    paymentMode = mode,
                    description = desc,
                    billNumber = bill,
                    addToCashbook = addToCashbook
                )
            },
            onMarkReceived = { bill, amt, mode, time, addToCashbook ->
                viewModel.recordReceivedPayment(
                    billId = bill.id,
                    amount = amt,
                    paymentMode = mode,
                    timestamp = time,
                    addToCashbook = addToCashbook
                )
            },
            onUndoReceived = { bill ->
                viewModel.undoReceivedPayment(bill.id)
            },
            onMultiBillReceived = { billIds, mode, addToCashbook ->
                viewModel.recordMultiBillPayment(billIds, mode, addToCashbook)
            },
            onToggleCompleted = { tx -> viewModel.toggleCompleted(tx) },
            onEditTransaction = { tx, amt, desc, time, mode, type, bill ->
                viewModel.updateTransactionEntry(tx, amt, desc, time, mode, type, bill)
            },
            onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
            onDeleteParty = { party ->
                viewModel.deleteParty(party) {
                    selectedPartyId = null
                }
            },
            onSendReminder = { party -> reminderTargetParty = party }
        )
    } else {
        BackHandler(enabled = currentTab != KirioTab.PARTIES || showCashbookModal) {
            if (showCashbookModal) {
                showCashbookModal = false
            } else {
                currentTab = KirioTab.PARTIES
            }
        }

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                // Exact bottom navigation bar matching user's photo:
                // [ Parties | Bills | Items / AI | More ]
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == KirioTab.PARTIES,
                        onClick = {
                            currentTab = KirioTab.PARTIES
                            showCashbookModal = false
                        },
                        icon = { Icon(Icons.Default.People, contentDescription = strings.partiesTab) },
                        label = { Text(strings.partiesTab) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryNavy,
                            selectedTextColor = PrimaryNavy,
                            indicatorColor = Color(0xFFD1E4FF)
                        ),
                        modifier = Modifier.testTag("nav_tab_parties")
                    )
                    NavigationBarItem(
                        selected = currentTab == KirioTab.BILLS,
                        onClick = {
                            currentTab = KirioTab.BILLS
                            showCashbookModal = false
                        },
                        icon = { Icon(Icons.Default.Receipt, contentDescription = strings.billsTab) },
                        label = { Text(strings.billsTab) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryNavy,
                            selectedTextColor = PrimaryNavy,
                            indicatorColor = Color(0xFFD1E4FF)
                        ),
                        modifier = Modifier.testTag("nav_tab_bills")
                    )
                    NavigationBarItem(
                        selected = currentTab == KirioTab.ITEMS,
                        onClick = {
                            currentTab = KirioTab.ITEMS
                            showCashbookModal = false
                        },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Munshi") },
                        label = { Text("AI Munshi") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryNavy,
                            selectedTextColor = PrimaryNavy,
                            indicatorColor = Color(0xFFD1E4FF)
                        ),
                        modifier = Modifier.testTag("nav_tab_items")
                    )
                    NavigationBarItem(
                        selected = currentTab == KirioTab.MORE,
                        onClick = {
                            currentTab = KirioTab.MORE
                            showCashbookModal = false
                        },
                        icon = { Icon(Icons.Default.MoreHoriz, contentDescription = strings.moreTab) },
                        label = { Text(strings.moreTab) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryNavy,
                            selectedTextColor = PrimaryNavy,
                            indicatorColor = Color(0xFFD1E4FF)
                        ),
                        modifier = Modifier.testTag("nav_tab_more")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showCashbookModal) {
                    CashbookScreen(
                        cashbookEntries = cashbookTransactions,
                        currencySymbol = profile.currencySymbol.ifBlank { "₹" },
                        onAddCashbookEntry = { amt, isIn, mode, desc, partyName, bill ->
                            viewModel.addCashbookEntry(amt, isIn, mode, desc, partyName, bill)
                        },
                        onDeleteEntry = viewModel::deleteTransaction
                    )
                } else {
                    when (currentTab) {
                        KirioTab.PARTIES -> {
                            // Filter parties by selected tab: CUSTOMER or SUPPLIER
                            val tabParties = allParties.filter { it.type == selectedPartyTypeInTab }
                            val filtered = tabParties.filter { party ->
                                searchQuery.isBlank() ||
                                    party.name.contains(searchQuery, ignoreCase = true) ||
                                    party.phone.contains(searchQuery, ignoreCase = true)
                            }

                            HomeScreen(
                                profile = profile,
                                allParties = allParties,
                                filteredParties = filtered,
                                searchQuery = searchQuery,
                                selectedPartyType = selectedPartyTypeInTab,
                                strings = strings,
                                onSearchQueryChange = viewModel::setSearchQuery,
                                onPartyTypeSelected = { selectedPartyTypeInTab = it },
                                onPartyClick = { party -> selectedPartyId = party.id },
                                onRemindParty = { party -> reminderTargetParty = party },
                                onAddPartyClick = { type -> showAddPartyDialogType = type },
                                onOpenCashbook = { showCashbookModal = true },
                                onPdfReportClick = { showPdfReportDialog = true },
                                onLanguageClick = { showLanguageDialog = true },
                                onSetupStoreDetailsClick = { showStoreOwnerDialog = true }
                            )
                        }

                        KirioTab.BILLS -> {
                            ReportsScreen(
                                profile = profile,
                                parties = allParties,
                                transactions = allTransactions,
                                onDownloadPdfClick = { showPdfReportDialog = true },
                                onDeleteTransaction = viewModel::deleteTransaction
                            )
                        }

                        KirioTab.ITEMS -> {
                            ChatbotScreen(
                                messages = chatMessages,
                                isLoading = isChatLoading,
                                onSendMessage = { text ->
                                    viewModel.sendChatMessage(text) { note ->
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(note)
                                        }
                                    }
                                },
                                onClearChat = viewModel::clearChatHistory
                            )
                        }

                        KirioTab.MORE -> {
                            ProfileSettingsScreen(
                                profile = profile,
                                onSaveProfile = { updated ->
                                    viewModel.saveBusinessProfile(updated)
                                    if (BiometricAuthManager.isPinConfigured(updated.appPin)) {
                                        viewModel.unlockApp()
                                    }
                                },
                                onOpenBusinessCard = { showBusinessCardDialog = true },
                                onOpenQrStandee = { showQrStandeeDialog = true },
                                onOpenCloudSync = { showCloudSyncDialog = true },
                                onOpenImportCsv = { showImportCsvDialog = true },
                                onOpenLanguageSelector = { showLanguageDialog = true },
                                onLockAppNow = { viewModel.lockApp() },
                                onOpenAppGuide = { showManualOnboardingGuide = true }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showStoreOwnerDialog) {
        StoreOwnerSetupDialog(
            profile = profile,
            onDismiss = { showStoreOwnerDialog = false },
            onSave = { updated ->
                viewModel.saveBusinessProfile(updated)
                showStoreOwnerDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Store details updated!")
                }
            }
        )
    }

    if (showAddPartyDialogType != null) {
        AddPartyDialog(
            initialType = showAddPartyDialogType!!,
            profile = profile,
            onDismiss = { showAddPartyDialogType = null },
            onSave = { name, phone, type, addr, gst, notes, openBal, isGave ->
                viewModel.addParty(name, phone, type, addr, gst, notes, openBal, isGave) {
                    showAddPartyDialogType = null
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Added $name!")
                    }
                }
            }
        )
    }

    if (showBillGeneratorDialog) {
        BillGeneratorDialog(
            profile = profile,
            parties = allParties,
            onDismiss = { showBillGeneratorDialog = false },
            onRecordBillToKhata = { partyId, amt, billNo, summary, isCredit ->
                if (isCredit && partyId != null) {
                    viewModel.addPartyTransaction(
                        partyId = partyId,
                        amount = amt,
                        type = TransactionType.YOU_GAVE,
                        paymentMode = PaymentMode.CASH,
                        description = "Bill $billNo",
                        billNumber = billNo,
                        itemsSummary = summary
                    )
                } else {
                    viewModel.addCashbookEntry(
                        amount = amt,
                        isCashIn = true,
                        paymentMode = PaymentMode.CASH,
                        description = "Cash Bill $billNo ($summary)",
                        partyName = allParties.find { it.id == partyId }?.name ?: "Cash Customer",
                        billNumber = billNo
                    )
                }
                showBillGeneratorDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Bill $billNo recorded!")
                }
            }
        )
    }

    if (showQrStandeeDialog) {
        QrStandeeDialog(
            profile = profile,
            onDismiss = { showQrStandeeDialog = false },
            onSaveUpiId = { newUpi ->
                viewModel.saveBusinessProfile(profile.copy(upiId = newUpi))
            }
        )
    }

    if (showPdfReportDialog) {
        MonthlyReportDialog(
            profile = profile,
            parties = allParties,
            transactions = allTransactions,
            onDismiss = { showPdfReportDialog = false }
        )
    }

    if (showBusinessCardDialog) {
        BusinessCardDialog(
            profile = profile,
            onDismiss = { showBusinessCardDialog = false }
        )
    }

    if (showCloudSyncDialog) {
        CloudSyncDialog(
            profile = profile,
            partiesCount = allParties.size,
            transactionsCount = allTransactions.size,
            syncStatusMessage = syncStatusMessage,
            onBackupNow = { viewModel.performCloudBackup() },
            onRestoreNow = { viewModel.performCloudRestore() },
            onExportFile = { exportFileLauncher.launch("kirio_backup_${System.currentTimeMillis()}.json") },
            onImportFile = { importFileLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
            onDismiss = { showCloudSyncDialog = false }
        )
    }

    if (showImportCsvDialog) {
        KhatabookImportDialog(
            onDismiss = { showImportCsvDialog = false },
            onImportRecords = { records ->
                viewModel.importKhatabookRecords(records) { count ->
                    showImportCsvDialog = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Imported $count entries!")
                    }
                }
            }
        )
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguageCode = profile.languageCode,
            onSelectLanguage = { code -> viewModel.updateLanguage(code) },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (reminderTargetParty != null) {
        ReminderDialog(
            party = reminderTargetParty!!,
            profile = profile,
            onDismiss = { reminderTargetParty = null }
        )
    }
}
