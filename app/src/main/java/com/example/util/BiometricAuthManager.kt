package com.example.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.MessageDigest
import java.security.SecureRandom

object BiometricAuthManager {
    fun isPinConfigured(storedPin: String): Boolean = storedPin.isNotBlank()

    fun isLegacyPlainPin(storedValue: String): Boolean {
        return storedValue.length == 4 && storedValue.all { it.isDigit() }
    }

    fun hashPin(pin: String): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        val saltHex = salt.joinToString("") { "%02x".format(it) }
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val hash = digest.digest(pin.toByteArray(Charsets.UTF_8))
        val hashHex = hash.joinToString("") { "%02x".format(it) }
        return "sha256:$saltHex:$hashHex"
    }

    fun verifyPin(enteredPin: String, storedValue: String): Boolean {
        if (storedValue.isBlank()) return false
        // Legacy plain 4-digit PIN support
        if (isLegacyPlainPin(storedValue)) {
            return enteredPin == storedValue
        }
        // Salted hash format: sha256:<saltHex>:<hashHex>
        if (storedValue.startsWith("sha256:")) {
            val parts = storedValue.split(":")
            if (parts.size == 3) {
                val saltHex = parts[1]
                val expectedHashHex = parts[2]
                val salt = saltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                val digest = MessageDigest.getInstance("SHA-256")
                digest.update(salt)
                val actualHash = digest.digest(enteredPin.toByteArray(Charsets.UTF_8))
                val actualHashHex = actualHash.joinToString("") { "%02x".format(it) }
                return actualHashHex.equals(expectedHashHex, ignoreCase = true)
            }
        }
        return false
    }

    fun canAuthenticateWithBiometrics(context: Context): Boolean {
        return try {
            val biometricManager = BiometricManager.from(context)
            val canAuth = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            canAuth == BiometricManager.BIOMETRIC_SUCCESS
        } catch (e: Exception) {
            false
        }
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!canAuthenticateWithBiometrics(activity)) {
            onError("Biometrics unavailable")
            return
        }

        try {
            val executor = ContextCompat.getMainExecutor(activity)
            val prompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        onError(errString.toString())
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onError("Fingerprint not recognized")
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText("Use PIN")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
                .build()

            prompt.authenticate(promptInfo)
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Biometric error")
        }
    }

    fun getSecuritySubtitle(context: Context, biometricEnabled: Boolean): String {
        return if (biometricEnabled && canAuthenticateWithBiometrics(context)) {
            "Enter your 4-digit Kirio PIN or use Fingerprint"
        } else {
            "Enter your 4-digit Kirio Security PIN to unlock your ledger"
        }
    }
}
