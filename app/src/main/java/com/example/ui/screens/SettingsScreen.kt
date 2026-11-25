package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.AssistantLanguage
import com.example.data.GeminiModelOption
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.NeonCyan
import com.example.viewmodel.AssistantUiState
import com.example.viewmodel.ConnectionTestState
import com.google.firebase.FirebaseApp
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    uiState: AssistantUiState,
    onBack: () -> Unit,
    onSelectModel: (GeminiModelOption) -> Unit,
    onTestGeminiConnection: () -> Unit,
    onSelectLanguage: (AssistantLanguage) -> Unit,
    onSelectSpeechLocale: (String) -> Unit,
    onSelectVoiceName: (String) -> Unit,
    onSpeechSpeedChange: (Float) -> Unit,
    onSpeechPitchChange: (Float) -> Unit,
    onAutoSpeakChange: (Boolean) -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
    onSaveHistoryChange: (Boolean) -> Unit,
    onIncludeContextChange: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    onMicPermissionResult: (Boolean) -> Unit,
    onInstallTtsData: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }
    var voiceMenuExpanded by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onMicPermissionResult
    )

    val settings = uiState.settings
    val firebaseProjectId = remember {
        try {
            FirebaseApp.getInstance().options.projectId ?: "Configured"
        } catch (_: Exception) {
            "Not initialized"
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back_button),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 1. Gemini AI Configuration (Firebase AI Logic + App Check)
            SettingsSectionCard(
                title = "Gemini AI Configuration",
                icon = Icons.Rounded.AutoAwesome
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VerifiedUser,
                        contentDescription = null,
                        tint = EmeraldGlow,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Firebase AI Logic + App Check Active (Project: $firebaseProjectId)",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldGlow
                    )
                }
                Text(
                    text = "Protected via Firebase App Check (${if (BuildConfig.DEBUG) "Debug Token" else "reCAPTCHA Enterprise"}). No private API key is hardcoded in the APK.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                GeminiModelOption.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectModel(option) }
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                    ) {
                        RadioButton(
                            selected = settings.selectedModel == option,
                            onClick = { onSelectModel(option) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = option.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Button(
                    onClick = onTestGeminiConnection,
                    enabled = uiState.connectionTestState !is ConnectionTestState.Testing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_gemini_button")
                ) {
                    if (uiState.connectionTestState is ConnectionTestState.Testing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Test Gemini Connection")
                    }
                }

                when (val testState = uiState.connectionTestState) {
                    is ConnectionTestState.Success -> {
                        Text(
                            text = "Success: ${testState.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldGlow
                        )
                    }
                    is ConnectionTestState.Error -> {
                        Text(
                            text = "Error: ${testState.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = CrimsonGlow
                        )
                    }
                    else -> Unit
                }
            }

            // 2. Language Selection
            SettingsSectionCard(
                title = "Language Selection",
                icon = Icons.Rounded.Language
            ) {
                Text(
                    text = "Assistant Reply Language",
                    style = MaterialTheme.typography.labelLarge
                )
                AssistantLanguage.entries.forEach { lang ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectLanguage(lang) }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = settings.preferredLanguage == lang,
                            onClick = { onSelectLanguage(lang) }
                        )
                        Text(
                            text = lang.displayName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                Text(
                    text = "Voice Recognition Input Language",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val isSinhala = settings.speechInputLocale.startsWith("si")
                    OutlinedButton(
                        onClick = { onSelectSpeechLocale("si-LK") },
                        colors = if (isSinhala) {
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = NeonCyan.copy(alpha = 0.16f)
                            )
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("සිංහල (si-LK)")
                    }
                    OutlinedButton(
                        onClick = { onSelectSpeechLocale("en-US") },
                        colors = if (!isSinhala) {
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = NeonCyan.copy(alpha = 0.16f)
                            )
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("English (en-US)")
                    }
                }
            }

            // 3. Voice & Speech Controls
            SettingsSectionCard(
                title = "Voice & Speech",
                icon = Icons.Rounded.RecordVoiceOver
            ) {
                SettingsSwitchRow(
                    title = "Auto-Speak Responses",
                    subtitle = "Read Gemini replies and action confirmations aloud",
                    checked = settings.autoSpeak,
                    onCheckedChange = onAutoSpeakChange
                )

                // Voice Picker
                Box(modifier = Modifier.fillMaxWidth()) {
                    val selectedLabel = uiState.availableVoices
                        .firstOrNull { it.name == settings.selectedVoiceName }
                        ?.label ?: "Auto (Match Sinhala / English)"

                    OutlinedButton(
                        onClick = { voiceMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "TTS Voice: $selectedLabel",
                            maxLines = 1
                        )
                    }

                    DropdownMenu(
                        expanded = voiceMenuExpanded,
                        onDismissRequest = { voiceMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Auto (Match Sinhala / English)") },
                            onClick = {
                                onSelectVoiceName("")
                                voiceMenuExpanded = false
                            }
                        )
                        uiState.availableVoices.take(15).forEach { voice ->
                            DropdownMenuItem(
                                text = { Text(voice.label) },
                                onClick = {
                                    onSelectVoiceName(voice.name)
                                    voiceMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                if (!uiState.isSinhalaTtsSupported) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Sinhala TTS voice pack not installed on device.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onInstallTtsData) {
                                Text("Install")
                            }
                        }
                    }
                }

                // Speech Speed Slider
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Speech Speed", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            text = "${((settings.speechSpeed * 10).roundToInt() / 10f)}x",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonCyan
                        )
                    }
                    Slider(
                        value = settings.speechSpeed,
                        onValueChange = onSpeechSpeedChange,
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = ElectricPurple
                        )
                    )
                }

                // Speech Pitch Slider
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Speech Pitch", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            text = "${((settings.speechPitch * 10).roundToInt() / 10f)}x",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonCyan
                        )
                    }
                    Slider(
                        value = settings.speechPitch,
                        onValueChange = onSpeechPitchChange,
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = ElectricPurple
                        )
                    )
                }
            }

            // 4. Appearance
            SettingsSectionCard(
                title = "Appearance",
                icon = Icons.Rounded.DarkMode
            ) {
                SettingsSwitchRow(
                    title = "Dark Neon Theme",
                    subtitle = "Black & dark purple futuristic UI (turn off for Light theme)",
                    checked = settings.darkTheme,
                    onCheckedChange = onDarkThemeChange
                )
            }

            // 5. Privacy & Conversation History
            SettingsSectionCard(
                title = "Privacy & History",
                icon = Icons.Rounded.Security
            ) {
                SettingsSwitchRow(
                    title = "Keep Conversation Context",
                    subtitle = "Remember recent turns during the session for follow-up questions",
                    checked = settings.saveHistory,
                    onCheckedChange = onSaveHistoryChange
                )
                SettingsSwitchRow(
                    title = "Share Device Telemetry with AI",
                    subtitle = "Include battery and network state to improve context-aware replies",
                    checked = settings.includeDeviceContext,
                    onCheckedChange = onIncludeContextChange
                )
                OutlinedButton(
                    onClick = { showClearDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonGlow),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clear_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Clear Conversation History (${uiState.messages.size})")
                }
            }

            // 6. Permissions & Device Compatibility
            SettingsSectionCard(
                title = "Permissions & Compatibility",
                icon = Icons.Rounded.Mic
            ) {
                DiagnosticStatusRow(
                    label = "Microphone Permission",
                    status = if (uiState.hasMicrophonePermission) "Granted" else "Not Granted",
                    isHealthy = uiState.hasMicrophonePermission
                )
                DiagnosticStatusRow(
                    label = "Speech Recognition Service",
                    status = if (uiState.isSpeechRecognitionAvailable) "Available" else "Unavailable",
                    isHealthy = uiState.isSpeechRecognitionAvailable
                )
                DiagnosticStatusRow(
                    label = "Sinhala TTS Voice",
                    status = if (uiState.isSinhalaTtsSupported) "Ready" else "Not Installed",
                    isHealthy = uiState.isSinhalaTtsSupported
                )
                DiagnosticStatusRow(
                    label = "Flashlight Hardware",
                    status = if (uiState.telemetry.hasFlashlight) "Supported" else "Not Present",
                    isHealthy = uiState.telemetry.hasFlashlight
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!uiState.hasMicrophonePermission) {
                        Button(
                            onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Grant Mic")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("App Permissions")
                    }
                }
            }

            // 7. App Information
            SettingsSectionCard(
                title = "App Information",
                icon = Icons.Rounded.Info
            ) {
                InfoRow("App Name", stringResource(R.string.brand_header))
                InfoRow("Developer", stringResource(R.string.developer_name))
                InfoRow("Package Name", BuildConfig.APPLICATION_ID)
                InfoRow("Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                InfoRow("Android Compatibility", "Android 8.0+ (API 26 – 36)")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Conversation?") },
            text = { Text("All messages in the current conversation will be removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear", color = CrimsonGlow)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        content()
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DiagnosticStatusRow(
    label: String,
    status: String,
    isHealthy: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isHealthy) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldGlow,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = status,
                style = MaterialTheme.typography.labelMedium,
                color = if (isHealthy) EmeraldGlow else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
