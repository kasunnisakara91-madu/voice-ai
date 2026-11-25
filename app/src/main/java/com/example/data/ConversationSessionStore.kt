package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class ActionBadge(
    val title: String,
    val succeeded: Boolean
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
    val isVoiceInput: Boolean = false,
    val languageCode: String = "en",
    val actionBadge: ActionBadge? = null,
    val isError: Boolean = false
)

/**
 * Shared session store so commands executed from Jarvis Overlay and MainActivity
 * stay synchronized in one conversation history.
 */
object ConversationSessionStore {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    fun append(message: ChatMessage) {
        _messages.update { it + message }
    }

    fun replaceWith(messages: List<ChatMessage>) {
        _messages.value = messages
    }

    fun clear() {
        _messages.value = emptyList()
    }
}
