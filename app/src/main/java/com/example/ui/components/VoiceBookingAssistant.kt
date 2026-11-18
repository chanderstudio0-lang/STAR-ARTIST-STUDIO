package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.model.AppLanguage
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.VoiceBookingPrefill
import com.example.util.ParsedVoiceBookingCommand
import com.example.util.VoiceBookingCommandParser
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import java.util.Locale

private enum class VoiceBookingMicState {
    IDLE,
    LISTENING,
    PARSING,
    ERROR
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceBookingAssistantPanel(
    language: AppLanguage,
    defaultClientName: String = "",
    defaultPhoneNumber: String = "",
    isEmailVerified: Boolean = true,
    isSubmitting: Boolean = false,
    onParseCommand: suspend (String) -> ParsedVoiceBookingCommand = { transcript ->
        VoiceBookingCommandParser.parseLocally(transcript)
    },
    onSubmitBooking: (
        clientName: String,
        contactNumber: String,
        serviceType: String,
        preferredDate: String,
        preferredTime: String,
        location: String,
        notes: String,
        onSuccess: (bookingId: String) -> Unit
    ) -> Unit,
    onApplyToFullForm: (VoiceBookingPrefill) -> Unit = {},
    onViewBookings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser = remember { runCatching { Firebase.auth.currentUser }.getOrNull() }

    var micState by remember { mutableStateOf(VoiceBookingMicState.IDLE) }
    var commandText by remember { mutableStateOf("") }
    var partialTranscript by remember { mutableStateOf("") }
    var statusNotice by remember { mutableStateOf<String?>(null) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var soundLevelRms by remember { mutableFloatStateOf(0f) }

    var parsedCommand by remember { mutableStateOf<ParsedVoiceBookingCommand?>(null) }
    var editableCategory by remember { mutableStateOf("Photography") }
    var editableService by remember { mutableStateOf("Photography") }
    var editableDate by remember { mutableStateOf("") }
    var editableTime by remember { mutableStateOf("") }
    var editableLocation by remember { mutableStateOf(VoiceBookingCommandParser.LOCATION_STUDIO) }
    var editableClientName by remember {
        mutableStateOf(defaultClientName.ifBlank { currentUser?.displayName.orEmpty() })
    }
    var editablePhone by remember { mutableStateOf(normalizeBookingPhone(defaultPhoneNumber)) }
    var editableNotes by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var confirmedBookingId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(defaultClientName, defaultPhoneNumber) {
        if (editableClientName.isBlank() && defaultClientName.isNotBlank()) {
            editableClientName = defaultClientName
        }
        if (editablePhone.isBlank() && defaultPhoneNumber.isNotBlank()) {
            editablePhone = normalizeBookingPhone(defaultPhoneNumber)
        }
    }

    // Spoken confirmation TTS toggle & lifecycle
    var ttsEnabled by remember { mutableStateOf(true) }
    var isTtsReady by remember { mutableStateOf(false) }
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(Unit) {
        val tts = runCatching {
            TextToSpeech(context) { status ->
                isTtsReady = (status == TextToSpeech.SUCCESS)
            }
        }.getOrNull()
        textToSpeech = tts
        onDispose {
            runCatching {
                tts?.stop()
                tts?.shutdown()
            }
            textToSpeech = null
        }
    }

    fun speakConfirmation(parsed: ParsedVoiceBookingCommand) {
        if (!ttsEnabled || !isTtsReady) return
        val spokenText = if (language == AppLanguage.HINDI) {
            parsed.spokenConfirmationHi.ifBlank { parsed.spokenConfirmationEn }
        } else {
            parsed.spokenConfirmationEn.ifBlank { parsed.spokenConfirmationHi }
        }
        if (spokenText.isBlank()) return
        runCatching {
            val locale = if (language == AppLanguage.HINDI) Locale("hi", "IN") else Locale("en", "IN")
            textToSpeech?.language = locale
            textToSpeech?.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, "voice_booking_summary")
        }
    }

    fun submitParsedBooking() {
        val cleanName = editableClientName.trim()
        val cleanPhone = normalizeBookingPhone(editablePhone)
        val cleanDate = editableDate.trim()
        val cleanTime = editableTime.trim()
        val cleanService = editableService.trim()

        if (currentUser != null && !isEmailVerified) {
            validationError = if (language == AppLanguage.HINDI) {
                "बुक करने से पहले कृपया अपना ईमेल सत्यापित करें।"
            } else {
                "Please verify your email address before reserving a studio session."
            }
            return
        }
        if (cleanName.length < 2) {
            validationError = if (language == AppLanguage.HINDI) {
                "कृपया कम से कम 2 अक्षरों का नाम दर्ज करें।"
            } else {
                "Please enter client name (at least 2 characters)."
            }
            return
        }
        if (cleanPhone.length !in 8..15) {
            validationError = if (language == AppLanguage.HINDI) {
                "कृपया मान्य संपर्क नंबर (8–15 अंक) दर्ज करें।"
            } else {
                "Please enter a valid phone number (8–15 digits)."
            }
            return
        }
        if (cleanDate.isBlank() || cleanTime.isBlank()) {
            validationError = if (language == AppLanguage.HINDI) {
                "कृपया मान्य तारीख और समय दर्ज करें।"
            } else {
                "Please provide a valid date and time slot."
            }
            return
        }

        validationError = null
        onSubmitBooking(
            cleanName,
            cleanPhone,
            cleanService,
            cleanDate,
            cleanTime,
            editableLocation.ifBlank { VoiceBookingCommandParser.LOCATION_STUDIO },
            editableNotes.trim()
        ) { newId ->
            confirmedBookingId = newId
            if (ttsEnabled && isTtsReady) {
                val successSpeech = if (language == AppLanguage.HINDI) {
                    "बुकिंग दर्ज हो गई है।"
                } else {
                    "Booking request submitted for $cleanService on $cleanDate."
                }
                runCatching {
                    textToSpeech?.speak(successSpeech, TextToSpeech.QUEUE_FLUSH, null, "voice_booking_done")
                }
            }
        }
    }

    fun processCommand(rawCommand: String) {
        val clean = rawCommand.trim()
        if (clean.isEmpty()) {
            statusNotice = if (language == AppLanguage.HINDI) {
                "कृपया पहले वॉइस कमांड बोलें या नीचे टाइप करें।"
            } else {
                "Speak a command or select one of the quick voice prompts below."
            }
            return
        }
        commandText = clean
        partialTranscript = ""
        validationError = null
        confirmedBookingId = null
        micState = VoiceBookingMicState.PARSING

        scope.launch {
            val result = onParseCommand(clean)
            if (result.isConfirmCommand && parsedCommand != null) {
                micState = VoiceBookingMicState.IDLE
                submitParsedBooking()
                return@launch
            }
            parsedCommand = result
            editableCategory = result.intentCategory
            editableService = result.serviceType
            editableDate = result.preferredDate
            editableTime = result.preferredTime
            editableLocation = result.location
            if (result.extractedClientName.isNotBlank()) {
                editableClientName = result.extractedClientName
            }
            if (result.extractedPhoneNumber.isNotBlank()) {
                editablePhone = normalizeBookingPhone(result.extractedPhoneNumber)
            }
            editableNotes = result.extractedNotes
            micState = VoiceBookingMicState.IDLE
            statusNotice = null
            speakConfirmation(result)
        }
    }

    val latestProcessCommand by rememberUpdatedState(::processCommand)
    val latestLanguage by rememberUpdatedState(language)

    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            }
            speechRecognizer = null
        }
    }

    fun startVoiceRecognition() {
        statusNotice = null
        partialTranscript = ""
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            micState = VoiceBookingMicState.ERROR
            statusNotice = if (language == AppLanguage.HINDI) {
                "इस डिवाइस/एम्युलेटर पर स्पीच सर्विस उपलब्ध नहीं है — नीचे से सैंपल चुनें या कमांड लिखें।"
            } else {
                "On-device Speech Service unavailable on this emulator — tap a sample command or type your request below."
            }
            return
        }

        try {
            speechRecognizer?.destroy()
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    micState = VoiceBookingMicState.LISTENING
                }

                override fun onBeginningOfSpeech() {
                    micState = VoiceBookingMicState.LISTENING
                }

                override fun onRmsChanged(rmsdB: Float) {
                    soundLevelRms = rmsdB
                }

                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    micState = VoiceBookingMicState.ERROR
                    statusNotice = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH ->
                            if (latestLanguage == AppLanguage.HINDI) "आवाज़ स्पष्ट नहीं थी, कृपया दोबारा बोलें।"
                            else "No speech matched. Speak clearly or try a sample prompt below."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                            if (latestLanguage == AppLanguage.HINDI) "समय समाप्त हुआ — माइक पर फिर से टैप करें।"
                            else "Listening timed out. Tap the microphone to try again."
                        SpeechRecognizer.ERROR_AUDIO ->
                            if (latestLanguage == AppLanguage.HINDI) "माइक्रोफ़ोन ऑडियो त्रुटि।"
                            else "Audio capture error. Check microphone access or use quick prompts."
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                            if (latestLanguage == AppLanguage.HINDI) "नेटवर्क त्रुटि — कमांड टाइप करके भी बुक कर सकते हैं।"
                            else "Speech network unavailable — you can still type or tap a voice command below."
                        else ->
                            if (latestLanguage == AppLanguage.HINDI) "वाक् पहचान त्रुटि ($error)"
                            else "Speech recognition unavailable (code $error). You can type your command below."
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val finalSpeech = matches?.firstOrNull()?.trim().orEmpty()
                    if (finalSpeech.isNotBlank()) {
                        latestProcessCommand(finalSpeech)
                    } else if (partialTranscript.isNotBlank()) {
                        latestProcessCommand(partialTranscript)
                    } else {
                        micState = VoiceBookingMicState.IDLE
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                        .orEmpty()
                    if (partial.isNotBlank()) {
                        partialTranscript = partial
                        commandText = partial
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    if (language == AppLanguage.HINDI) "hi-IN" else "en-IN"
                )
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            recognizer.startListening(intent)
            micState = VoiceBookingMicState.LISTENING
        } catch (e: Exception) {
            micState = VoiceBookingMicState.ERROR
            statusNotice = e.localizedMessage ?: "Failed to start microphone speech recognizer."
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showPermissionRationale = false
            startVoiceRecognition()
        } else {
            showPermissionRationale = true
            micState = VoiceBookingMicState.IDLE
        }
    }

    fun handleMicTap() {
        if (micState == VoiceBookingMicState.LISTENING) {
            runCatching { speechRecognizer?.stopListening() }
            if (partialTranscript.isNotBlank()) {
                processCommand(partialTranscript)
            } else {
                micState = VoiceBookingMicState.IDLE
            }
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                showPermissionRationale = false
                startVoiceRecognition()
            } else {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "voice_booking_halo")
    val haloScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.2.dp, Brush.verticalGradient(listOf(GoldPrimary, StudioOutline))),
                RoundedCornerShape(20.dp)
            )
            .testTag("voice_booking_assistant"),
        colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header + TTS Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(GoldContainer)
                            .border(1.dp, GoldPrimary.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Voice Booking Assistant",
                            tint = GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (language == AppLanguage.HINDI) "वॉइस-कंट्रोल्ड बुकिंग" else "Voice-Controlled Booking",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldLight
                        )
                        Text(
                            text = if (language == AppLanguage.HINDI)
                                "बोलकर फोटोग्राफी या मॉडलिंग सत्र बुक करें"
                            else
                                "Speak naturally to book Photography or Modeling",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = { ttsEnabled = !ttsEnabled },
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("voice_tts_toggle_btn")
                ) {
                    Icon(
                        imageVector = if (ttsEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = if (ttsEnabled) "Spoken Confirmation On" else "Spoken Confirmation Off",
                        tint = if (ttsEnabled) GoldPrimary else Color.Gray
                    )
                }
            }

            // Interactive Microphone Stage
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(StudioCardSurface)
                    .border(
                        1.dp,
                        if (micState == VoiceBookingMicState.LISTENING) GoldPrimary else StudioOutline,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (micState == VoiceBookingMicState.LISTENING) {
                        Box(
                            modifier = Modifier
                                .size(86.dp)
                                .scale(haloScale)
                                .clip(CircleShape)
                                .background(GoldPrimary.copy(alpha = 0.22f))
                        )
                    }
                    Surface(
                        onClick = { handleMicTap() },
                        modifier = Modifier
                            .size(72.dp)
                            .testTag("voice_booking_mic_button"),
                        shape = CircleShape,
                        color = when (micState) {
                            VoiceBookingMicState.LISTENING -> Color(0xFFE53935)
                            VoiceBookingMicState.PARSING -> GoldDark
                            else -> GoldPrimary
                        },
                        shadowElevation = 6.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            when (micState) {
                                VoiceBookingMicState.LISTENING -> Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop Listening",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                                VoiceBookingMicState.PARSING -> CircularProgressIndicator(
                                    color = StudioBlack,
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 3.dp
                                )
                                else -> Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Tap Microphone to Book by Voice",
                                    tint = StudioBlack,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = when (micState) {
                        VoiceBookingMicState.LISTENING ->
                            if (language == AppLanguage.HINDI) "सुन रहे हैं... अभी बोलें (टैप करके रोकें)" else "Listening... Speak your shoot command"
                        VoiceBookingMicState.PARSING ->
                            if (language == AppLanguage.HINDI) "आपकी बुकिंग समझ रहे हैं..." else "Parsing booking details..."
                        else ->
                            if (language == AppLanguage.HINDI)
                                "माइक टैप करें और बोलें: \"कल शाम 4:30 बजे मॉडलिंग पोर्टफोलियो शूट बुक करें\""
                            else
                                "Tap mic & say: \"Book a modeling portfolio shoot tomorrow at 4:30 PM\""
                    },
                    color = if (micState == VoiceBookingMicState.LISTENING) GoldLight else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (micState == VoiceBookingMicState.LISTENING) FontWeight.Bold else FontWeight.Medium
                )

                AnimatedVisibility(visible = micState == VoiceBookingMicState.LISTENING) {
                    VoiceBookingWaveform(
                        soundLevelRms = soundLevelRms,
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height(26.dp)
                            .testTag("voice_booking_waveform")
                    )
                }

                if (partialTranscript.isNotBlank() || commandText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(GoldContainer.copy(alpha = 0.45f))
                            .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "\"${partialTranscript.ifBlank { commandText }}\"",
                            color = TextPrimary,
                            fontSize = 12.5.sp,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.testTag("voice_booking_transcript_text")
                        )
                    }
                }
            }

            // Permission Rationale
            AnimatedVisibility(visible = showPermissionRationale, enter = fadeIn(), exit = fadeOut()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_booking_permission_rationale"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF281D0A)),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MicOff, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == AppLanguage.HINDI) "माइक्रोफ़ोन अनुमति आवश्यक है" else "Microphone Permission Needed",
                                    color = GoldLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                            }
                            IconButton(
                                onClick = { showPermissionRationale = false },
                                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss Permission Notice", tint = Color.Gray)
                            }
                        }
                        Text(
                            text = if (language == AppLanguage.HINDI)
                                "बोलकर फोटोग्राफी और मॉडलिंग सत्र बुक करने के लिए कृपया माइक्रोफ़ोन की अनुमति दें।"
                            else
                                "Allow microphone access so Star Artist Studio can listen to natural voice booking commands.",
                            color = TextSecondary,
                            fontSize = 11.5.sp
                        )
                        Button(
                            onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("voice_booking_grant_permission_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = StudioBlack),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (language == AppLanguage.HINDI) "माइक अनुमति दें" else "Allow Microphone Access",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Status / Speech Recognition Notice
            statusNotice?.let { notice ->
                Text(
                    text = notice,
                    color = GoldLight,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.testTag("voice_booking_status_notice")
                )
            }

            // Quick Spoken Command Examples + Editable Transcript Refinement
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (language == AppLanguage.HINDI) "त्वरित वॉइस कमांड उदाहरण:" else "Quick Voice Command Examples:",
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )

                val photoSample = "Book high-fashion editorial photography tomorrow at 10 AM in studio"
                val modelSample = "Schedule a modeling comp-card shoot this Saturday at 4:30 PM outdoor"
                val bridalSample = "Reserve royal pre-wedding bridal photography next week noon"

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = commandText == photoSample,
                        onClick = { processCommand(photoSample) },
                        label = { Text("Editorial Photo • Tomorrow 10 AM", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("voice_sample_chip_photography"),
                        colors = voiceChipColors(commandText == photoSample),
                        border = voiceChipBorder(commandText == photoSample)
                    )
                    FilterChip(
                        selected = commandText == modelSample,
                        onClick = { processCommand(modelSample) },
                        label = { Text("Modeling Comp-Card • Sat 4:30 PM", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("voice_sample_chip_modeling"),
                        colors = voiceChipColors(commandText == modelSample),
                        border = voiceChipBorder(commandText == modelSample)
                    )
                    FilterChip(
                        selected = commandText == bridalSample,
                        onClick = { processCommand(bridalSample) },
                        label = { Text("Bridal Pre-Wedding • Next Week", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("voice_sample_chip_bridal"),
                        colors = voiceChipColors(commandText == bridalSample),
                        border = voiceChipBorder(commandText == bridalSample)
                    )
                }

                OutlinedTextField(
                    value = commandText,
                    onValueChange = { commandText = it },
                    label = {
                        Text(
                            if (language == AppLanguage.HINDI) "बोला गया या टाइप किया गया बुकिंग कमांड" else "Spoken / Typed Booking Command"
                        )
                    },
                    placeholder = {
                        Text("e.g. Book modeling portfolio shoot tomorrow 2:30 PM studio")
                    },
                    maxLines = 2,
                    trailingIcon = {
                        if (commandText.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    commandText = ""
                                    parsedCommand = null
                                    confirmedBookingId = null
                                    validationError = null
                                },
                                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Command", tint = TextSecondary)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_booking_command_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = voiceFieldColors()
                )

                Button(
                    onClick = { processCommand(commandText) },
                    enabled = commandText.isNotBlank() && micState != VoiceBookingMicState.PARSING,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("voice_booking_parse_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldContainer,
                        contentColor = GoldLight,
                        disabledContainerColor = StudioCardSurface,
                        disabledContentColor = TextSecondary
                    ),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.HINDI) "कमांड से बुकिंग विवरण निकालें" else "Extract Booking Details from Command",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                }
            }

            // Success Confirmation Card OR Parsed Booking Preview Card
            val currentConfirmedId = confirmedBookingId
            val activeParsed = parsedCommand
            if (currentConfirmedId != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, SuccessGreen, RoundedCornerShape(16.dp))
                        .testTag("voice_booking_success_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0E2618)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == AppLanguage.HINDI) "वॉइस बुकिंग सफलतापूर्वक दर्ज!" else "Voice Booking Confirmed!",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        }
                        Text(
                            text = "Reference ID: $currentConfirmedId",
                            color = GoldLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$editableService • $editableDate at $editableTime ($editableLocation)",
                            color = Color(0xFFD6EADF),
                            fontSize = 12.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onViewBookings,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("voice_success_view_bookings_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = StudioBlack),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = if (language == AppLanguage.HINDI) "मेरी बुकिंग्स" else "My Bookings",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    confirmedBookingId = null
                                    parsedCommand = null
                                    commandText = ""
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("voice_book_another_btn"),
                                border = BorderStroke(1.dp, GoldPrimary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = if (language == AppLanguage.HINDI) "नया वॉइस शूट" else "Book Another",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            } else if (activeParsed != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.2.dp, GoldPrimary.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                        .testTag("voice_parsed_booking_card"),
                    colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (language == AppLanguage.HINDI) "समझा गया सत्र विवरण" else "Parsed Session Preview",
                                color = GoldLight,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(GoldContainer)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (activeParsed.parsedByEngine == "GEMINI_3_8_FLASH") "GEMINI NLU" else "VOICE NLU",
                                    color = GoldPrimary,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        // Quick Category Switcher (Photography / Modeling / Video Production)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Triple("Photography", Icons.Default.CameraAlt, "Photography"),
                                Triple("Modeling", Icons.Default.Person, "Modeling"),
                                Triple("Video", Icons.Default.Videocam, "Video Production")
                            ).forEach { (shortLabel, icon, catName) ->
                                val selected = editableCategory == catName
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        editableCategory = catName
                                        editableService = catName
                                    },
                                    label = { Text(shortLabel, fontSize = 11.sp) },
                                    leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("voice_cat_${shortLabel.lowercase()}"),
                                    colors = voiceChipColors(selected),
                                    border = voiceChipBorder(selected)
                                )
                            }
                        }

                        // Structured Extracted Summary Lines
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(StudioElevatedSurface)
                                .border(1.dp, StudioOutline, RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            VoiceSummaryRow(
                                icon = Icons.Default.EventAvailable,
                                label = if (language == AppLanguage.HINDI) "पैकेज" else "Service",
                                value = editableService
                            )
                            VoiceSummaryRow(
                                icon = Icons.Default.CalendarMonth,
                                label = if (language == AppLanguage.HINDI) "दिनांक" else "Date",
                                value = editableDate
                            )
                            VoiceSummaryRow(
                                icon = Icons.Default.Schedule,
                                label = if (language == AppLanguage.HINDI) "समय" else "Time",
                                value = editableTime
                            )
                            VoiceSummaryRow(
                                icon = Icons.Default.LocationOn,
                                label = if (language == AppLanguage.HINDI) "स्थान" else "Location",
                                value = editableLocation
                            )
                        }

                        // Inline Editable Client Details
                        OutlinedTextField(
                            value = editableClientName,
                            onValueChange = {
                                editableClientName = it
                                validationError = null
                            },
                            label = { Text(if (language == AppLanguage.HINDI) "ग्राहक का नाम" else "Client Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = GoldPrimary) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("voice_booking_name_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = voiceFieldColors()
                        )

                        OutlinedTextField(
                            value = editablePhone,
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }
                                if (digits.length <= 15) {
                                    editablePhone = digits
                                    validationError = null
                                }
                            },
                            label = { Text(if (language == AppLanguage.HINDI) "फ़ोन नंबर" else "Contact Phone Number") },
                            placeholder = { Text("10-digit mobile number") },
                            leadingIcon = { Icon(Icons.Default.Call, contentDescription = null, tint = GoldPrimary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("voice_booking_phone_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = voiceFieldColors()
                        )

                        OutlinedTextField(
                            value = editableNotes,
                            onValueChange = { editableNotes = it },
                            label = { Text(if (language == AppLanguage.HINDI) "सत्र नोट्स" else "Voice Notes & Instructions") },
                            maxLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("voice_booking_notes_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = voiceFieldColors()
                        )

                        if (!isEmailVerified) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GoldContainer)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.MarkEmailUnread, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (language == AppLanguage.HINDI)
                                        "बुकिंग दर्ज करने से पहले कृपया अपना ईमेल सत्यापित करें।"
                                    else
                                        "Email verification is required before final booking submission.",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        validationError?.let { err ->
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("voice_booking_error_text")
                            )
                        }

                        // Action Buttons
                        Button(
                            onClick = { submitParsedBooking() },
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("voice_confirm_booking_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = StudioBlack),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    color = StudioBlack,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == AppLanguage.HINDI) "कन्फर्म करें और बुक करें" else "Confirm & Book Session",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                onApplyToFullForm(
                                    VoiceBookingPrefill(
                                        serviceType = editableService,
                                        preferredDate = editableDate,
                                        preferredTime = editableTime,
                                        location = editableLocation,
                                        notes = editableNotes,
                                        clientName = editableClientName,
                                        contactNumber = editablePhone
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("voice_apply_to_form_button"),
                            border = BorderStroke(1.dp, GoldPrimary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (language == AppLanguage.HINDI) "पूरे फॉर्म में संपादित करें" else "Edit in Full Booking Form",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceBookingModalDialog(
    language: AppLanguage,
    defaultClientName: String = "",
    defaultPhoneNumber: String = "",
    isEmailVerified: Boolean = true,
    isSubmitting: Boolean = false,
    onParseCommand: suspend (String) -> ParsedVoiceBookingCommand = { VoiceBookingCommandParser.parseLocally(it) },
    onSubmitBooking: (
        clientName: String,
        contactNumber: String,
        serviceType: String,
        preferredDate: String,
        preferredTime: String,
        location: String,
        notes: String,
        onSuccess: (bookingId: String) -> Unit
    ) -> Unit,
    onApplyToFullForm: (VoiceBookingPrefill) -> Unit,
    onViewBookings: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 20.dp)
                .heightIn(max = 760.dp)
                .testTag("voice_booking_modal_dialog"),
            color = StudioBlack,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.5.dp, GoldPrimary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.HINDI) "स्टार आर्टिस्ट वॉइस बुकिंग" else "STAR ARTIST VOICE BOOKING",
                        color = GoldPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.5.sp,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("voice_modal_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Voice Booking", tint = GoldLight)
                    }
                }

                VoiceBookingAssistantPanel(
                    language = language,
                    defaultClientName = defaultClientName,
                    defaultPhoneNumber = defaultPhoneNumber,
                    isEmailVerified = isEmailVerified,
                    isSubmitting = isSubmitting,
                    onParseCommand = onParseCommand,
                    onSubmitBooking = onSubmitBooking,
                    onApplyToFullForm = { prefill ->
                        onDismiss()
                        onApplyToFullForm(prefill)
                    },
                    onViewBookings = {
                        onDismiss()
                        onViewBookings()
                    }
                )
            }
        }
    }
}

@Composable
private fun VoiceSummaryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label:",
            color = TextSecondary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(68.dp)
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun VoiceBookingWaveform(
    soundLevelRms: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice_booking_wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val primaryBarColor = GoldPrimary
    val lightBarColor = GoldLight

    Canvas(modifier = modifier) {
        val barCount = 9
        val barWidth = 4.dp.toPx()
        val spacing = (size.width - (barCount * barWidth)) / (barCount - 1).coerceAtLeast(1)
        val maxHeight = size.height
        val normalizedRms = (soundLevelRms.coerceIn(-2f, 10f) + 2f) / 12f

        for (i in 0 until barCount) {
            val wave = kotlin.math.sin(phase + (i * 0.7f))
            val fraction = (0.22f + 0.36f * kotlin.math.abs(wave) + normalizedRms * 0.45f).coerceIn(0.2f, 1.0f)
            val barHeight = maxHeight * fraction
            val x = i * (barWidth + spacing)
            val y = (maxHeight - barHeight) / 2f
            drawRoundRect(
                color = if (i % 2 == 0) primaryBarColor else lightBarColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}

@Composable
private fun voiceChipColors(selected: Boolean) = FilterChipDefaults.filterChipColors(
    selectedContainerColor = GoldPrimary,
    selectedLabelColor = StudioBlack,
    selectedLeadingIconColor = StudioBlack,
    containerColor = StudioCardSurface,
    labelColor = TextSecondary,
    iconColor = GoldPrimary
)

@Composable
private fun voiceChipBorder(selected: Boolean) = FilterChipDefaults.filterChipBorder(
    enabled = true,
    selected = selected,
    borderColor = if (selected) GoldPrimary else StudioOutline
)

@Composable
private fun voiceFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GoldPrimary,
    unfocusedBorderColor = StudioOutline,
    focusedLabelColor = GoldPrimary,
    unfocusedLabelColor = TextSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = GoldPrimary,
    focusedContainerColor = StudioElevatedSurface,
    unfocusedContainerColor = StudioElevatedSurface
)
