package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionType {
    YOU_GAVE,   // Credit given (Amount you gave to party)
    YOU_GOT,    // Payment received (Amount you got from party)
    CASH_IN,    // Cash drawer inflow
    CASH_OUT    // Cash drawer outflow
}

enum class PaymentMode {
    CASH,
    UPI,
    BANK_TRANSFER,
    CHEQUE
}

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Party::class,
            parentColumns = ["id"],
            childColumns = ["partyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["partyId"]),
        Index(value = ["timestamp"])
    ]
)
data class TransactionEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val partyId: Long? = null,
    val partyNameSnapshot: String = "",
    val amount: Double,
    val type: TransactionType,
    val paymentMode: PaymentMode = PaymentMode.CASH,
    val description: String = "",
    val billNumber: String = "",
    val itemsSummary: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isConfirmedGave: Boolean = false,
    val isConfirmedTaken: Boolean = false,
    val linkedToEntryId: Long? = null,
    val isCompleted: Boolean = false
)
