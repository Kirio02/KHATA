package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.ChatMessage
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun ChatbotScreen(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var autoSpeakReplies by remember { mutableStateOf(true) }
    var voiceLanguageCode by remember { mutableStateOf("hi-IN") } // default Hindi / Indian English
    val listState = rememberLazyListState()

    // Initialize TextToSpeech engine so AI can talk
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                ttsInstance?.language = Locale.Builder().setLanguage("hi").setRegion("IN").build()
            }
        }
        textToSpeech = ttsInstance

        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    fun speakText(text: String) {
        val tts = textToSpeech ?: return
        if (!isTtsReady) return
        tts.stop()

        // Clean markdown asterisks for smooth natural speech
        val spokenText = text.replace("**", "").replace("*", "").replace("#", "").replace("•", "")

        // Choose appropriate locale based on script
        val locale = when {
            text.any { it in '\u0A80'..'\u0AFF' } -> Locale.Builder().setLanguage("gu").setRegion("IN").build()
            text.any { it in '\u0900'..'\u097F' } -> Locale.Builder().setLanguage("hi").setRegion("IN").build()
            else -> Locale.Builder().setLanguage("en").setRegion("IN").build()
        }
        runCatching {
            tts.language = locale
            tts.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, "KirioAiUtterance")
        }
    }

    // Auto-speak new AI response when it arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
            val lastMsg = messages.last()
            if (!lastMsg.isFromUser && autoSpeakReplies && isTtsReady) {
                speakText(lastMsg.text)
            }
        }
    }

    // Speech-to-Text Voice Launcher (Supports English, Hindi, Gujarati)
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                inputText = spoken
                onSendMessage(spoken)
            }
        }
    }

    fun launchVoiceRecognition(langCode: String) {
        voiceLanguageCode = langCode
        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langCode)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langCode)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                when (langCode) {
                    "gu-IN" -> "ગુજરાતીમાં બોલો (જેમ કે: 'રમેશ 9876543210 ઉમેરો')…"
                    "hi-IN" -> "हिंदी में बोलिए (जैसे: 'रमेश 9876543210 जोड़ो')…"
                    else -> "Speak in English / Hinglish…"
                }
            )
        }
        runCatching { speechLauncher.launch(speechIntent) }
    }

    // Contact Picker for AI Recognition
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { contactUri ->
        contactUri?.let { uri ->
            runCatching {
                val projection = arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.HAS_PHONE_NUMBER
                )
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                        val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                        val pickedName = cursor.getString(nameIndex) ?: ""
                        val hasPhone = cursor.getInt(hasPhoneIndex) > 0
                        val contactId = cursor.getString(idIndex)
                        var pickedPhone = ""

                        if (hasPhone) {
                            context.contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                                arrayOf(contactId),
                                null
                            )?.use { phoneCursor ->
                                if (phoneCursor.moveToFirst()) {
                                    pickedPhone = phoneCursor.getString(0) ?: ""
                                }
                            }
                        }

                        val aiPrompt = "Add customer $pickedName with phone $pickedPhone"
                        inputText = aiPrompt
                        onSendMessage(aiPrompt)
                    }
                }
            }
        }
    }

    val quickPrompts = listOf(
        "Help! I feel in trouble" to "How do I use Kirio?",
        "Who owes me money?" to "Who owes me money?",
        "મારે કોની પાસેથી બાકી લેવાના છે?" to "બાકી ઉઘરાણી બતાવો",
        "दुकान की बाक़ी उधारी दिखाओ" to "बाक़ी उधारी दिखाओ",
        "Add customer Ramesh 9876543210" to "Add customer Ramesh",
        "Can you play cricket / tell movie?" to "Tell me movie news"
    )

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF7F9FC))) {
        // High-contrast Header in Deep Navy
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PrimaryNavy)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Munshi",
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kirio AI Munshi (વાત કરતો સહાયક)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Honest Ledger Voice Assistant • English / हिन्दी / ગુજરાતી",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFBBDEFB)
                    )
                }

                // Auto-speak toggle
                IconButton(
                    onClick = {
                        autoSpeakReplies = !autoSpeakReplies
                        if (!autoSpeakReplies) textToSpeech?.stop()
                    },
                    modifier = Modifier.testTag("ai_toggle_speech_button")
                ) {
                    Icon(
                        imageVector = if (autoSpeakReplies) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Toggle Speech",
                        tint = if (autoSpeakReplies) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.7f)
                    )
                }

                IconButton(
                    onClick = {
                        textToSpeech?.stop()
                        onClearChat()
                    },
                    modifier = Modifier.testTag("ai_clear_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Chat",
                        tint = Color.White
                    )
                }
            }
        }

        // Voice Language selector bar
        Surface(
            color = Color.White,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Voice Language:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                FilterChip(
                    selected = voiceLanguageCode == "gu-IN",
                    onClick = { voiceLanguageCode = "gu-IN" },
                    label = { Text("ગુજરાતી (GU)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryNavy,
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = voiceLanguageCode == "hi-IN",
                    onClick = { voiceLanguageCode = "hi-IN" },
                    label = { Text("हिंदी (HI)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryNavy,
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = voiceLanguageCode == "en-IN",
                    onClick = { voiceLanguageCode = "en-IN" },
                    label = { Text("English / Hinglish") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryNavy,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // Quick Suggestion Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickPrompts.forEach { (prompt, label) ->
                AssistChip(
                    onClick = {
                        inputText = prompt
                        onSendMessage(prompt)
                    },
                    label = { Text(label, fontWeight = FontWeight.Medium) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color.White,
                        labelColor = PrimaryNavy
                    )
                )
            }
        }

        HorizontalDivider(color = BorderSubtle)

        // Chat Message Stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.isFromUser
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (!isUser) {
                        // Speaker button to replay speech aloud
                        IconButton(
                            onClick = { speakText(msg.text) },
                            modifier = Modifier
                                .size(36.dp)
                                .padding(end = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Listen to AI speak",
                                tint = PrimaryNavy,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        ),
                        color = if (isUser) PrimaryNavy else Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier.widthIn(max = 320.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp
                                ),
                                fontWeight = FontWeight.Normal,
                                color = if (isUser) Color.White else TextPrimary
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = PrimaryNavy,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "AI Munshi is listening & analyzing ledger…",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Bottom Input Row: Mic (Voice), Contacts, Text Field & Send Button
        Surface(
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            color = Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Voice Recognition Mic button (Speaks in chosen language)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F0FE))
                        .clickable { launchVoiceRecognition(voiceLanguageCode) }
                        .testTag("ai_voice_mic_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Speak to AI",
                        tint = PrimaryNavy,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Pick from Contacts shortcut for AI auto-adding
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0F2F1))
                        .clickable { contactPickerLauncher.launch(null) }
                        .testTag("ai_pick_contact_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Contacts,
                        contentDescription = "Pick Contact for AI",
                        tint = Color(0xFF00695C),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = "Speak or ask in EN / हिंदी / ગુજરાતી…",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chatbot_input_field")
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = !isLoading && inputText.isNotBlank(),
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (inputText.isNotBlank()) PrimaryNavy else Color(0xFFE0E0E0),
                            shape = CircleShape
                        )
                        .testTag("chatbot_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) Color.White else Color(0xFF9E9E9E),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
