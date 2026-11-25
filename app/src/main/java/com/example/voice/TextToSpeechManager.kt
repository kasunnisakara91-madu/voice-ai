package com.example.voice

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

data class VoiceOption(
    val name: String,
    val label: String,
    val languageTag: String,
    val isNetworkRequired: Boolean
)

class TextToSpeechManager(private val context: Context) {

    companion object {
        private const val TAG = "TextToSpeechMgr"
        private val SINHALA_LOCALE = Locale("si", "LK")
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<VoiceOption>>(emptyList())
    val availableVoices: StateFlow<List<VoiceOption>> = _availableVoices.asStateFlow()

    private val _isSinhalaSupported = MutableStateFlow(false)
    val isSinhalaSupported: StateFlow<Boolean> = _isSinhalaSupported.asStateFlow()

    private var currentSpeed = 1.0f
    private var currentPitch = 1.0f
    private var preferredVoiceName = ""

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isInitialized = true
                    refreshCapabilities()
                } else {
                    Log.e(TAG, "TextToSpeech initialization failed with status=$status")
                }
            }
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    Log.w(TAG, "TTS utterance error code=$errorCode")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error creating TextToSpeech instance", e)
        }
    }

    fun refreshCapabilities() {
        val engine = tts ?: return
        if (!isInitialized) return
        try {
            val sinhalaSupport = engine.isLanguageAvailable(SINHALA_LOCALE)
            _isSinhalaSupported.value = sinhalaSupport >= TextToSpeech.LANG_AVAILABLE

            val voices = engine.voices
                ?.filter { voice ->
                    val lang = voice.locale.language
                    lang == "si" || lang == "en"
                }
                ?.sortedWith(
                    compareByDescending<Voice> { it.locale.language == "si" }
                        .thenBy { it.locale.displayName }
                        .thenBy { it.name }
                )
                ?.map { voice ->
                    val langLabel = voice.locale.displayName
                    val qualityTag = if (voice.isNetworkConnectionRequired) "Network" else "Offline"
                    VoiceOption(
                        name = voice.name,
                        label = "$langLabel ($qualityTag)",
                        languageTag = voice.locale.toLanguageTag(),
                        isNetworkRequired = voice.isNetworkConnectionRequired
                    )
                }
                .orEmpty()

            _availableVoices.value = voices
        } catch (e: Exception) {
            Log.w(TAG, "Failed querying TTS voices", e)
        }
    }

    fun updateSettings(speed: Float, pitch: Float, voiceName: String) {
        currentSpeed = speed.coerceIn(0.5f, 2.0f)
        currentPitch = pitch.coerceIn(0.5f, 2.0f)
        preferredVoiceName = voiceName
    }

    fun speak(text: String, preferredLanguageCode: String? = null) {
        val engine = tts ?: return
        if (!isInitialized) {
            Log.w(TAG, "TTS not yet initialized; skipping speak.")
            return
        }

        val cleanText = sanitizeForSpeech(text)
        if (cleanText.isBlank()) return

        try {
            val containsSinhala = cleanText.any { it in '\u0D80'..'\u0DFF' }
            val targetLocale = when {
                containsSinhala || preferredLanguageCode == "si" -> SINHALA_LOCALE
                else -> Locale.US
            }

            // Apply custom voice if chosen and compatible, otherwise pick matching locale
            val matchingCustomVoice = if (preferredVoiceName.isNotBlank()) {
                engine.voices?.firstOrNull { it.name == preferredVoiceName }
            } else {
                null
            }

            if (matchingCustomVoice != null &&
                (!containsSinhala || matchingCustomVoice.locale.language == "si")
            ) {
                engine.voice = matchingCustomVoice
            } else {
                val langStatus = engine.isLanguageAvailable(targetLocale)
                if (langStatus >= TextToSpeech.LANG_AVAILABLE) {
                    engine.language = targetLocale
                } else if (containsSinhala) {
                    // Fallback to English voice if device lacks Sinhala TTS pack
                    engine.language = Locale.US
                } else {
                    engine.language = Locale.getDefault()
                }
            }

            engine.setSpeechRate(currentSpeed)
            engine.setPitch(currentPitch)

            val utteranceId = UUID.randomUUID().toString()
            _isSpeaking.value = true
            engine.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } catch (e: Exception) {
            _isSpeaking.value = false
            Log.e(TAG, "Failed to speak text", e)
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Failed stopping TTS", e)
        } finally {
            _isSpeaking.value = false
        }
    }

    fun createInstallTtsDataIntent(): Intent =
        Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun destroy() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.w(TAG, "Failed shutting down TTS", e)
        }
        tts = null
        isInitialized = false
        _isSpeaking.value = false
    }

    private fun sanitizeForSpeech(raw: String): String {
        return raw
            .replace(Regex("[*_`#~]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
