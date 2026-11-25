package com.example.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.actions.ActionResult
import com.example.actions.DeviceTelemetry
import com.example.actions.MediaControlAction
import com.example.actions.PhoneActionManager
import com.example.actions.VolumeAction
import com.example.ai.AssistantIntent
import com.example.ai.ConversationTurn
import com.example.ai.GeminiRepository
import com.example.ai.GeminiResult
import com.example.data.ActionBadge
import com.example.data.AppSettingsRepository
import com.example.data.AppSettingsState
import com.example.data.AssistantLanguage
import com.example.data.ChatMessage
import com.example.data.ConversationSessionStore
import com.example.data.GeminiModelOption
import com.example.voice.TextToSpeechManager
import com.example.voice.VoiceOption
import com.example.voice.VoiceRecognitionEvent
import com.example.voice.VoiceRecognitionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AssistantStatus {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR
}

sealed interface ConnectionTestState {
    data object Idle : ConnectionTestState
    data object Testing : ConnectionTestState
    data class Success(val message: String) : ConnectionTestState
    data class Error(val message: String) : ConnectionTestState
}

data class AssistantUiState(
    val status: AssistantStatus = AssistantStatus.IDLE,
    val messages: List<ChatMessage> = emptyList(),
    val partialTranscript: String = "",
    val errorBanner: String? = null,
    val rmsLevel: Float = 0f,
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val settings: AppSettingsState = AppSettingsState(),
    val telemetry: DeviceTelemetry = DeviceTelemetry(
        batteryPercent = -1,
        isCharging = false,
        isOnline = false,
        networkType = "Checking",
        hasFlashlight = false,
        isFlashlightOn = false
    ),
    val availableVoices: List<VoiceOption> = emptyList(),
    val isSinhalaTtsSupported: Boolean = false,
    val isSpeechRecognitionAvailable: Boolean = true,
    val hasMicrophonePermission: Boolean = false,
    val connectionTestState: ConnectionTestState = ConnectionTestState.Idle
)

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = AppSettingsRepository(application)
    private val voiceRecognitionManager = VoiceRecognitionManager(application)
    private val textToSpeechManager = TextToSpeechManager(application)
    private val phoneActionManager = PhoneActionManager(application)
    private val geminiRepository = GeminiRepository()

    private val _messages = ConversationSessionStore.messages
    private val _partialTranscript = MutableStateFlow("")
    private val _errorBanner = MutableStateFlow<String?>(null)
    private val _isThinking = MutableStateFlow(false)
    private val _telemetry = MutableStateFlow(phoneActionManager.readTelemetry())
    private val _hasMicPermission = MutableStateFlow(voiceRecognitionManager.hasMicrophonePermission())
    private val _connectionTestState = MutableStateFlow<ConnectionTestState>(ConnectionTestState.Idle)

    val settingsFlow: StateFlow<AppSettingsState> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettingsState())

    private val voiceStateFlow = combine(
        voiceRecognitionManager.isListening,
        voiceRecognitionManager.rmsLevel,
        textToSpeechManager.isSpeaking,
        textToSpeechManager.availableVoices,
        textToSpeechManager.isSinhalaSupported
    ) { isListening, rms, isSpeaking, voices, sinhalaTts ->
        VoiceSnapshot(isListening, rms, isSpeaking, voices, sinhalaTts)
    }

    private val conversationStateFlow = combine(
        _messages,
        _partialTranscript,
        _errorBanner,
        _isThinking,
        _telemetry
    ) { messages, partial, error, thinking, telemetry ->
        ConversationSnapshot(messages, partial, error, thinking, telemetry)
    }

    val uiState: StateFlow<AssistantUiState> = combine(
        settingsFlow,
        voiceStateFlow,
        conversationStateFlow,
        _hasMicPermission,
        _connectionTestState
    ) { settings, voice, conv, hasMic, connTest ->
        val status = when {
            voice.isListening -> AssistantStatus.LISTENING
            conv.isThinking -> AssistantStatus.THINKING
            voice.isSpeaking -> AssistantStatus.SPEAKING
            conv.errorBanner != null -> AssistantStatus.ERROR
            else -> AssistantStatus.IDLE
        }

        AssistantUiState(
            status = status,
            messages = conv.messages,
            partialTranscript = conv.partialTranscript,
            errorBanner = conv.errorBanner,
            rmsLevel = voice.rmsLevel,
            isListening = voice.isListening,
            isSpeaking = voice.isSpeaking,
            settings = settings,
            telemetry = conv.telemetry,
            availableVoices = voice.availableVoices,
            isSinhalaTtsSupported = voice.isSinhalaTtsSupported,
            isSpeechRecognitionAvailable = voiceRecognitionManager.isSpeechRecognitionAvailable(),
            hasMicrophonePermission = hasMic,
            connectionTestState = connTest
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = AssistantUiState(
            telemetry = phoneActionManager.readTelemetry(),
            isSpeechRecognitionAvailable = voiceRecognitionManager.isSpeechRecognitionAvailable(),
            hasMicrophonePermission = voiceRecognitionManager.hasMicrophonePermission()
        )
    )

    init {
        observeSettingsForTts()
        observeVoiceRecognitionEvents()
        observeFlashlightState()
    }

    private fun observeSettingsForTts() {
        viewModelScope.launch {
            settingsFlow.collect { settings ->
                textToSpeechManager.updateSettings(
                    speed = settings.speechSpeed,
                    pitch = settings.speechPitch,
                    voiceName = settings.selectedVoiceName
                )
            }
        }
    }

    private fun observeFlashlightState() {
        viewModelScope.launch {
            phoneActionManager.isFlashlightOn.collect {
                _telemetry.value = phoneActionManager.readTelemetry()
            }
        }
    }

    private fun observeVoiceRecognitionEvents() {
        viewModelScope.launch {
            voiceRecognitionManager.events.collect { event ->
                when (event) {
                    VoiceRecognitionEvent.Idle -> Unit
                    VoiceRecognitionEvent.ReadyForSpeech -> {
                        _errorBanner.value = null
                        _partialTranscript.value = ""
                    }
                    is VoiceRecognitionEvent.PartialResult -> {
                        _partialTranscript.value = event.text
                    }
                    is VoiceRecognitionEvent.FinalResult -> {
                        _partialTranscript.value = ""
                        voiceRecognitionManager.clearEvent()
                        sendMessage(event.text, isVoice = true)
                    }
                    is VoiceRecognitionEvent.Error -> {
                        _partialTranscript.value = ""
                        _errorBanner.value = event.message
                        voiceRecognitionManager.clearEvent()
                    }
                }
            }
        }
    }

    fun refreshDiagnostics() {
        _hasMicPermission.value = voiceRecognitionManager.hasMicrophonePermission()
        _telemetry.value = phoneActionManager.readTelemetry()
        textToSpeechManager.refreshCapabilities()
    }

    fun onMicPermissionResult(granted: Boolean) {
        _hasMicPermission.value = granted
        if (granted) {
            _errorBanner.value = null
            startVoiceListening()
        } else {
            _errorBanner.value =
                "Microphone permission denied. Grant permission in Settings or type your request below."
        }
    }

    fun startVoiceListening() {
        if (textToSpeechManager.isSpeaking.value) {
            textToSpeechManager.stop()
        }
        _errorBanner.value = null
        val locale = settingsFlow.value.speechInputLocale
        voiceRecognitionManager.startListening(locale)
    }

    fun stopVoiceListening() {
        voiceRecognitionManager.stopListening()
    }

    fun stopSpeaking() {
        textToSpeechManager.stop()
    }

    fun speakMessage(text: String, languageCode: String) {
        textToSpeechManager.speak(text, languageCode)
    }

    fun dismissError() {
        _errorBanner.value = null
    }

    fun toggleSpeechLocaleQuick() {
        viewModelScope.launch {
            val current = settingsFlow.value.speechInputLocale
            val next = if (current.startsWith("si")) "en-US" else "si-LK"
            settingsRepository.setSpeechInputLocale(next)
        }
    }

    fun sendMessage(rawText: String, isVoice: Boolean = false) {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty() || _isThinking.value) return

        if (textToSpeechManager.isSpeaking.value) {
            textToSpeechManager.stop()
        }
        _errorBanner.value = null
        _telemetry.value = phoneActionManager.readTelemetry()

        val currentSettings = settingsFlow.value
        val userMsg = ChatMessage(
            isUser = true,
            text = trimmed,
            isVoiceInput = isVoice,
            languageCode = if (trimmed.any { it in '\u0D80'..'\u0DFF' }) "si" else "en"
        )

        if (currentSettings.saveHistory) {
            ConversationSessionStore.append(userMsg)
        } else {
            ConversationSessionStore.replaceWith(listOf(userMsg))
        }

        viewModelScope.launch {
            _isThinking.value = true
            try {
                val historyTurns = if (currentSettings.saveHistory) {
                    _messages.value.dropLast(1).map {
                        ConversationTurn(isUser = it.isUser, text = it.text)
                    }
                } else {
                    emptyList()
                }

                val telemetrySnapshot = if (currentSettings.includeDeviceContext) {
                    _telemetry.value
                } else {
                    null
                }

                when (
                    val result = geminiRepository.processAssistantTurn(
                        userMessage = trimmed,
                        history = historyTurns,
                        preferredLanguage = currentSettings.preferredLanguage,
                        modelOption = currentSettings.selectedModel,
                        telemetry = telemetrySnapshot
                    )
                ) {
                    is GeminiResult.Success -> {
                        val decision = result.data
                        val actionResult = executePhoneAction(
                            intent = decision.intent,
                            target = decision.target,
                            secondaryTarget = decision.secondaryTarget
                        )

                        val finalReply = buildFinalAssistantReply(
                            geminiReply = decision.reply,
                            replyLanguage = decision.replyLanguage,
                            actionResult = actionResult
                        )

                        val assistantMsg = ChatMessage(
                            isUser = false,
                            text = finalReply,
                            languageCode = decision.replyLanguage,
                            actionBadge = actionResult?.let {
                                ActionBadge(title = it.badgeTitle, succeeded = it.success)
                            }
                        )

                        if (currentSettings.saveHistory) {
                            ConversationSessionStore.append(assistantMsg)
                        } else {
                            ConversationSessionStore.replaceWith(listOf(userMsg, assistantMsg))
                        }

                        _telemetry.value = phoneActionManager.readTelemetry()

                        if (currentSettings.autoSpeak) {
                            textToSpeechManager.speak(finalReply, decision.replyLanguage)
                        }
                    }

                    is GeminiResult.Error -> {
                        _errorBanner.value = result.message
                        val errorMsg = ChatMessage(
                            isUser = false,
                            text = result.message,
                            isError = true,
                            languageCode = "en"
                        )
                        if (currentSettings.saveHistory) {
                            ConversationSessionStore.append(errorMsg)
                        } else {
                            ConversationSessionStore.replaceWith(listOf(userMsg, errorMsg))
                        }
                    }
                }
            } finally {
                _isThinking.value = false
            }
        }
    }

    private suspend fun executePhoneAction(
        intent: AssistantIntent,
        target: String,
        secondaryTarget: String
    ): ActionResult? {
        return when (intent) {
            AssistantIntent.CHAT -> null
            AssistantIntent.OPEN_APP -> phoneActionManager.openApp(target)
            AssistantIntent.OPEN_SETTINGS -> phoneActionManager.openSettings(target.ifBlank { null })
            AssistantIntent.OPEN_CAMERA -> phoneActionManager.openCamera()
            AssistantIntent.FLASHLIGHT_ON -> phoneActionManager.setFlashlight(true)
            AssistantIntent.FLASHLIGHT_OFF -> phoneActionManager.setFlashlight(false)
            AssistantIntent.GET_BATTERY -> phoneActionManager.getBatteryStatus()
            AssistantIntent.GET_TIME -> phoneActionManager.getCurrentTime()
            AssistantIntent.GET_DATE -> phoneActionManager.getCurrentDate()
            AssistantIntent.VOLUME_UP -> phoneActionManager.adjustMediaVolume(VolumeAction.UP)
            AssistantIntent.VOLUME_DOWN -> phoneActionManager.adjustMediaVolume(VolumeAction.DOWN)
            AssistantIntent.VOLUME_MUTE -> phoneActionManager.adjustMediaVolume(VolumeAction.MUTE)
            AssistantIntent.VOLUME_UNMUTE -> phoneActionManager.adjustMediaVolume(VolumeAction.UNMUTE)
            AssistantIntent.WEB_SEARCH -> phoneActionManager.openBrowserSearch(target)
            AssistantIntent.OPEN_DIALER -> phoneActionManager.openDialer(target)
            AssistantIntent.PREPARE_SMS -> phoneActionManager.prepareSms(
                recipient = secondaryTarget.ifBlank { null },
                messageBody = target.ifBlank { null }
            )
            AssistantIntent.CHECK_INTERNET -> phoneActionManager.getNetworkStatus()
            AssistantIntent.SWITCH_LANGUAGE -> {
                val newLang = if (target.lowercase().startsWith("si")) {
                    AssistantLanguage.SINHALA
                } else {
                    AssistantLanguage.ENGLISH
                }
                settingsRepository.setPreferredLanguage(newLang)
                ActionResult(
                    success = true,
                    badgeTitle = "Language: ${newLang.displayName}",
                    messageEn = "Switched assistant language to English.",
                    messageSi = "සහකරුගේ භාෂාව සිංහලට මාරු කරන ලදී."
                )
            }
            AssistantIntent.GO_HOME -> phoneActionManager.goHome()
            AssistantIntent.MEDIA_PLAY_PAUSE -> phoneActionManager.controlMedia(MediaControlAction.PLAY_PAUSE)
            AssistantIntent.MEDIA_NEXT -> phoneActionManager.controlMedia(MediaControlAction.NEXT)
            AssistantIntent.MEDIA_PREVIOUS -> phoneActionManager.controlMedia(MediaControlAction.PREVIOUS)
            AssistantIntent.SET_TIMER -> phoneActionManager.setTimer(target)
        }
    }

    private fun buildFinalAssistantReply(
        geminiReply: String,
        replyLanguage: String,
        actionResult: ActionResult?
    ): String {
        if (actionResult == null) return geminiReply

        val verifiedMessage = if (replyLanguage == "si") {
            actionResult.messageSi
        } else {
            actionResult.messageEn
        }

        // If the real device action failed, always prioritize the honest failure report.
        return if (!actionResult.success) {
            verifiedMessage
        } else {
            verifiedMessage
        }
    }

    // Settings updates
    fun setSelectedModel(model: GeminiModelOption) {
        viewModelScope.launch { settingsRepository.setSelectedModel(model) }
    }

    fun setPreferredLanguage(language: AssistantLanguage) {
        viewModelScope.launch { settingsRepository.setPreferredLanguage(language) }
    }

    fun setSpeechInputLocale(locale: String) {
        viewModelScope.launch { settingsRepository.setSpeechInputLocale(locale) }
    }

    fun setSelectedVoiceName(voiceName: String) {
        viewModelScope.launch { settingsRepository.setSelectedVoiceName(voiceName) }
    }

    fun setSpeechSpeed(speed: Float) {
        viewModelScope.launch { settingsRepository.setSpeechSpeed(speed) }
    }

    fun setSpeechPitch(pitch: Float) {
        viewModelScope.launch { settingsRepository.setSpeechPitch(pitch) }
    }

    fun setAutoSpeak(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoSpeak(enabled) }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDarkTheme(enabled) }
    }

    fun setSaveHistory(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSaveHistory(enabled) }
    }

    fun setIncludeDeviceContext(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setIncludeDeviceContext(enabled) }
    }

    fun setJarvisStandbyEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setJarvisStandbyEnabled(enabled) }
    }

    fun setJarvisAutoListen(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setJarvisAutoListen(enabled) }
    }

    fun clearConversationHistory() {
        ConversationSessionStore.clear()
        _errorBanner.value = null
        textToSpeechManager.stop()
    }

    fun testGeminiConnection() {
        viewModelScope.launch {
            _connectionTestState.value = ConnectionTestState.Testing
            when (val result = geminiRepository.testConnection(settingsFlow.value.selectedModel)) {
                is GeminiResult.Success -> {
                    _connectionTestState.value = ConnectionTestState.Success(result.data)
                }
                is GeminiResult.Error -> {
                    _connectionTestState.value = ConnectionTestState.Error(result.message)
                }
            }
        }
    }

    fun createInstallTtsDataIntent(): Intent = textToSpeechManager.createInstallTtsDataIntent()

    override fun onCleared() {
        super.onCleared()
        voiceRecognitionManager.destroy()
        textToSpeechManager.destroy()
        phoneActionManager.cleanup()
    }

    private data class VoiceSnapshot(
        val isListening: Boolean,
        val rmsLevel: Float,
        val isSpeaking: Boolean,
        val availableVoices: List<VoiceOption>,
        val isSinhalaTtsSupported: Boolean
    )

    private data class ConversationSnapshot(
        val messages: List<ChatMessage>,
        val partialTranscript: String,
        val errorBanner: String?,
        val isThinking: Boolean,
        val telemetry: DeviceTelemetry
    )
}
