package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import com.example.data.model.BusinessProfile
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.data.model.PaymentMode
import com.example.data.model.TransactionEntry
import com.example.data.model.TransactionType
import com.example.data.repository.FirestoreSyncRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Kirio", appName)
  }

  @Test
  fun testBackupJson_includesAllFieldsAndExcludesPin() {
    val profile = BusinessProfile(
      businessName = "Shree Krishna Garments",
      ownerName = "Ramesh Bhai",
      phone = "9876543210",
      category = "Kids Wear Wholesale",
      appPin = "sha256:abcd:ef01",
      biometricEnabled = true
    )
    val parties = listOf(
      Party(id = 1L, name = "Aarav Fabrics", phone = "9123456789", type = PartyType.SUPPLIER, balance = 1200.0)
    )
    val entries = listOf(
      TransactionEntry(
        id = 10L,
        partyId = 1L,
        amount = 1200.0,
        type = TransactionType.YOU_GAVE,
        paymentMode = PaymentMode.UPI,
        linkedToEntryId = 5L,
        isCompleted = true,
        itemsSummary = "Denim Pants 50pcs"
      )
    )

    val jsonString = FirestoreSyncRepository.createFullBackupJson(profile, parties, entries)
    assertFalse("Backup JSON must NOT contain appPin", jsonString.contains("sha256:abcd:ef01"))
    assertTrue("Backup JSON must contain linkedToEntryId", jsonString.contains("\"linkedToEntryId\""))
    assertTrue("Backup JSON must contain isCompleted", jsonString.contains("\"isCompleted\""))
    assertTrue("Backup JSON must contain category", jsonString.contains("Kids Wear Wholesale"))

    val parsed = FirestoreSyncRepository.parseBackupJson(jsonString)
    assertNotNull(parsed)
    val (parsedProfile, parsedParties, parsedEntries) = parsed!!
    assertNotNull(parsedProfile)
    assertEquals("Shree Krishna Garments", parsedProfile?.businessName)
    assertEquals("", parsedProfile?.appPin) // PIN is empty in parsed backup
    assertEquals(1, parsedParties.size)
    assertEquals("Aarav Fabrics", parsedParties[0].name)
    assertEquals(1, parsedEntries.size)
    assertEquals(5L, parsedEntries[0].linkedToEntryId)
    assertTrue(parsedEntries[0].isCompleted)
    assertEquals(PaymentMode.UPI, parsedEntries[0].paymentMode)
    assertEquals("Denim Pants 50pcs", parsedEntries[0].itemsSummary)
  }
}

