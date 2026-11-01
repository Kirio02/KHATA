package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_profile")
data class BusinessProfile(
    @PrimaryKey val id: Int = 1,
    val businessName: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val gstin: String = "",
    val upiId: String = "",
    val category: String = "Retail & General Store",
    val tagline: String = "Digital Khata & Store Ledger",
    val currencySymbol: String = "₹",
    val appPin: String = "",
    val biometricEnabled: Boolean = false,
    val languageCode: String = "en",
    val lastSyncedAt: Long? = null,
    val hasCompletedOnboarding: Boolean = false
)
