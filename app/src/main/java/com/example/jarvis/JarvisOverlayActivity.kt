package com.example.jarvis

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.MainActivity
import com.example.R
import com.example.configureAppCheck
import com.example.ui.components.AiOrb
import com.example.ui.components.VoiceStatusStrip
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.DeepVoidPurple
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.AssistantVoiceTheme
import com.example.viewmodel.AssistantStatus
import com.example.viewmodel.AssistantUiState
import com.example.viewmodel.AssistantViewModel

/**
 * Translucent floating Jarvis sheet that appears directly over any active app
 * when triggered via Default Assistant gesture, Quick Settings tile, Widget, or Notification.
 */
class JarvisOverlayActivity : ComponentActivity() {

    companion object {
        const val EXTRA_AUTO_LISTEN = "extra_auto_listen"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure Firebase App Check is configured with launch intent before any AI calls.
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        val autoListenRequested = intent?.getBooleanExtra(EXTRA_AUTO_LISTEN, true) ?: true

        setContent {
            val viewModel: AssistantViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            AssistantVoiceTheme(darkTheme = uiState.settings.darkTheme) {
                JarvisOverlaySheet(
                    uiState = uiState,
                    autoListenRequested = autoListenRequested,
                    onStartListening = viewModel::startVoiceListening,
                    onStopListening = viewModel::stopVoiceListening,
                    onStopSpeaking = viewModel::stopSpeaking,
                    onMicPermissionResult = viewModel::onMicPermissionResult,
                    onSendMessage = { text -> viewModel.sendMessage(text, isVoice = false) },
                    onToggleLanguage = viewModel::toggleSpeechLocaleQuick,
                    onOpenFullApp = {
                        startActivity(
                            Intent(this, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                        )
                        finish()
                    },
                    onDismiss = {
                        viewModel.stopVoiceListening()
                        finish()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureAppCheck(this, intent)
    }
}

@Composable
private fun JarvisOverlaySheet(
    uiState: AssistantUiState,
    autoListenRequested: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onStopSpeaking: () -> Unit,
    onMicPermissionResult: (Boolean) -> Unit,
    onSendMessage: (String) -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenFullApp: () -> Unit,
    onDismiss: () -> Unit
) {
    var inputText by rememberSaveable { mutableStateOf("") }
    var autoListenTriggered by rememberSaveable { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onMicPermissionResult
    )

    LaunchedEffect(autoListenRequested, uiState.hasMicrophonePermission, uiState.settings.jarvisAutoListen) {
        if (!autoListenTriggered && autoListenRequested && uiState.settings.jarvisAutoListen) {
            autoListenTriggered = true
            if (uiState.hasMicrophonePermission) {
                onStartListening()
            } else {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val handleOrbTap = {
        when {
            uiState.isListening -> onStopListening()
            uiState.isSpeaking -> onStopSpeaking()
            uiState.hasMicrophonePermission -> onStartListening()
            else -> micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val latestReply = uiState.messages.lastOrNull { !it.isUser }

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DeepVoidPurple, ObsidianBlack)
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(listOf(NeonCyan, ElectricPurple)),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Drag Handle & Header Row
            Box(
                modifier = Modifier
                    .size(width = 42.dp, height = 4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(NeonCyan.copy(alpha = 0.45f))
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.jarvis_overlay_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = NeonCyan
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MidnightCard,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(50))
                            .clickable(onClick = onToggleLanguage)
                    ) {
                        val isSi = uiState.settings.speechInputLocale.startsWith("si")
                        Text(
                            text = if (isSi) "සිං | SI" else "EN | US",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onOpenFullApp,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = "Open full Assistant Voice app",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close Jarvis overlay",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            AiOrb(
                status = uiState.status,
                rmsLevel = uiState.rmsLevel,
                onClick = handleOrbTap,
                size = 128.dp
            )

            VoiceStatusStrip(
                status = uiState.status,
                rmsLevel = uiState.rmsLevel
            )

            // Live partial transcript
            AnimatedVisibility(visible = uiState.partialTranscript.isNotBlank()) {
                Text(
                    text = "\"${uiState.partialTranscript}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeonCyan,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            // Latest response card
            if (latestReply != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MidnightCard.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (latestReply.isError) CrimsonGlow.copy(alpha = 0.5f) else NeonCyan.copy(alpha = 0.35f),
                            RoundedCornerShape(20.dp)
                        )
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        latestReply.actionBadge?.let { badge ->
                            val badgeColor = if (badge.succeeded) EmeraldGlow else AmberGlow
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (badge.succeeded) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = badge.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeColor
                                )
                            }
                        }
                        Text(
                            text = latestReply.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else if (uiState.errorBanner != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = uiState.errorBanner,
                    style = MaterialTheme.typography.bodySmall,
                    color = CrimsonGlow
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Instant hardware shortcut chips inside overlay
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AssistChip(
                    onClick = {
                        onSendMessage(
                            if (uiState.telemetry.isFlashlightOn) "Flashlight off" else "Flashlight on"
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.FlashlightOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(
                            if (uiState.telemetry.isFlashlightOn) "Torch Off" else "Torch On",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(containerColor = MidnightCard)
                )
                AssistChip(
                    onClick = { onSendMessage("Battery status") },
                    label = { Text("Battery", style = MaterialTheme.typography.labelSmall) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = MidnightCard)
                )
                AssistChip(
                    onClick = { onSendMessage("Go home") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Home,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Home", style = MaterialTheme.typography.labelSmall) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = MidnightCard)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Text Command Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.input_hint_si),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    }),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = ElectricPurple.copy(alpha = 0.5f),
                        focusedContainerColor = MidnightCard,
                        unfocusedContainerColor = MidnightCard
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("jarvis_overlay_input")
                )

                val canSend = inputText.isNotBlank() && uiState.status != AssistantStatus.THINKING
                IconButton(
                    onClick = {
                        if (canSend) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = canSend,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (canSend) {
                                Brush.linearGradient(listOf(ElectricPurple, NeonCyan))
                            } else {
                                Brush.linearGradient(listOf(MidnightCard, MidnightCard))
                            }
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = stringResource(R.string.cd_send_button),
                        tint = Color.White
                    )
                }
            }
        }
    }
}
