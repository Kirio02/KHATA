package com.example.util

import com.example.data.model.PartyType
import com.example.data.model.PaymentMode

data class ImportedKhataRecord(
    val partyName: String,
    val phone: String,
    val partyType: PartyType,
    val amount: Double,
    val isYouGave: Boolean,
    val description: String,
    val paymentMode: PaymentMode = PaymentMode.CASH
)

object KhatabookImporter {

    /**
     * Parses CSV or tab/pipe separated text pasted or uploaded from Khatabook / OkCredit / Excel.
     * Expected flexible columns: Name, Phone, Amount, Type (Gave/Got or Dr/Cr), Note
     */
    fun parseCsvContent(rawText: String): List<ImportedKhataRecord> {
        val records = mutableListOf<ImportedKhataRecord>()
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        for ((index, line) in lines.withIndex()) {
            // Skip header row if present
            if (index == 0 && (line.lowercase().contains("party") || line.lowercase().contains("name") || line.lowercase().contains("amount"))) {
                continue
            }

            val parts = line.split(",", ";", "\t").map { it.trim().removeSurrounding("\"") }
            if (parts.size < 2) continue

            val name = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: continue
            val secondCol = parts.getOrNull(1).orEmpty()
            val thirdCol = parts.getOrNull(2).orEmpty()
            val fourthCol = parts.getOrNull(3).orEmpty()
            val fifthCol = parts.getOrNull(4).orEmpty()

            // Detect if column 2 is phone or amount
            val col2AsDouble = secondCol.replace("₹", "").replace(",", "").toDoubleOrNull()
            val col3AsDouble = thirdCol.replace("₹", "").replace(",", "").toDoubleOrNull()

            val phone: String
            val amount: Double
            val typeIndicator: String
            val note: String

            if (col2AsDouble != null && col3AsDouble == null) {
                phone = ""
                amount = kotlin.math.abs(col2AsDouble)
                typeIndicator = thirdCol
                note = fourthCol.ifBlank { "Imported from Khatabook CSV" }
            } else {
                phone = secondCol
                amount = kotlin.math.abs(col3AsDouble ?: 0.0)
                typeIndicator = fourthCol
                note = fifthCol.ifBlank { "Imported from Khatabook CSV" }
            }

            if (amount <= 0.0) continue

            val lowerType = typeIndicator.lowercase()
            val isYouGave = !(lowerType.contains("got") || lowerType.contains("cr") || lowerType.contains("receive") || lowerType.contains("payable") || (col2AsDouble != null && col2AsDouble < 0) || (col3AsDouble != null && col3AsDouble < 0))
            val partyType = if (lowerType.contains("supplier") || lowerType.contains("vendor")) {
                PartyType.SUPPLIER
            } else {
                PartyType.CUSTOMER
            }

            records.add(
                ImportedKhataRecord(
                    partyName = name,
                    phone = phone,
                    partyType = partyType,
                    amount = amount,
                    isYouGave = isYouGave,
                    description = note
                )
            )
        }
        return records
    }

    fun getSampleCsvTemplate(): String {
        return """
            Party Name,Phone,Amount,Type,Description
            Ramesh Kirana Store,9811223344,3200,YOU_GAVE,Monthly grocery supply bill #402
            Suresh Dairy Farm,9822334455,1450,YOU_GOT,Milk & paneer advance settled
            Ananya Medicals,9876501234,2750,YOU_GAVE,Wholesale wellness items
            Metro Packaging Co,9900887766,4100,SUPPLIER_GOT,Carton boxes & tape credit
        """.trimIndent()
    }
}
