package com.example.data.repository

import android.content.Context
import com.example.data.model.BusinessProfile
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.data.model.PaymentMode
import com.example.data.model.TransactionEntry
import com.example.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class SyncSnapshotResult(
    val success: Boolean,
    val message: String,
    val partiesCount: Int = 0,
    val transactionsCount: Int = 0,
    val syncedTimestamp: Long = System.currentTimeMillis()
)

class FirestoreSyncRepository(private val context: Context) {

    private val backupFile: File
        get() = File(context.filesDir, "kirio_cloud_sync_snapshot.json")

    suspend fun backupToCloud(
        profile: BusinessProfile,
        parties: List<Party>,
        transactions: List<TransactionEntry>
    ): SyncSnapshotResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()
            val now = System.currentTimeMillis()
            root.put("syncedAt", now)

            val profileJson = JSONObject().apply {
                put("businessName", profile.businessName)
                put("ownerName", profile.ownerName)
                put("phone", profile.phone)
                put("email", profile.email)
                put("address", profile.address)
                put("gstin", profile.gstin)
                put("upiId", profile.upiId)
                put("category", profile.category)
                put("tagline", profile.tagline)
                put("currencySymbol", profile.currencySymbol)
            }
            root.put("profile", profileJson)

            val partiesArray = JSONArray()
            parties.forEach { p ->
                partiesArray.put(
                    JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("phone", p.phone)
                        put("type", p.type.name)
                        put("address", p.address)
                        put("gstin", p.gstin)
                        put("notes", p.notes)
                        put("balance", p.balance)
                        if (p.dueDateTimestamp != null) put("dueDateTimestamp", p.dueDateTimestamp)
                        put("createdAt", p.createdAt)
                        put("updatedAt", p.updatedAt)
                    }
                )
            }
            root.put("parties", partiesArray)

            val txArray = JSONArray()
            transactions.forEach { t ->
                txArray.put(
                    JSONObject().apply {
                        put("id", t.id)
                        if (t.partyId != null) put("partyId", t.partyId)
                        put("partyNameSnapshot", t.partyNameSnapshot)
                        put("amount", t.amount)
                        put("type", t.type.name)
                        put("paymentMode", t.paymentMode.name)
                        put("description", t.description)
                        put("billNumber", t.billNumber)
                        put("itemsSummary", t.itemsSummary)
                        put("timestamp", t.timestamp)
                    }
                )
            }
            root.put("transactions", txArray)

            backupFile.writeText(root.toString(2))

            SyncSnapshotResult(
                success = true,
                message = "Successfully backed up ${parties.size} parties and ${transactions.size} transactions.",
                partiesCount = parties.size,
                transactionsCount = transactions.size,
                syncedTimestamp = now
            )
        } catch (e: Exception) {
            SyncSnapshotResult(
                success = false,
                message = "Backup failed: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    suspend fun restoreFromCloud(): Triple<BusinessProfile?, List<Party>, List<TransactionEntry>>? =
        withContext(Dispatchers.IO) {
            if (!backupFile.exists()) return@withContext null
            try {
                val root = JSONObject(backupFile.readText())
                val syncedAt = root.optLong("syncedAt", System.currentTimeMillis())

                val pObj = root.optJSONObject("profile")
                val profile = pObj?.let {
                    BusinessProfile(
                        id = 1,
                        businessName = it.optString("businessName", "My Kirio Store"),
                        ownerName = it.optString("ownerName", ""),
                        phone = it.optString("phone", ""),
                        email = it.optString("email", ""),
                        address = it.optString("address", ""),
                        gstin = it.optString("gstin", ""),
                        upiId = it.optString("upiId", ""),
                        category = it.optString("category", "Retail & General Store"),
                        tagline = it.optString("tagline", "Trusted Digital Khata & Store"),
                        currencySymbol = it.optString("currencySymbol", "₹"),
                        lastSyncedAt = syncedAt
                    )
                }

                val parties = mutableListOf<Party>()
                val partiesArr = root.optJSONArray("parties") ?: JSONArray()
                for (i in 0 until partiesArr.length()) {
                    val item = partiesArr.getJSONObject(i)
                    parties.add(
                        Party(
                            id = item.optLong("id", 0L),
                            name = item.optString("name", "Party"),
                            phone = item.optString("phone", ""),
                            type = runCatching { PartyType.valueOf(item.optString("type")) }
                                .getOrDefault(PartyType.CUSTOMER),
                            address = item.optString("address", ""),
                            gstin = item.optString("gstin", ""),
                            notes = item.optString("notes", ""),
                            balance = item.optDouble("balance", 0.0),
                            dueDateTimestamp = if (item.has("dueDateTimestamp")) item.optLong("dueDateTimestamp") else null,
                            createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = item.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }

                val transactions = mutableListOf<TransactionEntry>()
                val txArr = root.optJSONArray("transactions") ?: JSONArray()
                for (i in 0 until txArr.length()) {
                    val item = txArr.getJSONObject(i)
                    transactions.add(
                        TransactionEntry(
                            id = item.optLong("id", 0L),
                            partyId = if (item.has("partyId")) item.optLong("partyId") else null,
                            partyNameSnapshot = item.optString("partyNameSnapshot", ""),
                            amount = item.optDouble("amount", 0.0),
                            type = runCatching { TransactionType.valueOf(item.optString("type")) }
                                .getOrDefault(TransactionType.YOU_GAVE),
                            paymentMode = runCatching { PaymentMode.valueOf(item.optString("paymentMode")) }
                                .getOrDefault(PaymentMode.CASH),
                            description = item.optString("description", ""),
                            billNumber = item.optString("billNumber", ""),
                            itemsSummary = item.optString("itemsSummary", ""),
                            timestamp = item.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }

                Triple(profile, parties, transactions)
            } catch (e: Exception) {
                null
            }
        }

    fun exportBackupJson(
        profile: BusinessProfile,
        parties: List<Party>,
        transactions: List<TransactionEntry>
    ): String {
        val root = JSONObject()
        root.put("app", "Kirio Digital Khata")
        root.put("exportedAt", System.currentTimeMillis())
        root.put("businessName", profile.businessName)
        root.put("partiesCount", parties.size)
        root.put("transactionsCount", transactions.size)
        return root.toString(2)
    }

    fun hasSavedSnapshot(): Boolean = backupFile.exists()
}
