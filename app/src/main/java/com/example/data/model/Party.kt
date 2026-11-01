package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PartyType {
    CUSTOMER,
    SUPPLIER
}

@Entity(tableName = "parties")
data class Party(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val type: PartyType = PartyType.CUSTOMER,
    val address: String = "",
    val gstin: String = "",
    val notes: String = "",
    val balance: Double = 0.0, // Positive = You Will Get (Customer owes you), Negative = You Will Give (You owe them)
    val dueDateTimestamp: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
