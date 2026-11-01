package com.example

import com.example.data.model.BusinessProfile
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.data.model.PaymentMode
import com.example.data.model.TransactionEntry
import com.example.data.model.TransactionType
import com.example.data.repository.FirestoreSyncRepository
import com.example.data.repository.computeRunningBalances
import com.example.util.BiometricAuthManager
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testRunningBalances_pureFunctionAccumulation() {
    val entries = listOf(
      TransactionEntry(id = 1L, partyId = 10L, amount = 1000.0, type = TransactionType.YOU_GAVE, timestamp = 100L),
      TransactionEntry(id = 2L, partyId = 10L, amount = 400.0, type = TransactionType.YOU_GOT, timestamp = 200L),
      TransactionEntry(id = 3L, partyId = 10L, amount = 250.0, type = TransactionType.YOU_GAVE, timestamp = 300L)
    )
    val runningBalances = computeRunningBalances(entries)
    assertEquals(1000.0, runningBalances[1L] ?: 0.0, 0.001)
    assertEquals(600.0, runningBalances[2L] ?: 0.0, 0.001)
    assertEquals(850.0, runningBalances[3L] ?: 0.0, 0.001)
  }

  @Test
  fun testRunningBalances_unorderedTimestampsAreSorted() {
    val entries = listOf(
      TransactionEntry(id = 3L, partyId = 10L, amount = 250.0, type = TransactionType.YOU_GAVE, timestamp = 300L),
      TransactionEntry(id = 1L, partyId = 10L, amount = 1000.0, type = TransactionType.YOU_GAVE, timestamp = 100L),
      TransactionEntry(id = 2L, partyId = 10L, amount = 400.0, type = TransactionType.YOU_GOT, timestamp = 200L)
    )
    val runningBalances = computeRunningBalances(entries)
    assertEquals(1000.0, runningBalances[1L] ?: 0.0, 0.001)
    assertEquals(600.0, runningBalances[2L] ?: 0.0, 0.001)
    assertEquals(850.0, runningBalances[3L] ?: 0.0, 0.001)
  }

  @Test
  fun testBiometricAuthManager_pinHashingAndVerification() {
    val plainPin = "1234"
    val hashed = BiometricAuthManager.hashPin(plainPin)
    assertTrue("Hashed PIN should have sha256 prefix", hashed.startsWith("sha256:"))

    // Verify correct PIN
    assertTrue(BiometricAuthManager.verifyPin("1234", hashed))
    // Verify wrong PIN
    assertFalse(BiometricAuthManager.verifyPin("9999", hashed))

    // Verify legacy plain PIN
    assertTrue(BiometricAuthManager.isLegacyPlainPin("4321"))
    assertFalse(BiometricAuthManager.isLegacyPlainPin(hashed))
    assertTrue(BiometricAuthManager.verifyPin("4321", "4321"))
  }
}



