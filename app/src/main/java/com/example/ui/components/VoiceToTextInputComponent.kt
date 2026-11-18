package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.AppLanguage
import com.example.ui.theme.DarkStudioBlack
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.util.StudioStrings
import java.util.Locale

/**
 * State of the voice-to-text recognition session.
 */
enum class VoiceRecognitionState {
    IDLE,
    LISTENING,
    ERROR
}

/**
 * Voice-to-Text input component for the Star Artist Studio booking form.
 *
 * Allows users to conversationally describe photoshoot requirements, themes,
 * costume changes, and cinematography instructions by speaking into their microphone.
 * Handles microphone permissions, live audio visualizer feedback, and error recovery.
 */
@Composable
fun VoiceToTextInputComponent(
    currentText: String,
    onTextAppended: (String) -> Unit,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val s = StudioStrings(language)

    var recognitionState by remember { mutableStateOf(VoiceRecognitionState.IDLE) }
    var partialTranscription by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var soundLevelRms by remember { mutableFloatStateOf(0f) }

    val currentTextState by rememberUpdatedState(currentText)
    val onTextAppendedState by rememberUpdatedState(onTextAppended)

    // SpeechRecognizer holder
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    // Pulsing audio animation
    val infiniteTransition = rememberInfiniteTransition(label = "voice_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Clean up SpeechRecognizer on disposal
    DisposableEffect(Unit) {
        onDispose {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
        }
    }

    // Helper to launch speech recognizer
    fun startListeningInternal() {
        errorMessage = null
        partialTranscription = ""

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            recognitionState = VoiceRecognitionState.ERROR
            errorMessage = s.voiceNotSupported
            Toast.makeText(context, s.voiceNotSupported, Toast.LENGTH_SHORT).show()
            return
        }

        try {
            speechRecognizer?.destroy()
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    recognitionState = VoiceRecognitionState.LISTENING
                }

                override fun onBeginningOfSpeech() {
                    recognitionState = VoiceRecognitionState.LISTENING
                }

                override fun onRmsChanged(rmsdB: Float) {
                    soundLevelRms = rmsdB
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    // Transitioning to result processing
                }

                override fun onError(error: Int) {
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> if (language == AppLanguage.HINDI) "कोई आवाज़ सुनाई नहीं दी" else "No speech detected"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> if (language == AppLanguage.HINDI) "समय समाप्त हुआ" else "Speech timed out"
                        SpeechRecognizer.ERROR_AUDIO -> if (language == AppLanguage.HINDI) "ऑडियो रिकॉर्डिंग त्रुटि" else "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> if (language == AppLanguage.HINDI) "क्लाइंट त्रुटि" else "Client error"
                        SpeechRecognizer.ERROR_NETWORK -> if (language == AppLanguage.HINDI) "नेटवर्क त्रुटि" else "Network error"
                        else -> if (language == AppLanguage.HINDI) "वाक् पहचान में त्रुटि हुई" else "Speech recognition error"
                    }
                    errorMessage = msg
                    recognitionState = VoiceRecognitionState.ERROR
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val transcribed = matches?.firstOrNull()?.trim()
                    if (!transcribed.isNullOrBlank()) {
                        val updated = if (currentTextState.isBlank()) {
                            transcribed
                        } else {
                            "${currentTextState.trim()} $transcribed"
                        }
                        onTextAppendedState(updated)
                    }
                    recognitionState = VoiceRecognitionState.IDLE
                    partialTranscription = ""
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = matches?.firstOrNull()
                    if (!partial.isNullOrBlank()) {
                        partialTranscription = partial
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (language == AppLanguage.HINDI) "hi-IN" else "en-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            recognizer.startListening(intent)
            recognitionState = VoiceRecognitionState.LISTENING
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Failed to start speech recognition"
            recognitionState = VoiceRecognitionState.ERROR
        }
    }

    // Microphone permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showPermissionRationale = false
            startListeningInternal()
        } else {
            showPermissionRationale = true
            recognitionState = VoiceRecognitionState.IDLE
        }
    }

    fun handleMicClick() {
        if (recognitionState == VoiceRecognitionState.LISTENING) {
            // Stop listening
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {}
            recognitionState = VoiceRecognitionState.IDLE
        } else {
            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                showPermissionRationale = false
                startListeningInternal()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                BorderStroke(
                    1.dp,
                    if (recognitionState == VoiceRecognitionState.LISTENING) GoldPrimary else StudioOutline
                ),
                RoundedCornerShape(14.dp)
            )
            .testTag("voice_to_text_component"),
        colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Row: Title & Interactive Mic Action
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
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (recognitionState == VoiceRecognitionState.LISTENING) GoldPrimary else GoldContainer)
                            .border(1.dp, GoldPrimary.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (recognitionState == VoiceRecognitionState.LISTENING)
                                Icons.Default.RecordVoiceOver
                            else
                                Icons.Default.Mic,
                            contentDescription = "Voice to Text",
                            tint = if (recognitionState == VoiceRecognitionState.LISTENING) StudioBlack else GoldPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = s.voiceToTextTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldLight,
                            fontSize = 12.5.sp
                        )
                        Text(
                            text = if (recognitionState == VoiceRecognitionState.LISTENING)
                                s.voiceListening
                            else
                                s.voiceToTextPrompt,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (recognitionState == VoiceRecognitionState.LISTENING) GoldPrimary else Color(0xFFAAAAAA),
                            fontSize = 9.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Primary 48dp Mic Control Button
                Surface(
                    onClick = { handleMicClick() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("voice_to_text_mic_button"),
                    shape = CircleShape,
                    color = if (recognitionState == VoiceRecognitionState.LISTENING) Color(0xFFE53935) else GoldPrimary,
                    shadowElevation = if (recognitionState == VoiceRecognitionState.LISTENING) 8.dp else 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (recognitionState == VoiceRecognitionState.LISTENING) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = s.voiceTapToStop,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Start Voice Input",
                                tint = StudioBlack,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // =================================================================
            // LIVE LISTENING STATUS & AUDIO WAVE VISUALIZER
            // =================================================================
            androidx.compose.animation.AnimatedVisibility(
                visible = recognitionState == VoiceRecognitionState.LISTENING,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF191307))
                        .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                        .testTag("voice_listening_indicator")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Pulsing glowing dot
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = s.voiceListening,
                                color = GoldLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Audio Waveform Visualization
                        VoiceAudioWaveform(
                            soundLevelRms = soundLevelRms,
                            modifier = Modifier
                                .width(90.dp)
                                .height(22.dp)
                        )
                    }

                    if (partialTranscription.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "\"$partialTranscription\"",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }

            // =================================================================
            // PERMISSION RATIONALE CARD (Shown if user previously denied)
            // =================================================================
            androidx.compose.animation.AnimatedVisibility(
                visible = showPermissionRationale,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .testTag("voice_permission_rationale"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF261D0C)),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MicOff,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = s.voicePermissionNeeded,
                                    color = GoldLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                )
                            }
                            IconButton(
                                onClick = { showPermissionRationale = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = s.voicePermissionRationale,
                            color = Color(0xFFD4D4D4),
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("voice_request_permission_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = StudioBlack),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = s.voiceGrantPermissionBtn,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Error notice banner
            errorMessage?.let { error ->
                if (recognitionState == VoiceRecognitionState.ERROR) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "⚠ $error",
                        color = Color(0xFFFF8A80),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * 5-bar animated audio waveform canvas responding to live audio sound level RMS.
 */
@Composable
private fun VoiceAudioWaveform(
    soundLevelRms: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val barCount = 7
        val barWidth = 4.dp.toPx()
        val spacing = (size.width - (barCount * barWidth)) / (barCount - 1).coerceAtLeast(1)
        val maxHeight = size.height

        val normalizedRms = (soundLevelRms.coerceIn(-2f, 10f) + 2f) / 12f // 0f to 1f

        for (i in 0 until barCount) {
            val wave = kotlin.math.sin(phase + (i * 0.8f))
            val baseFraction = 0.25f + 0.35f * kotlin.math.abs(wave)
            val dynamicFraction = (baseFraction + (normalizedRms * 0.5f)).coerceIn(0.2f, 1.0f)
            val barHeight = maxHeight * dynamicFraction

            val x = i * (barWidth + spacing)
            val y = (maxHeight - barHeight) / 2f

            drawRoundRect(
                color = if (i % 2 == 0) Color(0xFFD4AF37) else Color(0xFFF6E79D),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}
