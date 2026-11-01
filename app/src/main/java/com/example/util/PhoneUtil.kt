package com.example.util

object PhoneUtil {

    /**
     * Normalizes a raw phone input to digits only.
     * Strips leading 0 (e.g. 09876543210 -> 9876543210).
     * If resulting string is 10 digits, adds "91" country prefix.
     * Keeps existing country code if already present.
     */
    fun normalize(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        if (digits.isBlank()) return ""

        val withoutLeadingZero = if (digits.startsWith("0")) digits.drop(1) else digits

        return if (withoutLeadingZero.length == 10) {
            "91$withoutLeadingZero"
        } else {
            withoutLeadingZero
        }
    }

    /**
     * Returns the phone number ready for wa.me links (digits only, e.g. 919876543210).
     */
    fun toWhatsAppNumber(raw: String): String {
        return normalize(raw)
    }

    /**
     * Formats normalized number for display (e.g. "+91 98765 43210").
     */
    fun toDisplay(raw: String): String {
        val norm = normalize(raw)
        return if (norm.length == 12 && norm.startsWith("91")) {
            "+91 ${norm.substring(2, 7)} ${norm.substring(7)}"
        } else if (norm.length == 10) {
            "+91 ${norm.substring(0, 5)} ${norm.substring(5)}"
        } else if (norm.isNotBlank()) {
            "+$norm"
        } else {
            raw
        }
    }
}
