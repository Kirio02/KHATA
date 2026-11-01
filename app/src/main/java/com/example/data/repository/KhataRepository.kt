package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.dao.BusinessProfileDao
import com.example.data.dao.PartyDao
import com.example.data.dao.TransactionDao
import com.example.data.db.AppDatabase
import com.example.data.model.BusinessProfile
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.data.model.PaymentMode
import com.example.data.model.TransactionEntry
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

/**
 * Pure function to compute the running balance for each transaction of a party.
 * Sorts entries oldest -> newest by timestamp and id, accumulates:
 * YOU_GAVE adds (debit/credit given), YOU_GOT subtracts (payment received).
 * Returns a map from transaction id to running balance after that transaction.
 */
fun computeRunningBalances(entries: List<TransactionEntry>): Map<Long, Double> {
    val sorted = entries.sortedWith(compareBy({ it.timestamp }, { it.id }))
    val balances = mutableMapOf<Long, Double>()
    var running = 0.0
    for (entry in sorted) {
        when (entry.type) {
            TransactionType.YOU_GAVE -> running += entry.amount
            TransactionType.YOU_GOT -> running -= entry.amount
            else -> {}
        }
        balances[entry.id] = running
    }
    return balances
}

class KhataRepository(
    private val database: AppDatabase,
    private val businessProfileDao: BusinessProfileDao,
    private val partyDao: PartyDao,
    private val transactionDao: TransactionDao
) {
    val businessProfile: Flow<BusinessProfile?> = businessProfileDao.getProfile()
    val allParties: Flow<List<Party>> = partyDao.getAllParties()
    val allTransactions: Flow<List<TransactionEntry>> = transactionDao.getAllTransactions()
    val cashbookTransactions: Flow<List<TransactionEntry>> = transactionDao.getCashbookTransactions()

    fun getPartyById(partyId: Long): Flow<Party?> = partyDao.getPartyById(partyId)

    fun getTransactionsForParty(partyId: Long): Flow<List<TransactionEntry>> =
        transactionDao.getTransactionsForParty(partyId)

    suspend fun ensureDefaultDataSeeded() {
        val existingProfile = businessProfileDao.getProfileOnce()
        if (existingProfile == null) {
            businessProfileDao.saveProfile(
                BusinessProfile(
                    id = 1,
                    businessName = "",
                    ownerName = "",
                    phone = "",
                    email = "",
                    address = "",
                    gstin = "",
                    upiId = "",
                    category = "Kids Wear Wholesale",
                    tagline = "Digital Khata & Store Ledger",
                    hasCompletedOnboarding = false
                )
            )
        }
        // Demo data removed: fresh install starts with completely empty parties and transactions
    }

    suspend fun saveBusinessProfile(profile: BusinessProfile) {
        businessProfileDao.saveProfile(profile.copy(id = 1))
    }

    suspend fun addParty(
        name: String,
        phone: String,
        type: PartyType,
        address: String = "",
        gstin: String = "",
        notes: String = "",
        openingBalance: Double = 0.0,
        isYouGaveOpening: Boolean = true
    ): Long {
        val now = System.currentTimeMillis()
        val signedOpening = if (openingBalance == 0.0) {
            0.0
        } else if (isYouGaveOpening) {
            kotlin.math.abs(openingBalance)
        } else {
            -kotlin.math.abs(openingBalance)
        }

        val partyId = partyDao.insertParty(
            Party(
                name = name.trim(),
                phone = phone.trim(),
                type = type,
                address = address.trim(),
                gstin = gstin.trim(),
                notes = notes.trim(),
                balance = signedOpening,
                createdAt = now,
                updatedAt = now
            )
        )

        if (openingBalance > 0.0) {
            transactionDao.insertTransaction(
                TransactionEntry(
                    partyId = partyId,
                    partyNameSnapshot = name.trim(),
                    amount = kotlin.math.abs(openingBalance),
                    type = if (isYouGaveOpening) TransactionType.YOU_GAVE else TransactionType.YOU_GOT,
                    paymentMode = PaymentMode.CASH,
                    description = "Opening Balance",
                    billNumber = "OPEN-1",
                    timestamp = now
                )
            )
        }
        return partyId
    }

    suspend fun updateParty(party: Party) {
        partyDao.updateParty(party.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteParty(party: Party) {
        partyDao.deleteParty(party)
    }

    suspend fun addPartyTransaction(
        partyId: Long,
        amount: Double,
        type: TransactionType,
        paymentMode: PaymentMode,
        description: String,
        billNumber: String = "",
        itemsSummary: String = "",
        timestamp: Long = System.currentTimeMillis(),
        linkedToEntryId: Long? = null,
        isCompleted: Boolean = false
    ) {
        val party = partyDao.getPartyByIdOnce(partyId) ?: return
        transactionDao.insertTransaction(
            TransactionEntry(
                partyId = partyId,
                partyNameSnapshot = party.name,
                amount = amount,
                type = type,
                paymentMode = paymentMode,
                description = description.trim(),
                billNumber = billNumber.trim(),
                itemsSummary = itemsSummary.trim(),
                timestamp = timestamp,
                linkedToEntryId = linkedToEntryId,
                isCompleted = isCompleted
            )
        )
        recalculatePartyBalance(partyId)
    }

    suspend fun addCashbookEntry(
        amount: Double,
        isCashIn: Boolean,
        paymentMode: PaymentMode,
        description: String,
        partyName: String = "",
        billNumber: String = "",
        timestamp: Long = System.currentTimeMillis()
    ) {
        transactionDao.insertTransaction(
            TransactionEntry(
                partyId = null,
                partyNameSnapshot = partyName.ifBlank { if (isCashIn) "Cash Inflow" else "Expense / Outflow" },
                amount = amount,
                type = if (isCashIn) TransactionType.CASH_IN else TransactionType.CASH_OUT,
                paymentMode = paymentMode,
                description = description.trim(),
                billNumber = billNumber.trim(),
                timestamp = timestamp
            )
        )
    }

    suspend fun updateTransaction(entry: TransactionEntry) {
        transactionDao.updateTransaction(entry)
        entry.partyId?.let { recalculatePartyBalance(it) }
    }

    suspend fun deleteTransaction(entry: TransactionEntry) {
        transactionDao.deleteTransaction(entry)
        entry.partyId?.let { recalculatePartyBalance(it) }
    }

    suspend fun recalculatePartyBalance(partyId: Long) {
        val party = partyDao.getPartyByIdOnce(partyId) ?: return
        val txs = transactionDao.getTransactionsForPartyOnce(partyId)
        var newBalance = 0.0
        for (tx in txs) {
            when (tx.type) {
                TransactionType.YOU_GAVE -> newBalance += tx.amount
                TransactionType.YOU_GOT -> newBalance -= tx.amount
                else -> {}
            }
        }
        partyDao.updateParty(
            party.copy(
                balance = newBalance,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updatePartyDueDate(partyId: Long, dueDateTimestamp: Long?) {
        val party = partyDao.getPartyByIdOnce(partyId) ?: return
        partyDao.updateParty(
            party.copy(
                dueDateTimestamp = dueDateTimestamp,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun restoreFullSnapshot(
        profile: BusinessProfile?,
        parties: List<Party>,
        transactions: List<TransactionEntry>
    ) {
        database.withTransaction {
            profile?.let {
                val current = businessProfileDao.getProfileOnce()
                val currentPin = current?.appPin.orEmpty()
                val currentBio = current?.biometricEnabled ?: false
                businessProfileDao.saveProfile(
                    it.copy(
                        id = 1,
                        appPin = currentPin,
                        biometricEnabled = currentBio
                    )
                )
            }
            transactionDao.deleteAllTransactions()
            partyDao.deleteAllParties()
            parties.forEach { partyDao.insertParty(it) }
            transactions.forEach { transactionDao.insertTransaction(it) }
        }
    }
}
