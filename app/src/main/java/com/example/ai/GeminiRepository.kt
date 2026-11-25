package com.example.ai

import android.util.Log
import com.example.actions.DeviceTelemetry
import com.example.data.AssistantLanguage
import com.example.data.GeminiModelOption
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

enum class AssistantIntent {
    CHAT,
    OPEN_APP,
    OPEN_SETTINGS,
    OPEN_CAMERA,
    FLASHLIGHT_ON,
    FLASHLIGHT_OFF,
    GET_BATTERY,
    GET_TIME,
    GET_DATE,
    VOLUME_UP,
    VOLUME_DOWN,
    VOLUME_MUTE,
    VOLUME_UNMUTE,
    WEB_SEARCH,
    OPEN_DIALER,
    PREPARE_SMS,
    CHECK_INTERNET,
    SWITCH_LANGUAGE,
    GO_HOME,
    MEDIA_PLAY_PAUSE,
    MEDIA_NEXT,
    MEDIA_PREVIOUS,
    SET_TIMER;

    companion object {
        fun fromWire(value: String?): AssistantIntent =
            entries.firstOrNull { it.name == value } ?: CHAT
    }
}

data class ConversationTurn(
    val isUser: Boolean,
    val text: String
)

data class GeminiAssistantDecision(
    val intent: AssistantIntent,
    val target: String,
    val secondaryTarget: String,
    val reply: String,
    val replyLanguage: String
)

sealed interface GeminiResult<out T> {
    data class Success<T>(val data: T) : GeminiResult<T>
    data class Error(val message: String) : GeminiResult<Nothing>
}

class GeminiRepository {

    companion object {
        private const val TAG = "GeminiRepository"
        private const val REQUEST_TIMEOUT_MS = 60_000L
        private const val TEST_TIMEOUT_MS = 30_000L
    }

    private val assistantSchema = Schema.obj(
        properties = mapOf(
            "intent" to Schema.enumeration(
                values = AssistantIntent.entries.map { it.name },
                description = "The action intent detected from the user's Sinhala, Singlish, or English request. Use CHAT for general questions, translation, conversation, or knowledge."
            ),
            "target" to Schema.string(
                description = "Primary argument: app name for OPEN_APP (e.g. YouTube, WhatsApp, Facebook, Chrome), search query for WEB_SEARCH, phone number for OPEN_DIALER, message body for PREPARE_SMS, language code ('si' or 'en') for SWITCH_LANGUAGE, or settings section for OPEN_SETTINGS.",
                nullable = true
            ),
            "secondaryTarget" to Schema.string(
                description = "Optional secondary argument such as recipient phone number for PREPARE_SMS.",
                nullable = true
            ),
            "reply" to Schema.string(
                description = "Helpful natural response in Sinhala, English, or matching language preference. Keep voice responses concise and clear. When intent is a phone action (other than CHAT), say you are performing or checking it without claiming it already succeeded."
            ),
            "replyLanguage" to Schema.enumeration(
                values = listOf("si", "en"),
                description = "Language code of the reply for TextToSpeech ('si' for Sinhala script, 'en' for English)."
            )
        ),
        optionalProperties = listOf("target", "secondaryTarget")
    )

    private val structuredConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = assistantSchema
        temperature = 0.4f
    }

    // Lazily constructed per modelId so Firebase.ai is never evaluated at class construction time.
    private val structuredModels = mutableMapOf<String, GenerativeModel>()
    private val plainModels = mutableMapOf<String, GenerativeModel>()

    private fun getStructuredModel(modelOption: GeminiModelOption): GenerativeModel {
        return structuredModels.getOrPut(modelOption.modelId) {
            Firebase.ai.generativeModel(
                modelName = modelOption.modelId,
                generationConfig = structuredConfig,
                systemInstruction = content {
                    text(
                        """
                        You are ASSISTANT VOICE, a friendly, intelligent Android personal voice assistant created by MADUSANKA.
                        You understand natural Sinhala (සිංහල), Singlish (Sinhala written in Latin letters like "YouTube eka open karanna", "ada dawasa kiyada", "mage battery kiyada"), and English seamlessly.

                        INTENT RULES:
                        - "YouTube open කරන්න" / "Assistant, YouTube එක open කරන්න" / "YouTube eka danna" -> OPEN_APP with target="YouTube"
                        - "WhatsApp open කරන්න" / "whatsapp eka arinna" -> OPEN_APP with target="WhatsApp"
                        - "Facebook open කරන්න" / "Chrome open කරන්න" / "open <app>" -> OPEN_APP with target="<app>"
                        - "Settings open කරන්න" / "open wifi settings" -> OPEN_SETTINGS (target optional like "wifi")
                        - "Camera open කරන්න" / "take a photo" -> OPEN_CAMERA
                        - "Flashlight එක on කරන්න" / "torch on" / "light eka danna" -> FLASHLIGHT_ON
                        - "Flashlight එක off කරන්න" / "torch off" / "light eka niwanna" -> FLASHLIGHT_OFF
                        - "Battery එක කොච්චර තියෙනවද?" / "battery percentage" -> GET_BATTERY
                        - "දැන් වෙලාව කීයද?" / "what time is it" -> GET_TIME
                        - "මට අද දවස කියන්න" / "what is today's date" -> GET_DATE
                        - "volume up" / "saddaya wadi karanna" -> VOLUME_UP
                        - "volume down" / "saddaya adu karanna" -> VOLUME_DOWN
                        - "mute volume" / "silent karanna" -> VOLUME_MUTE
                        - "Google එකේ search කරන්න <topic>" / "search Google for <topic>" -> WEB_SEARCH with target="<topic>"
                        - "call <number>" / "dial <number>" / "open dialer" -> OPEN_DIALER with target="<number>"
                        - "send SMS to <number> saying <msg>" / "message <msg>" -> PREPARE_SMS with target="<msg>", secondaryTarget="<number>"
                        - "internet connection check කරන්න" / "am I online" -> CHECK_INTERNET
                        - "මට සිංහලෙන් උත්තර දෙන්න" / "speak in Sinhala" -> SWITCH_LANGUAGE with target="si"
                        - "speak in English" / "English walin kiyanna" -> SWITCH_LANGUAGE with target="en"
                        - "Home යන්න" / "go home" / "home screen" -> GO_HOME
                        - "play music" / "pause music" / "සින්දුව නවත්තන්න" / "song eka pause karanna" -> MEDIA_PLAY_PAUSE
                        - "next song" / "ඊළඟ සින්දුව" -> MEDIA_NEXT
                        - "previous song" / "පෙර සින්දුව" -> MEDIA_PREVIOUS
                        - "විනාඩි 5ක timer එකක් දාන්න" / "set timer for 30 seconds" -> SET_TIMER with target="<number and unit, e.g. 5 minutes>"
                        - General knowledge, Q&A, translation, explanations, conversation -> CHAT

                        LANGUAGE & ACCURACY RULES:
                        - Respect the user's language preference when specified. If AUTO, reply in Sinhala script when the user writes/speaks Sinhala or Singlish, and in English when the user uses English.
                        - Never claim a device action succeeded in advance; the app executes the real Android API and reports the verified outcome.
                        - Keep answers concise, warm, and natural for spoken Text-to-Speech output.
                        """.trimIndent()
                    )
                }
            )
        }
    }

    private fun getPlainModel(modelOption: GeminiModelOption): GenerativeModel {
        return plainModels.getOrPut(modelOption.modelId) {
            Firebase.ai.generativeModel(modelName = modelOption.modelId)
        }
    }

    suspend fun processAssistantTurn(
        userMessage: String,
        history: List<ConversationTurn>,
        preferredLanguage: AssistantLanguage,
        modelOption: GeminiModelOption,
        telemetry: DeviceTelemetry?
    ): GeminiResult<GeminiAssistantDecision> {
        return try {
            val model = getStructuredModel(modelOption)
            val prompt = buildContextPrompt(
                userMessage = userMessage,
                history = history,
                preferredLanguage = preferredLanguage,
                telemetry = telemetry
            )

            val response = withTimeout(REQUEST_TIMEOUT_MS) {
                model.generateContent(prompt)
            }

            val jsonString = response.text
                ?: return GeminiResult.Error("Gemini returned an empty response.")

            val json = JSONObject(jsonString)
            val intent = AssistantIntent.fromWire(json.optString("intent", "CHAT"))
            val target = if (json.isNull("target")) "" else json.optString("target", "").trim()
            val secondaryTarget = if (json.isNull("secondaryTarget")) {
                ""
            } else {
                json.optString("secondaryTarget", "").trim()
            }
            val reply = json.optString("reply", "").trim()
            val replyLanguage = json.optString("replyLanguage", "en").let {
                if (it == "si") "si" else "en"
            }

            if (reply.isBlank() && intent == AssistantIntent.CHAT) {
                return GeminiResult.Error("Gemini returned a blank answer. Please try again.")
            }

            GeminiResult.Success(
                GeminiAssistantDecision(
                    intent = intent,
                    target = target,
                    secondaryTarget = secondaryTarget,
                    reply = reply,
                    replyLanguage = replyLanguage
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini request timed out", e)
            GeminiResult.Error("Gemini timed out after 60 seconds. Please check your connection and try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini request failed", e)
            GeminiResult.Error(
                e.localizedMessage ?: "Failed to reach Gemini AI via Firebase AI Logic."
            )
        }
    }

    suspend fun testConnection(modelOption: GeminiModelOption): GeminiResult<String> {
        return try {
            val model = getPlainModel(modelOption)
            val response = withTimeout(TEST_TIMEOUT_MS) {
                model.generateContent("Reply with exactly: Connected to ${modelOption.modelId}")
            }
            val text = response.text?.trim()
            if (!text.isNullOrBlank()) {
                GeminiResult.Success(text)
            } else {
                GeminiResult.Error("Received empty response from ${modelOption.modelId}.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Connection test timed out", e)
            GeminiResult.Error("Connection test timed out after 30 seconds.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed", e)
            GeminiResult.Error(e.localizedMessage ?: "Connection test failed.")
        }
    }

    private fun buildContextPrompt(
        userMessage: String,
        history: List<ConversationTurn>,
        preferredLanguage: AssistantLanguage,
        telemetry: DeviceTelemetry?
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Preferred response language setting: ${preferredLanguage.name}")
        if (telemetry != null) {
            sb.appendLine(
                "Current device snapshot: online=${telemetry.isOnline} (${telemetry.networkType}), " +
                    "battery=${telemetry.batteryPercent}%, charging=${telemetry.isCharging}, " +
                    "hasFlashlight=${telemetry.hasFlashlight}, flashlightOn=${telemetry.isFlashlightOn}"
            )
        }
        val recentHistory = history.takeLast(8)
        if (recentHistory.isNotEmpty()) {
            sb.appendLine("Recent conversation context:")
            for (turn in recentHistory) {
                val role = if (turn.isUser) "User" else "Assistant"
                sb.appendLine("$role: ${turn.text}")
            }
        }
        sb.appendLine("Current user input:")
        sb.append(userMessage)
        return sb.toString()
    }
}
