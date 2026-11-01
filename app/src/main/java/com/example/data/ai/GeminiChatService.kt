package com.example.data.ai

import com.example.BuildConfig
import com.example.data.model.BusinessProfile
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.data.model.TransactionEntry
import com.example.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

data class AiActionParsed(
    val replyText: String,
    val addPartyName: String? = null,
    val addPartyPhone: String? = null,
    val addPartyType: PartyType? = null,
    val addPartyOpeningBalance: Double? = null,
    val isYouGave: Boolean = true
)

class GeminiChatService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun askBusinessAdvisor(
        userMessage: String,
        profile: BusinessProfile,
        parties: List<Party>,
        transactions: List<TransactionEntry>,
        history: List<ChatMessage>,
        languageCode: String = "en"
    ): AiActionParsed = withContext(Dispatchers.IO) {
        val currency = profile.currencySymbol.ifBlank { "₹" }
        val totalGet = parties.filter { it.balance > 0 }.sumOf { it.balance }
        val totalGive = parties.filter { it.balance < 0 }.sumOf { kotlin.math.abs(it.balance) }
        val cashIn = transactions.filter { it.type == TransactionType.CASH_IN }.sumOf { it.amount }
        val cashOut = transactions.filter { it.type == TransactionType.CASH_OUT }.sumOf { it.amount }
        val topDebtors = parties.filter { it.balance > 0 }
            .sortedByDescending { it.balance }
            .take(5)
            .joinToString(", ") { "${it.name} ($currency${formatNum(it.balance)})" }
        val topCreditors = parties.filter { it.balance < 0 }
            .sortedBy { it.balance }
            .take(5)
            .joinToString(", ") { "${it.name} ($currency${formatNum(kotlin.math.abs(it.balance))})" }

        // Local intent check first: if user asked to add customer/supplier
        val extractedParty = tryExtractAddParty(userMessage)

        val systemContext = """
            You are Kirio AI Munshi, an honest, highly reliable Indian business accountant and ledger voice assistant in Kirio Khata.
            Language Rule: The user speaks/types in English, Hindi, Gujarati, or Hinglish. Always reply warmly in the user's chosen language (${languageCode}) or the language they used.
            
            CRITICAL HONESTY & CAPABILITY RULES:
            1. STRICT REJECTION OF OUT-OF-SCOPE QUERIES:
               If a user asks about anything unrelated to Kirio ledger, store accounting, dues, bills, customers, or suppliers (such as movies, cricket, politics, weather, songs, general trivia, gaming, coding, hacking), you MUST explicitly and politely say NO:
               - English: "No, I cannot do that. I am your specialized Kirio Khata accountant and can only help with your customers, suppliers, dues, cashbook, and bills."
               - Hindi: "माफ़ कीजिए, मैं यह नहीं कर सकता। मैं आपका किरिओ मुंशी हूँ और केवल आपके ग्राहकों, सप्लायर्स, उधारी, पेमेंट और बिल बनाने में मदद कर सकता हूँ।"
               - Gujarati: "માફ કરશો, હું આ નથી કરી શકતો. હું તમારો કિરીઓ મુન્શી છું અને માત્ર તમારા ગ્રાહકો, વેપારીઓ, બાકી ઉઘરાણી, પેમેન્ટ અને બિલ હિસાબમાં મદદ કરી શકું છું."
               - Hinglish: "Sorry, main yeh nahi kar sakta. Main sirf aapke Kirio Khata, customer, supplier aur ledger hisab-kitab mein help kar sakta hoon."

            2. GUIDING USERS IN TROUBLE / CONFUSION:
               If the user says they are having trouble, confused, or asks how to use the app, provide concise, numbered steps in their language on how to:
               - Add a customer or supplier (+ ADD CUSTOMER)
               - Record money given (RED 'YOU GAVE') or money received (GREEN 'YOU GOT')
               - Manually tick confirmed payments in the ledger entry list
               - Send WhatsApp payment reminders via 'REMIND >'
               - Make quick bills in the Bills tab.

            3. AUTOMATICALLY ADDING CUSTOMERS / SUPPLIERS:
               If the user asks to add a customer or supplier (e.g. "Add Suresh 9876543210 customer with 500 balance" or "રમેશ ભાઈ ને કસ્ટમર તરીકે ઉમેરો"), warmly acknowledge that you have created the party in their ledger.

            Current Ledger Snapshot for "${profile.businessName}":
            - Total Parties: ${parties.size}
            - You Will Get (Receivables): $currency${formatNum(totalGet)}
            - You Will Give (Payables): $currency${formatNum(totalGive)}
            - Net Drawer Cash: $currency${formatNum(cashIn - cashOut)}
            - Top Receivables: ${topDebtors.ifBlank { "None" }}
            - Top Payables: ${topCreditors.ifBlank { "None" }}
        """.trimIndent()

        val apiKey = runCatching { BuildConfig.GEMINI_API_KEY }.getOrDefault("")
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "your_api_key_here") {
            val localText = generateLocalSmartResponse(
                userMessage = userMessage,
                currency = currency,
                businessName = profile.businessName,
                parties = parties,
                transactions = transactions,
                totalGet = totalGet,
                totalGive = totalGive,
                cashIn = cashIn,
                cashOut = cashOut,
                languageCode = languageCode,
                extractedParty = extractedParty
            )
            return@withContext AiActionParsed(
                replyText = localText,
                addPartyName = extractedParty?.name,
                addPartyPhone = extractedParty?.phone,
                addPartyType = extractedParty?.type,
                addPartyOpeningBalance = extractedParty?.balance
            )
        }

        try {
            val contentsArray = JSONArray()
            history.takeLast(6).forEach { msg ->
                val partObj = JSONObject().put("text", msg.text)
                val partsArr = JSONArray().put(partObj)
                val role = if (msg.isFromUser) "user" else "model"
                contentsArray.put(JSONObject().put("role", role).put("parts", partsArr))
            }
            contentsArray.put(
                JSONObject()
                    .put("role", "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
            )

            val payload = JSONObject().apply {
                put(
                    "systemInstruction",
                    JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemContext)))
                )
                put("contents", contentsArray)
                put("generationConfig", JSONObject().put("temperature", 0.3))
            }

            val url =
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val local = generateLocalSmartResponse(
                        userMessage, currency, profile.businessName, parties, transactions, totalGet, totalGive, cashIn, cashOut, languageCode, extractedParty
                    )
                    return@withContext AiActionParsed(
                        replyText = local,
                        addPartyName = extractedParty?.name,
                        addPartyPhone = extractedParty?.phone,
                        addPartyType = extractedParty?.type,
                        addPartyOpeningBalance = extractedParty?.balance
                    )
                }
                val json = JSONObject(bodyStr)
                val text = json.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")

                val finalReply = if (!text.isNullOrBlank()) text.trim() else generateLocalSmartResponse(
                    userMessage, currency, profile.businessName, parties, transactions, totalGet, totalGive, cashIn, cashOut, languageCode, extractedParty
                )

                AiActionParsed(
                    replyText = finalReply,
                    addPartyName = extractedParty?.name,
                    addPartyPhone = extractedParty?.phone,
                    addPartyType = extractedParty?.type,
                    addPartyOpeningBalance = extractedParty?.balance
                )
            }
        } catch (e: Exception) {
            val fallback = generateLocalSmartResponse(
                userMessage, currency, profile.businessName, parties, transactions, totalGet, totalGive, cashIn, cashOut, languageCode, extractedParty
            )
            AiActionParsed(
                replyText = fallback,
                addPartyName = extractedParty?.name,
                addPartyPhone = extractedParty?.phone,
                addPartyType = extractedParty?.type,
                addPartyOpeningBalance = extractedParty?.balance
            )
        }
    }

    data class ExtractedParty(val name: String, val phone: String, val type: PartyType, val balance: Double)

    private fun tryExtractAddParty(msg: String): ExtractedParty? {
        val lower = msg.lowercase(Locale.getDefault())
        val isAdd = lower.contains("add") || lower.contains("create") || lower.contains("jodo") ||
            lower.contains("joado") || lower.contains("જોડો") || lower.contains("ઉમેરો") ||
            lower.contains("उमेदवार") || lower.contains("जोड़ो") || lower.contains("बनाओ") ||
            lower.contains("banao") || lower.contains("दर्ज") || lower.contains("new party") ||
            lower.contains("new customer") || lower.contains("new supplier")
        if (!isAdd) return null

        val isSupplier = lower.contains("supplier") || lower.contains("vepari") ||
            lower.contains("सप्लायर") || lower.contains("વેપારી") || lower.contains("व्यापारी")
        val partyType = if (isSupplier) PartyType.SUPPLIER else PartyType.CUSTOMER

        // Extract phone if 10 digits
        val phoneMatch = Regex("""(\+91[\-\s]?)?[6-9]\d{9}""").find(msg)?.value ?: ""

        // Extract amount if any
        val amountMatch = Regex("""(?:₹|rs\.?|inr|balance|udhaar|બાકી|रुपये)?\s*(\d+(?:\.\d+)?)""").find(lower)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

        // Extract candidate name: clean out keywords, numbers, phones
        var clean = msg
            .replace(Regex("""(?i)\b(add|customer|supplier|party|jodo|banao|with|phone|mobile|balance|rupees|rs|inr|udhaar|ne|ko|tariqe|ka|ki|number)\b"""), " ")
            .replace(phoneMatch, " ")
            .replace(Regex("""\d+"""), " ")
            .replace(Regex("""(જોડો|ઉમેરો|ગ્રાહક|વેપારી|સપ્લાયર|કસ્ટમર|તરીકે|ખાતામાં|જોડો|जोड़ो|बनाओ|ग्राहक|सप्लायर|रुपये)"""), " ")
            .replace(Regex("""[^\w\s\u0A80-\u0AFF\u0900-\u097F]"""), " ")
            .trim()

        val nameCandidate = clean.split(Regex("""\s+""")).filter { it.length > 1 }.take(2).joinToString(" ")
        if (nameCandidate.isNotBlank() && nameCandidate.length >= 2) {
            return ExtractedParty(
                name = nameCandidate.replaceFirstChar { it.uppercase() },
                phone = phoneMatch,
                type = partyType,
                balance = amountMatch
            )
        }
        return null
    }

    private fun generateLocalSmartResponse(
        userMessage: String,
        currency: String,
        businessName: String,
        parties: List<Party>,
        transactions: List<TransactionEntry>,
        totalGet: Double,
        totalGive: Double,
        cashIn: Double,
        cashOut: Double,
        languageCode: String,
        extractedParty: ExtractedParty? = null
    ): String {
        val q = userMessage.lowercase(Locale.getDefault())
        val debtors = parties.filter { it.balance > 0 }.sortedByDescending { it.balance }
        val creditors = parties.filter { it.balance < 0 }.sortedBy { it.balance }

        // If party was successfully extracted and added:
        if (extractedParty != null) {
            val typeStr = if (extractedParty.type == PartyType.CUSTOMER) "Customer" else "Supplier"
            val typeStrHi = if (extractedParty.type == PartyType.CUSTOMER) "ग्राहक" else "सप्लायर"
            val typeStrGu = if (extractedParty.type == PartyType.CUSTOMER) "ગ્રાહક" else "વેપારી"
            val balNote = if (extractedParty.balance > 0) " with $currency${formatNum(extractedParty.balance)} balance" else ""
            val balNoteHi = if (extractedParty.balance > 0) " (प्रारंभिक बाक़ी: $currency${formatNum(extractedParty.balance)})" else ""
            val balNoteGu = if (extractedParty.balance > 0) " (શરૂઆતની બાકી: $currency${formatNum(extractedParty.balance)})" else ""

            return when (languageCode) {
                "hi" -> "✅ सफलता! मैंने **${extractedParty.name}** को आपके **$typeStrHi** सूची में जोड़ दिया है$balNoteHi। अब आप उनके नाम पर सीधे उधार या पेमेंट लिख सकते हैं!"
                "gu" -> "✅ સફળતા! મેં **${extractedParty.name}** ને તમારા **$typeStrGu** તરીકે ખાતામાં ઉમેરી દીધા છે$balNoteGu। તમે હવે સીધો હિસાબ રાખી શકો છો!"
                "hinglish" -> "✅ Done! Maine **${extractedParty.name}** ko aapke **$typeStr** list mein add kar diya hai$balNote। Ab aap inka hisab-kitab shuru kar sakte hain!"
                else -> "✅ Success! I have added **${extractedParty.name}** as a **$typeStr** to your ledger$balNote. You can now record credit and payments directly!"
            }
        }

        // Saying NO gracefully to unrelated requests
        if (q.contains("weather") || q.contains("movie") || q.contains("joke") || q.contains("cricket") ||
            q.contains("song") || q.contains("recipe") || q.contains("game") || q.contains("code") ||
            q.contains("match") || q.contains("film") || q.contains("dance") || q.contains("story")
        ) {
            return when (languageCode) {
                "hi" -> "माफ़ कीजिए, मैं केवल आपके खाता (Kirio Khata) और दुकान के हिसाब-किताब में मदद कर सकता हूँ। अन्य जानकारी के लिए मैं असमर्थ हूँ।"
                "gu" -> "માફ કરશો, હું ફક્ત તમારા ખાતાવહી (Kirio Khata) અને દુકાનના હિસાબમાં જ મદદ કરી શકું છું. અન્ય પ્રશ્નો માટે હું ના પાડું છું."
                "hinglish" -> "Sorry, main sirf aapke Kirio Khata aur business hisab-kitab mein help kar sakta hoon. Baaki cheezon ke liye I cannot do."
                else -> "No, I cannot help with that. I am your specialized Kirio Khata accountant and can only help with your customers, suppliers, dues, cashbook, and bills."
            }
        }

        // Help in trouble
        if (q.contains("help") || q.contains("trouble") || q.contains("madad") || q.contains("problem") ||
            q.contains("kaise") || q.contains("કેવી રીતે") || q.contains("સમસ્યા") || q.contains("મુશ્કેલી") ||
            q.contains("dikkat") || q.contains("samajh nahi")
        ) {
            return when (languageCode) {
                "hi" -> """
                    चिंता न करें! मैं आपकी पूरी मदद करूँगा:
                    1. **नया ग्राहक जोड़ने के लिए:** नीचे लाल '+ ADD CUSTOMER' बटन दबाएं (या मुझे बोलें: 'Add customer रमेश')।
                    2. **उधार लिखने के लिए:** ग्राहक के नाम पर टैप करें और लाल 'YOU GAVE ₹' दबाएं।
                    3. **रुपये मिलने पर:** ग्राहक पर टैप करके हरा 'YOU GOT ₹' दबाएं।
                    4. **पेमेंट पक्का करने के लिए:** एंट्री के सामने सही का निशान (✓) लगाएं।
                    5. **तगादा भेजने के लिए:** नाम के आगे 'REMIND >' पर टैप करके व्हाट्सएप पेमेंट लिंक भेजें।
                """.trimIndent()
                "gu" -> """
                    ચિંતા ના કરો! હું તમારી સંપૂર્ણ મદદ કરીશ:
                    1. **નવો ગ્રાહક ઉમેરવા:** નીચે '+ ADD CUSTOMER' બટન દબાવો (અથવા મને કહો: 'Add customer રમેશ')।
                    2. **ઉધાર આપ્યા હોય તો:** ગ્રાહક પર ક્લિક કરી લાલ 'YOU GAVE ₹' દબાવો.
                    3. **જમા થયા હોય તો:** લીલું 'YOU GOT ₹' બટન દબાવો.
                    4. **ચૂકવણી કન્ફર્મ કરવા:** હિસાબની બાજુમાં સાચું (✓) ટિક કરો.
                    5. **રીમાઇન્ડર મોકલવા:** 'REMIND >' દબાવી સીધો વોટ્સએપ મેસેજ મોકલો.
                """.trimIndent()
                "hinglish" -> """
                    Fikar mat karein! Kirio use karna bohot simple hai:
                    1. **Customer Add:** Neeche '+ ADD CUSTOMER' dabayein ya mujhe bolein 'Add customer Ramesh'.
                    2. **Udhaar Dena:** Customer par tap karke RED 'YOU GAVE ₹' dabayein.
                    3. **Paise Milna:** Customer par tap karke GREEN 'YOU GOT ₹' dabayein.
                    4. **Manual Tick:** Entry ke paas Tick (✓) laga kar confirm karein ki transaction ho gaya hai.
                    5. **Reminder:** 'REMIND >' par tap karke direct WhatsApp reminder bhejein.
                """.trimIndent()
                else -> """
                    Don't worry! Here is how Kirio works step-by-step:
                    1. **Add Customer / Supplier:** Tap the bottom '+ ADD CUSTOMER' button (or tell me: "Add customer Ramesh 9876543210").
                    2. **Record Credit Given:** Tap the party and press RED 'YOU GAVE ₹'.
                    3. **Record Payment Received:** Tap the party and press GREEN 'YOU GOT ₹'.
                    4. **Manual Confirmation Tick:** Tap the checkbox next to the entry to mark it confirmed.
                    5. **Send Reminders:** Tap 'REMIND >' next to any debtor to send an instant WhatsApp/SMS reminder with UPI link.
                """.trimIndent()
            }
        }

        // Pending Dues / Who owes me
        if (q.contains("owe") || q.contains("pending") || q.contains("collect") || q.contains("baaki") ||
            q.contains("બાકી") || q.contains("લેવાના") || q.contains("लेना") || q.contains("उधारी")
        ) {
            if (debtors.isEmpty()) {
                return when (languageCode) {
                    "hi" -> "बधाई हो! आपकी **कोई ग्राहक उधारी बाक़ी नहीं है** (You Will Get: ₹0)!"
                    "gu" -> "અભિનંદન! હાલમાં કોઈ ગ્રાહક પાસે **રૂપિયા બાકી નથી** (You Will Get: ₹0)!"
                    else -> "Great news! You have **0 pending customer dues** right now (Total You Will Get: ${currency}0)."
                }
            }
            val list = debtors.take(5).joinToString("\n") { "• **${it.name}**: $currency${formatNum(it.balance)}" }
            return when (languageCode) {
                "hi" -> "यहाँ आपकी बाक़ी उधारी की सूची है (**कुल लेना है: $currency${formatNum(totalGet)}**):\n\n$list\n\n💡 व्हाट्सएप तगादा भेजने के लिए नाम के आगे 'REMIND >' दबाएं।"
                "gu" -> "અહીં તમારી બાકી ઉઘરાણીની યાદી છે (**કુલ લેવાના: $currency${formatNum(totalGet)}**):\n\n$list\n\n💡 વોટ્સએપ રીમાઇન્ડર મોકલવા માટે 'REMIND >' દબાવો."
                else -> "Here are your pending collections (**Total You Will Get: $currency${formatNum(totalGet)}**):\n\n$list\n\n💡 Tap 'REMIND >' next to their name to send a WhatsApp payment link."
            }
        }

        // Supplier payables
        if (q.contains("supplier") || q.contains("give") || q.contains("dena") || q.contains("આપવાના") ||
            q.contains("દેવાના") || q.contains("व्यापारी") || q.contains("વેપારી")
        ) {
            if (creditors.isEmpty()) {
                return when (languageCode) {
                    "hi" -> "आपके पास **सप्लायर्स का कोई देना बाक़ी नहीं है** (You Will Give: ₹0)।"
                    "gu" -> "તમારે **વેપારીઓને કોઈ રૂપિયા આપવાના બાકી નથી** (You Will Give: ₹0)."
                    else -> "You have **no pending supplier payables** (Total You Will Give: ${currency}0)."
                }
            }
            val list = creditors.take(5).joinToString("\n") { "• **${it.name}**: $currency${formatNum(kotlin.math.abs(it.balance))}" }
            return when (languageCode) {
                "hi" -> "सप्लायर्स को देने योग्य राशि (**कुल देना है: $currency${formatNum(totalGive)}**):\n\n$list"
                "gu" -> "વેપારીઓને ચૂકવવાપાત્ર રકમ (**કુલ આપવાના: $currency${formatNum(totalGive)}**):\n\n$list"
                else -> "Your pending supplier dues (**Total You Will Give: $currency${formatNum(totalGive)}**):\n\n$list"
            }
        }

        return when (languageCode) {
            "hi" -> """
                नमस्ते! मैं **$businessName** का **किरिओ AI मुंशी** हूँ।
                
                • **कुल लेना है:** $currency${formatNum(totalGet)}
                • **कुल देना है:** $currency${formatNum(totalGive)}
                • **गल्ला (कैश ड्रावर):** $currency${formatNum(cashIn - cashOut)}
                
                आप मुझसे हिंदी, गुजराती या अंग्रेजी में बोलकर बात कर सकते हैं!
                बोलें: *"रमेश को 9876543210 ग्राहक जोड़ो"* या *"बाक़ी रुपये की लिस्ट दिखाओ"*
            """.trimIndent()
            "gu" -> """
                નમસ્તે! હું **$businessName** નો **કિરીઓ AI મુન્શી** છું.
                
                • **લેવાના બાકી:** $currency${formatNum(totalGet)}
                • **આપવાના બાકી:** $currency${formatNum(totalGive)}
                • **રોકડ ગલ્લો:** $currency${formatNum(cashIn - cashOut)}
                
                તમે મારી સાથે ગુજરાતી, હિન્દી કે અંગ્રેજીમાં બોલીને વાત કરી શકો છો!
                બોલો: *"રમેશ 9876543210 ગ્રાહક ઉમેરો"* અથવા *"કોની પાસે બાકી છે?"*
            """.trimIndent()
            else -> """
                Namaste! I am your **Kirio AI Munshi** for **$businessName**.
                
                • **You Will Get:** $currency${formatNum(totalGet)}
                • **You Will Give:** $currency${formatNum(totalGive)}
                • **Net Drawer Cash:** $currency${formatNum(cashIn - cashOut)}
                
                You can speak or type in **English, Hindi, Gujarati, or Hinglish**!
                Say: *"Add customer Suresh 9876543210"* or *"Who owes me money?"*
            """.trimIndent()
        }
    }

    private fun formatNum(amount: Double): String {
        val fmt = NumberFormat.getNumberInstance(Locale.Builder().setLanguage("en").setRegion("IN").build())
        fmt.maximumFractionDigits = 0
        return fmt.format(amount)
    }
}
