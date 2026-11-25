package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AssistantLanguage
import com.example.ui.components.AiOrb
import com.example.ui.components.ChatBubble
import com.example.ui.components.JarvisHubSheet
import com.example.ui.components.VoiceStatusStrip
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.DeepViolet
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.NeonCyan
import com.example.viewmodel.AssistantStatus
import com.example.viewmodel.AssistantUiState

private val QUICK_SUGGESTIONS = listOf(
    "YouTube open කරන්න",
    "දැන් වෙලාව කීයද?",
    "Battery එක කොච්චර තියෙනවද?",
    "Flashlight එක on කරන්න",
    "අද දවස කියන්න",
    "Settings open කරන්න"
)

@Composable
fun AssistantScreen(
    uiState: AssistantUiState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onStopSpeaking: () -> Unit,
    onMicPermissionResult: (Boolean) -> Unit,
    onSendMessage: (String) -> Unit,
    onSpeakMessage: (String, String) -> Unit,
    onToggleVoiceLanguage: () -> Unit,
    onToggleJarvisStandby: (Boolean) -> Unit,
    onToggleJarvisAutoListen: (Boolean) -> Unit,
    onDismissError: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by rememberSaveable { mutableStateOf("") }
    var showJarvisHub by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onMicPermissionResult
    )

    val handleMicTap = {
        when {
            uiState.isListening -> onStopListening()
            uiState.isSpeaking -> onStopSpeaking()
            uiState.hasMicrophonePermission -> onStartListening()
            else -> micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.background
        )
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        val isCompactHeight = maxHeight < 640.dp
        val orbSize = when {
            uiState.messages.isEmpty() && !isCompactHeight -> 182.dp
            uiState.messages.isEmpty() -> 136.dp
            else -> 92.dp
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Brand & Telemetry Bar
            TopAssistantBar(
                uiState = uiState,
                onOpenJarvisHub = { showJarvisHub = true },
                onToggleVoiceLanguage = onToggleVoiceLanguage,
                onOpenSettings = onOpenSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Main Content Area
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .padding(horizontal = 16.dp)
            ) {
                AiOrb(
                    status = uiState.status,
                    rmsLevel = uiState.rmsLevel,
                    onClick = handleMicTap,
                    size = orbSize
                )

                Spacer(modifier = Modifier.height(6.dp))

                VoiceStatusStrip(
                    status = uiState.status,
                    rmsLevel = uiState.rmsLevel
                )

                // Live partial voice transcript banner
                AnimatedVisibility(
                    visible = uiState.partialTranscript.isNotBlank(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        color = NeonCyan.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .border(
                                1.dp,
                                NeonCyan.copy(alpha = 0.4f),
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        Text(
                            text = "\"${uiState.partialTranscript}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }

                // Error banner
                AnimatedVisibility(
                    visible = uiState.errorBanner != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        color = CrimsonGlow.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .border(
                                1.dp,
                                CrimsonGlow.copy(alpha = 0.5f),
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
                        ) {
                            Text(
                                text = uiState.errorBanner.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = onDismissError,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Dismiss error",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (uiState.messages.isEmpty()) {
                    // Empty / Hero Welcome State
                    Spacer(modifier = Modifier.height(14.dp))
                    val greeting = if (uiState.settings.preferredLanguage == AssistantLanguage.SINHALA ||
                        uiState.settings.speechInputLocale.startsWith("si")
                    ) {
                        stringResource(R.string.greeting_english) + "\n" + stringResource(R.string.greeting_sinhala)
                    } else {
                        stringResource(R.string.greeting_english)
                    }
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap the mic or type in Sinhala, Singlish, or English",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick command suggestions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp)
                    ) {
                        QUICK_SUGGESTIONS.forEach { suggestion ->
                            AssistChip(
                                onClick = { onSendMessage(suggestion) },
                                label = {
                                    Text(
                                        text = suggestion,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                border = AssistChipDefaults.assistChipBorder(
                                    enabled = true,
                                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("conversation_list")
                    ) {
                        itemsIndexed(
                            items = uiState.messages,
                            key = { _, msg -> msg.id }
                        ) { index, msg ->
                            val isLatestSpeaking = uiState.isSpeaking &&
                                !msg.isUser &&
                                index == uiState.messages.indexOfLast { !it.isUser && !it.isError }
                            ChatBubble(
                                message = msg,
                                isCurrentlySpeaking = isLatestSpeaking,
                                onSpeakClick = { onSpeakMessage(msg.text, msg.languageCode) },
                                onStopSpeakClick = onStopSpeaking
                            )
                        }
                    }
                }
            }

            // Bottom Input & Glowing Microphone Dock
            BottomCommandDock(
                inputText = inputText,
                onInputTextChange = { inputText = it },
                isSinhalaVoice = uiState.settings.speechInputLocale.startsWith("si"),
                status = uiState.status,
                onSendClick = {
                    if (inputText.isNotBlank()) {
                        onSendMessage(inputText)
                        inputText = ""
                    }
                },
                onMicClick = handleMicTap,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }

    if (showJarvisHub) {
        JarvisHubSheet(
            standbyEnabled = uiState.settings.jarvisStandbyEnabled,
            autoListenEnabled = uiState.settings.jarvisAutoListen,
            hasMicPermission = uiState.hasMicrophonePermission,
            onToggleStandby = onToggleJarvisStandby,
            onToggleAutoListen = onToggleJarvisAutoListen,
            onMicPermissionGranted = onMicPermissionResult,
            onDismiss = { showJarvisHub = false }
        )
    }
}

@Composable
private fun TopAssistantBar(
    uiState: AssistantUiState,
    onOpenJarvisHub: () -> Unit,
    onToggleVoiceLanguage: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.brand_header),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (uiState.telemetry.isOnline) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                    contentDescription = null,
                    tint = if (uiState.telemetry.isOnline) EmeraldGlow else CrimsonGlow,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = uiState.telemetry.networkType,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (uiState.telemetry.batteryPercent >= 0) {
                    Icon(
                        imageVector = if (uiState.telemetry.isCharging) {
                            Icons.Rounded.BatteryChargingFull
                        } else {
                            Icons.Rounded.BatteryStd
                        },
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${uiState.telemetry.batteryPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = if (uiState.settings.jarvisStandbyEnabled) {
                    EmeraldGlow.copy(alpha = 0.22f)
                } else {
                    ElectricPurple.copy(alpha = 0.25f)
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(50))
                    .border(
                        width = 1.dp,
                        color = if (uiState.settings.jarvisStandbyEnabled) EmeraldGlow else ElectricPurple,
                        shape = RoundedCornerShape(50)
                    )
                    .clickable(onClick = onOpenJarvisHub)
                    .testTag("jarvis_hub_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = "Jarvis system controls",
                        tint = if (uiState.settings.jarvisStandbyEnabled) EmeraldGlow else NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "JARVIS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            val isSinhalaInput = uiState.settings.speechInputLocale.startsWith("si")
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(50))
                    .border(
                        width = 1.dp,
                        color = NeonCyan.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(50)
                    )
                    .clickable(onClick = onToggleVoiceLanguage)
                    .testTag("language_toggle_pill")
            ) {
                Text(
                    text = if (isSinhalaInput) "සිං" else "EN",
                    style = MaterialTheme.typography.labelMedium,
                    color = NeonCyan,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.cd_settings_button),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun BottomCommandDock(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    isSinhalaVoice: Boolean,
    status: AssistantStatus,
    onSendClick: () -> Unit,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
    ) {
        // Large Glowing Microphone Button
        val micGradient = when (status) {
            AssistantStatus.LISTENING -> Brush.linearGradient(listOf(NeonCyan, ElectricPurple))
            AssistantStatus.SPEAKING -> Brush.linearGradient(listOf(EmeraldGlow, NeonCyan))
            else -> Brush.linearGradient(listOf(DeepViolet, ElectricPurple, NeonCyan))
        }

        val micIcon = when (status) {
            AssistantStatus.LISTENING -> Icons.Rounded.GraphicEq
            AssistantStatus.SPEAKING -> Icons.Rounded.Stop
            else -> Icons.Rounded.Mic
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(68.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = NeonCyan,
                    spotColor = ElectricPurple
                )
                .clip(CircleShape)
                .background(micGradient)
                .border(
                    width = 1.5.dp,
                    color = Color.White.copy(alpha = 0.35f),
                    shape = CircleShape
                )
                .clickable(onClick = onMicClick)
                .testTag("mic_button")
        ) {
            Icon(
                imageVector = micIcon,
                contentDescription = if (status == AssistantStatus.LISTENING) {
                    stringResource(R.string.cd_stop_mic_button)
                } else {
                    stringResource(R.string.cd_mic_button)
                },
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }

        // Text Message Input + Send Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChange,
                placeholder = {
                    Text(
                        text = if (isSinhalaVoice) {
                            stringResource(R.string.input_hint_si)
                        } else {
                            stringResource(R.string.input_hint_en)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendClick() }),
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("message_input")
            )

            val canSend = inputText.isNotBlank() && status != AssistantStatus.THINKING
            IconButton(
                onClick = onSendClick,
                enabled = canSend,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend) {
                            Brush.linearGradient(listOf(ElectricPurple, NeonCyan))
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    )
                    .testTag("send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = stringResource(R.string.cd_send_button),
                    tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
