package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.ChatMessage
import com.example.data.ai.GeminiChatService
import com.example.data.db.AppDatabase
import com.example.data.model.BusinessProfile
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.data.model.PaymentMode
import com.example.data.model.TransactionEntry
import com.example.data.model.TransactionType
import com.example.data.repository.FirestoreSyncRepository
import com.example.data.repository.KhataRepository
import com.example.data.repository.SyncSnapshotResult
import com.example.util.ImportedKhataRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PartyFilter {
    ALL,
    CUSTOMERS,
    SUPPLIERS,
    RECEIVABLES,
    PAYABLES
}

class KhataViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = KhataRepository(
        database = database,
        businessProfileDao = database.businessProfileDao(),
        partyDao = database.partyDao(),
        transactionDao = database.transactionDao()
    )
    private val syncRepository = FirestoreSyncRepository(application)
    private val geminiChatService = GeminiChatService()

    val businessProfile: StateFlow<BusinessProfile> = repository.businessProfile
        .combine(MutableStateFlow(BusinessProfile())) { saved, fallback -> saved ?: fallback }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BusinessProfile()
        )

    val allParties: StateFlow<List<Party>> = repository.allParties
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allTransactions: StateFlow<List<TransactionEntry>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val cashbookTransactions: StateFlow<List<TransactionEntry>> = repository.cashbookTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _partyFilter = MutableStateFlow(PartyFilter.ALL)
    val partyFilter: StateFlow<PartyFilter> = _partyFilter.asStateFlow()

    val filteredParties: StateFlow<List<Party>> = combine(
        allParties,
        _searchQuery,
        _partyFilter
    ) { parties, query, filter ->
        parties.filter { party ->
            val matchesQuery = query.isBlank() ||
                party.name.contains(query, ignoreCase = true) ||
                party.phone.contains(query, ignoreCase = true) ||
                party.notes.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                PartyFilter.ALL -> true
                PartyFilter.CUSTOMERS -> party.type == PartyType.CUSTOMER
                PartyFilter.SUPPLIERS -> party.type == PartyType.SUPPLIER
                PartyFilter.RECEIVABLES -> party.balance > 0
                PartyFilter.PAYABLES -> party.balance < 0
            }
            matchesQuery && matchesFilter
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isAppUnlocked = MutableStateFlow(false)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "Namaste! I am your **Kirio AI Munshi**.\nAsk me about your pending collections, cashbook balance, top customers, or ask me to draft a WhatsApp payment reminder!",
                isFromUser = false
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    private val _syncStatusMessage = MutableStateFlow<String?>(null)
    val syncStatusMessage: StateFlow<String?> = _syncStatusMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultDataSeeded()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setPartyFilter(filter: PartyFilter) {
        _partyFilter.value = filter
    }

    fun unlockApp() {
        _isAppUnlocked.value = true
    }

    fun lockApp() {
        _isAppUnlocked.value = false
    }

    fun getPartyById(partyId: Long) = repository.getPartyById(partyId)

    fun getTransactionsForParty(partyId: Long) = repository.getTransactionsForParty(partyId)

    fun addParty(
        name: String,
        phone: String,
        type: PartyType,
        address: String = "",
        gstin: String = "",
        notes: String = "",
        openingBalance: Double = 0.0,
        isYouGaveOpening: Boolean = true,
        onCreated: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val id = repository.addParty(
                name = name,
                phone = phone,
                type = type,
                address = address,
                gstin = gstin,
                notes = notes,
                openingBalance = openingBalance,
                isYouGaveOpening = isYouGaveOpening
            )
            onCreated(id)
        }
    }

    fun updateParty(party: Party) {
        viewModelScope.launch {
            repository.updateParty(party)
        }
    }

    fun deleteParty(party: Party, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteParty(party)
            onDeleted()
        }
    }

    fun addPartyTransaction(
        partyId: Long,
        amount: Double,
        type: TransactionType,
        paymentMode: PaymentMode,
        description: String,
        billNumber: String = "",
        itemsSummary: String = ""
    ) {
        viewModelScope.launch {
            repository.addPartyTransaction(
                partyId = partyId,
                amount = amount,
                type = type,
                paymentMode = paymentMode,
                description = description,
                billNumber = billNumber,
                itemsSummary = itemsSummary
            )
        }
    }

    fun addCashbookEntry(
        amount: Double,
        isCashIn: Boolean,
        paymentMode: PaymentMode,
        description: String,
        partyName: String = "",
        billNumber: String = ""
    ) {
        viewModelScope.launch {
            repository.addCashbookEntry(
                amount = amount,
                isCashIn = isCashIn,
                paymentMode = paymentMode,
                description = description,
                partyName = partyName,
                billNumber = billNumber
            )
        }
    }

    fun toggleConfirmGave(entry: TransactionEntry) {
        viewModelScope.launch {
            repository.updateTransaction(entry.copy(isConfirmedGave = !entry.isConfirmedGave))
        }
    }

    fun toggleConfirmTaken(entry: TransactionEntry) {
        viewModelScope.launch {
            repository.updateTransaction(entry.copy(isConfirmedTaken = !entry.isConfirmedTaken))
        }
    }

    fun toggleCompleted(entry: TransactionEntry) {
        viewModelScope.launch {
            repository.updateTransaction(entry.copy(isCompleted = !entry.isCompleted))
        }
    }

    fun updateTransactionEntry(
        entry: TransactionEntry,
        amount: Double,
        description: String,
        timestamp: Long,
        paymentMode: PaymentMode,
        type: TransactionType = entry.type,
        billNumber: String = entry.billNumber
    ) {
        viewModelScope.launch {
            repository.updateTransaction(
                entry.copy(
                    amount = amount,
                    description = description.trim(),
                    timestamp = timestamp,
                    paymentMode = paymentMode,
                    type = type,
                    billNumber = billNumber.trim()
                )
            )
        }
    }

    fun deleteTransaction(entry: TransactionEntry) {
        viewModelScope.launch {
            repository.deleteTransaction(entry)
        }
    }

    fun updatePartyDueDate(partyId: Long, dueDateTimestamp: Long?) {
        viewModelScope.launch {
            repository.updatePartyDueDate(partyId, dueDateTimestamp)
        }
    }

    fun saveBusinessProfile(profile: BusinessProfile) {
        viewModelScope.launch {
            repository.saveBusinessProfile(profile)
        }
    }

    fun skipOnboarding() {
        viewModelScope.launch {
            val current = businessProfile.value
            repository.saveBusinessProfile(current.copy(hasCompletedOnboarding = true))
        }
    }

    fun updateLanguage(languageCode: String) {
        viewModelScope.launch {
            val current = businessProfile.value
            repository.saveBusinessProfile(current.copy(languageCode = languageCode))
        }
    }

    fun importKhatabookRecords(records: List<ImportedKhataRecord>, onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val existing = allParties.value.associateBy { it.name.lowercase() }.toMutableMap()
            var importedCount = 0
            for (rec in records) {
                val key = rec.partyName.trim().lowercase()
                val existingParty = existing[key]
                val partyId = if (existingParty != null) {
                    existingParty.id
                } else {
                    val newId = repository.addParty(
                        name = rec.partyName,
                        phone = rec.phone,
                        type = rec.partyType
                    )
                    val created = repository.getPartyById(newId).first()
                    if (created != null) {
                        existing[key] = created
                    }
                    newId
                }
                repository.addPartyTransaction(
                    partyId = partyId,
                    amount = rec.amount,
                    type = if (rec.isYouGave) TransactionType.YOU_GAVE else TransactionType.YOU_GOT,
                    paymentMode = rec.paymentMode,
                    description = rec.description,
                    billNumber = "IMP-${importedCount + 1}"
                )
                importedCount++
            }
            onComplete(importedCount)
        }
    }

    fun sendChatMessage(userText: String, onPartyAutoAdded: ((String) -> Unit)? = null) {
        if (userText.isBlank() || _isChatLoading.value) return
        val userMsg = ChatMessage(text = userText.trim(), isFromUser = true)
        val priorHistory = _chatMessages.value
        _chatMessages.value = priorHistory + userMsg
        _isChatLoading.value = true

        viewModelScope.launch {
            val result = geminiChatService.askBusinessAdvisor(
                userMessage = userText.trim(),
                profile = businessProfile.value,
                parties = allParties.value,
                transactions = allTransactions.value,
                history = priorHistory,
                languageCode = businessProfile.value.languageCode
            )

            // If AI extracted a customer/supplier to add automatically:
            if (!result.addPartyName.isNullOrBlank()) {
                val newPartyType = result.addPartyType ?: PartyType.CUSTOMER
                val openBal = result.addPartyOpeningBalance ?: 0.0
                addParty(
                    name = result.addPartyName,
                    phone = result.addPartyPhone.orEmpty(),
                    type = newPartyType,
                    openingBalance = openBal,
                    isYouGaveOpening = result.isYouGave
                ) { newId ->
                    onPartyAutoAdded?.invoke("${result.addPartyName} added automatically!")
                }
            }

            _chatMessages.value = _chatMessages.value + ChatMessage(text = result.replyText, isFromUser = false)
            _isChatLoading.value = false
        }
    }

    fun clearChatHistory() {
        _chatMessages.value = listOf(
            ChatMessage(
                text = "Chat cleared! Ask me anything about your **${businessProfile.value.businessName}** ledger, receivables, or cashflow.",
                isFromUser = false
            )
        )
    }

    fun performCloudBackup(onResult: (SyncSnapshotResult) -> Unit = {}) {
        viewModelScope.launch {
            val res = syncRepository.backupToCloud(
                profile = businessProfile.value,
                parties = allParties.value,
                transactions = allTransactions.value
            )
            if (res.success) {
                repository.saveBusinessProfile(
                    businessProfile.value.copy(lastSyncedAt = res.syncedTimestamp)
                )
            }
            _syncStatusMessage.value = res.message
            onResult(res)
        }
    }

    fun performCloudRestore(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val snapshot = syncRepository.restoreFromCloud()
            if (snapshot == null) {
                val msg = "No saved backup snapshot found yet. Tap 'Backup Now' first."
                _syncStatusMessage.value = msg
                onResult(false, msg)
            } else {
                val (prof, parties, txs) = snapshot
                repository.restoreFullSnapshot(prof, parties, txs)
                val msg = "Restored ${parties.size} parties and ${txs.size} transactions!"
                _syncStatusMessage.value = msg
                onResult(true, msg)
            }
        }
    }

    fun getBackupJsonString(): String {
        return syncRepository.createFullBackupJson(
            profile = businessProfile.value,
            parties = allParties.value,
            transactions = allTransactions.value
        )
    }

    fun restoreFromBackupJsonString(jsonString: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val snapshot = syncRepository.parseBackupJson(jsonString)
            if (snapshot == null) {
                val msg = "Invalid or corrupted backup file."
                _syncStatusMessage.value = msg
                onResult(false, msg)
            } else {
                val (prof, parties, txs) = snapshot
                repository.restoreFullSnapshot(prof, parties, txs)
                val msg = "Successfully restored ${parties.size} parties and ${txs.size} entries!"
                _syncStatusMessage.value = msg
                onResult(true, msg)
            }
        }
    }

    fun hasCloudSnapshot(): Boolean = syncRepository.hasSavedSnapshot()
}
