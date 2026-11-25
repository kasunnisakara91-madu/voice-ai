package com.example.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface VoiceRecognitionEvent {
    data object Idle : VoiceRecognitionEvent
    data object ReadyForSpeech : VoiceRecognitionEvent
    data class PartialResult(val text: String) : VoiceRecognitionEvent
    data class FinalResult(val text: String) : VoiceRecognitionEvent
    data class Error(val message: String, val code: Int) : VoiceRecognitionEvent
}

class VoiceRecognitionManager(private val context: Context) {

    companion object {
        private const val TAG = "VoiceRecognitionMgr"
    }

    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _events = MutableStateFlow<VoiceRecognitionEvent>(VoiceRecognitionEvent.Idle)
    val events: StateFlow<VoiceRecognitionEvent> = _events.asStateFlow()

    fun isSpeechRecognitionAvailable(): Boolean =
        SpeechRecognizer.isRecognitionAvailable(context)

    fun hasMicrophonePermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    fun startListening(languageTag: String) {
        if (!hasMicrophonePermission()) {
            _isListening.value = false
            _events.value = VoiceRecognitionEvent.Error(
                message = "Microphone permission is required for voice commands.",
                code = SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
            )
            return
        }

        if (!isSpeechRecognitionAvailable()) {
            _isListening.value = false
            _events.value = VoiceRecognitionEvent.Error(
                message = "Speech recognition service is unavailable on this device. Please install or enable Google Speech Services, or type your message below.",
                code = -1
            )
            return
        }

        try {
            destroyRecognizer()
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                    _events.value = VoiceRecognitionEvent.ReadyForSpeech
                }

                override fun onBeginningOfSpeech() {
                    _isListening.value = true
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Normalize typical SpeechRecognizer rmsdB (-2f..10f) into 0f..1f
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    _rmsLevel.value = normalized
                }

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    _isListening.value = false
                    _rmsLevel.value = 0f
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    _rmsLevel.value = 0f
                    val message = mapSpeechError(error, languageTag)
                    Log.w(TAG, "SpeechRecognizer error=$error: $message")
                    _events.value = VoiceRecognitionEvent.Error(message = message, code = error)
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    _rmsLevel.value = 0f
                    val matches = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val recognized = matches?.firstOrNull()?.trim().orEmpty()
                    if (recognized.isNotEmpty()) {
                        _events.value = VoiceRecognitionEvent.FinalResult(recognized)
                    } else {
                        _events.value = VoiceRecognitionEvent.Error(
                            message = if (languageTag.startsWith("si")) {
                                "හඬ හඳුනාගැනීමට නොහැකි විය. කරුණාකර නැවත උත්සාහ කරන්න."
                            } else {
                                "Could not recognize speech. Please try again."
                            },
                            code = SpeechRecognizer.ERROR_NO_MATCH
                        )
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                        .orEmpty()
                    if (partial.isNotEmpty()) {
                        _events.value = VoiceRecognitionEvent.PartialResult(partial)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }

            _isListening.value = true
            _events.value = VoiceRecognitionEvent.ReadyForSpeech
            recognizer.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SpeechRecognizer", e)
            _isListening.value = false
            _rmsLevel.value = 0f
            _events.value = VoiceRecognitionEvent.Error(
                message = "Unable to start speech recognition: ${e.localizedMessage ?: "Unknown error"}",
                code = -2
            )
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping SpeechRecognizer", e)
        } finally {
            _isListening.value = false
            _rmsLevel.value = 0f
        }
    }

    fun cancelListening() {
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling SpeechRecognizer", e)
        } finally {
            _isListening.value = false
            _rmsLevel.value = 0f
            _events.value = VoiceRecognitionEvent.Idle
        }
    }

    fun clearEvent() {
        _events.value = VoiceRecognitionEvent.Idle
    }

    fun destroy() {
        destroyRecognizer()
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error destroying SpeechRecognizer", e)
        }
        speechRecognizer = null
    }

    private fun mapSpeechError(errorCode: Int, languageTag: String): String {
        val isSinhala = languageTag.startsWith("si")
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> if (isSinhala) {
                "මයික්‍රෆෝනයෙන් හඬ ලබාගැනීමේ දෝෂයකි."
            } else {
                "Audio recording error. Check microphone access."
            }
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                "Microphone permission is required."
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> if (isSinhala) {
                "හඬ හඳුනාගැනීමට අන්තර්ජාල සම්බන්ධතාවය අවශ්‍ය වේ."
            } else {
                "Network error during speech recognition. Check your internet connection."
            }
            SpeechRecognizer.ERROR_NO_MATCH -> if (isSinhala) {
                "ඔබ පැවසූ දේ පැහැදිලි නැත. නැවත කතා කරන්න."
            } else {
                "Didn't catch that. Please tap the mic and speak clearly."
            }
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                "Speech service is busy. Please try again in a moment."
            SpeechRecognizer.ERROR_SERVER ->
                "Speech server unavailable. Try again or type your command."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> if (isSinhala) {
                "හඬක් ඇසුණේ නැත. මයික්‍රෆෝනය තට්ටු කර නැවත කතා කරන්න."
            } else {
                "No speech detected. Tap the mic to try again."
            }
            SpeechRecognizer.ERROR_CLIENT ->
                "Speech recognition cancelled."
            else ->
                "Speech recognition unavailable (code $errorCode). You can type your message instead."
        }
    }
}
