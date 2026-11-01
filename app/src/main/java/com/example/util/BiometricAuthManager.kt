package com.example.util

import android.content.Context
import android.os.Build

object BiometricAuthManager {
    fun isPinConfigured(pin: String): Boolean = pin.length == 4 && pin.all { it.isDigit() }

    fun verifyPin(enteredPin: String, savedPin: String): Boolean {
        return enteredPin == savedPin
    }

    fun getSecuritySubtitle(context: Context, biometricEnabled: Boolean): String {
        return if (biometricEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            "Enter your 4-digit Kirio PIN or use Quick Unlock"
        } else {
            "Enter your 4-digit Kirio Security PIN to unlock your ledger"
        }
    }
}
