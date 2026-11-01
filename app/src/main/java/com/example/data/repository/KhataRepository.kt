package com.example.data.repository

import com.example.data.dao.BusinessProfileDao
import com.example.data.dao.PartyDao
import com.example.data.dao.TransactionDao
import com.example.data.model.BusinessProfile
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.data.model.PaymentMode
import com.example.data.model.TransactionEntry
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

class KhataRepository(
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
                    category = "Retail & Wholesale Store",
                    tagline = "Digital Khata & Store Ledger",
                    hasCompletedOnboarding = false
                )
            )
        }

        val existingParties = partyDao.getAllPartiesOnce()
        if (existingParties.isEmpty()) {
            val now = System.currentTimeMillis()
            val dayMs = 86_400_000L

            val p1Id = partyDao.insertParty(
                Party(
                    name = "Aarav General Store",
                    phone = "9820112233",
                    type = PartyType.CUSTOMER,
                    address = "Indiranagar 100ft Rd",
                    notes = "Monthly wholesale grocery buyer",
                    balance = 4250.0,
                    dueDateTimestamp = now + (3 * dayMs),
                    createdAt = now - (14 * dayMs),
                    updatedAt = now - (1 * dayMs)
                )
            )

            val p2Id = partyDao.insertParty(
                Party(
                    name = "Meera Textiles & Mart",
                    phone = "9845098450",
                    type = PartyType.CUSTOMER,
                    address = "Jayanagar 4th Block",
                    notes = "Prefers UPI payment on 5th",
                    balance = 1800.0,
                    dueDateTimestamp = now + (5 * dayMs),
                    createdAt = now - (10 * dayMs),
                    updatedAt = now - (2 * dayMs)
                )
            )

            val p3Id = partyDao.insertParty(
                Party(
                    name = "Sri Balaji Agro Distributors",
                    phone = "9900112244",
                    type = PartyType.SUPPLIER,
                    address = "APMC Yard, Yeshwanthpur",
                    gstin = "29BALAJ9988K1Z2",
                    notes = "Main rice & oil supplier",
                    balance = -6400.0,
                    dueDateTimestamp = now + (7 * dayMs),
                    createdAt = now - (20 * dayMs),
                    updatedAt = now - (1 * dayMs)
                )
            )

            val p4Id = partyDao.insertParty(
                Party(
                    name = "Vikram Cafe & Bakery",
                    phone = "9731223344",
                    type = PartyType.CUSTOMER,
                    address = "Koramangala 5th Block",
                    notes = "Daily dairy & flour supply",
                    balance = 950.0,
                    createdAt = now - (8 * dayMs),
                    updatedAt = now - (12 * 3600_000L)
                )
            )

            // Seed sample transactions
            transactionDao.insertTransaction(
                TransactionEntry(
                    partyId = p1Id,
                    partyNameSnapshot = "Aarav General Store",
                    amount = 6250.0,
                    type = TransactionType.YOU_GAVE,
                    paymentMode = PaymentMode.CASH,
                    description = "25kg Basmati Rice + Refined Oil Cartons",
                    billNumber = "INV-1041",
                    timestamp = now - (4 * dayMs)
                )
            )
            transactionDao.insertTransaction(
                TransactionEntry(
                    partyId = p1Id,
                    partyNameSnapshot = "Aarav General Store",
                    amount = 2000.0,
                    type = TransactionType.YOU_GOT,
                    paymentMode = PaymentMode.UPI,
                    description = "Part payment received via PhonePe",
                    billNumber = "REC-208",
                    timestamp = now - (1 * dayMs)
                )
            )
            transactionDao.insertTransaction(
                TransactionEntry(
                    partyId = p2Id,
                    partyNameSnapshot = "Meera Textiles & Mart",
                    amount = 1800.0,
                    type = TransactionType.YOU_GAVE,
                    paymentMode = PaymentMode.UPI,
                    description = "Pantry supplies & packaging rolls",
                    billNumber = "INV-1045",
                    timestamp = now - (2 * dayMs)
                )
            )
            transactionDao.insertTransaction(
                TransactionEntry(
                    partyId = p3Id,
                    partyNameSnapshot = "Sri Balaji Agro Distributors",
                    amount = 6400.0,
                    type = TransactionType.YOU_GOT,
                    paymentMode = PaymentMode.BANK_TRANSFER,
                    description = "Wholesale stock purchase on credit",
                    billNumber = "PUR-882",
                    timestamp = now - (3 * dayMs)
                )
            )
            transactionDao.insertTransaction(
                TransactionEntry(
                    partyId = p4Id,
                    partyNameSnapshot = "Vikram Cafe & Bakery",
                    amount = 950.0,
                    type = TransactionType.YOU_GAVE,
                    paymentMode = PaymentMode.CASH,
                    description = "Maida 20kg + Sugar 10kg",
                    billNumber = "INV-1049",
                    timestamp = now - (12 * 3600_000L)
                )
            )

            // Cashbook entries
            transactionDao.insertTransaction(
                TransactionEntry(
                    partyId = null,
                    partyNameSnapshot = "Walk-in Counter Sales",
                    amount = 3450.0,
                    type = TransactionType.CASH_IN,
                    paymentMode = PaymentMode.CASH,
                    description = "Morning counter retail sales",
                    billNumber = "CSH-01",
                    timestamp = now - (18 * 3600_000L)
                )
            )
            transactionDao.insertTransaction(
                TransactionEntry(
                    partyId = null,
                    partyNameSnapshot = "UPI QR Counter Collection",
                    amount = 2180.0,
                    type = TransactionType.CASH_IN,
                    paymentMode = PaymentMode.UPI,
                    description = "QR Standee instant payments",
                    billNumber = "CSH-02",
                    timestamp = now - (8 * 3600_000L)
                )
            )
            transactionDao.insertTransaction(
                TransactionEntry(
                    partyId = null,
                    partyNameSnapshot = "Store Electricity & Delivery Fuel",
                    amount = 680.0,
                    type = TransactionType.CASH_OUT,
                    paymentMode = PaymentMode.CASH,
                    description = "Delivery van fuel & tea expense",
                    billNumber = "EXP-12",
                    timestamp = now - (5 * 3600_000L)
                )
            )
        }
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
        timestamp: Long = System.currentTimeMillis()
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
                timestamp = timestamp
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
        profile?.let { businessProfileDao.saveProfile(it.copy(id = 1)) }
        transactionDao.deleteAllTransactions()
        partyDao.deleteAllParties()
        parties.forEach { partyDao.insertParty(it) }
        transactions.forEach { transactionDao.insertTransaction(it) }
    }
}
