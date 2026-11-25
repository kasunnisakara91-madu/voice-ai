package com.example.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import com.example.jarvis.JarvisSystemHelper
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.NeonCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisHubSheet(
    standbyEnabled: Boolean,
    autoListenEnabled: Boolean,
    hasMicPermission: Boolean,
    onToggleStandby: (Boolean) -> Unit,
    onToggleAutoListen: (Boolean) -> Unit,
    onMicPermissionGranted: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var statusFeedback by remember { mutableStateOf<String?>(null) }
    val isDefaultAssistant = remember { JarvisSystemHelper.isDefaultAssistant(context) }

    val standbyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val micGranted = results[Manifest.permission.RECORD_AUDIO]
            ?: JarvisSystemHelper.hasMicrophonePermission(context)
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            results[Manifest.permission.POST_NOTIFICATIONS]
                ?: JarvisSystemHelper.hasNotificationPermission(context)
        } else {
            true
        }

        onMicPermissionGranted(micGranted)

        if (micGranted && notifGranted) {
            val started = JarvisSystemHelper.startJarvisStandbyService(context)
            onToggleStandby(started)
            statusFeedback = if (started) {
                "Jarvis Standby Notification is now active."
            } else {
                "Unable to start Jarvis Standby service."
            }
        } else {
            onToggleStandby(false)
            statusFeedback = "Microphone and Notification permissions are required for Jarvis Standby."
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Jarvis System Control",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "ඇප් එකේ නොඉඳ ඕනෑම තැනක සිට phone එක පාලනය කරන්න",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (statusFeedback != null) {
                Surface(
                    color = NeonCyan.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusFeedback.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonCyan,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // 1. Default Digital Assistant
            JarvisFeatureCard(
                icon = Icons.Rounded.PhoneAndroid,
                title = "1. Default Digital Assistant",
                description = if (isDefaultAssistant) {
                    "Active! Long-press Power or swipe from bottom corner anywhere to summon Jarvis."
                } else {
                    "Set Assistant Voice as your phone's Digital Assistant to summon Jarvis over any app."
                },
                badgeText = if (isDefaultAssistant) "Active" else null
            ) {
                OutlinedButton(
                    onClick = { JarvisSystemHelper.openAssistantSettings(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("set_default_assistant_button")
                ) {
                    Text(if (isDefaultAssistant) "Manage Default Assistant" else "Set as Default Assistant")
                }
            }

            // 2. Quick Settings Tile
            JarvisFeatureCard(
                icon = Icons.Rounded.TouchApp,
                title = "2. Quick Settings Tile",
                description = "Swipe down from the top of any screen and tap 'Jarvis Voice' to launch floating voice control."
            ) {
                OutlinedButton(
                    onClick = {
                        JarvisSystemHelper.requestAddQuickSettingsTile(context) { msg ->
                            statusFeedback = msg
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_qs_tile_button")
                ) {
                    Text("Add Quick Settings Tile")
                }
            }

            // 3. Home Screen Widget
            JarvisFeatureCard(
                icon = Icons.Rounded.Widgets,
                title = "3. Home Screen Jarvis Bar",
                description = "Pin a floating Jarvis Mic & Flashlight bar directly onto your phone's home screen."
            ) {
                OutlinedButton(
                    onClick = {
                        val pinned = JarvisSystemHelper.requestPinHomeWidget(context)
                        statusFeedback = if (pinned) {
                            "Follow the system prompt to place the Jarvis widget on your home screen."
                        } else {
                            "Long-press your home screen -> Widgets -> add 'Assistant Voice'."
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_widget_button")
                ) {
                    Text("Pin Home Screen Widget")
                }
            }

            // 4. Standby Persistent Notification
            JarvisFeatureCard(
                icon = Icons.Rounded.NotificationsActive,
                title = "4. Jarvis Standby Notification",
                description = "Keep a quick Speak & Flashlight bar in your notification shade and lock screen."
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (standbyEnabled) "Standby Notification ON" else "Standby Notification OFF",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = standbyEnabled,
                        onCheckedChange = { enable ->
                            if (!enable) {
                                JarvisSystemHelper.stopJarvisStandbyService(context)
                                onToggleStandby(false)
                                statusFeedback = "Jarvis Standby Notification stopped."
                            } else {
                                val needsMic = !hasMicPermission
                                val needsNotif = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !JarvisSystemHelper.hasNotificationPermission(context)

                                if (!needsMic && !needsNotif) {
                                    val started = JarvisSystemHelper.startJarvisStandbyService(context)
                                    onToggleStandby(started)
                                } else {
                                    val perms = buildList {
                                        add(Manifest.permission.RECORD_AUDIO)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            add(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }.toTypedArray()
                                    standbyPermissionLauncher.launch(perms)
                                }
                            }
                        },
                        modifier = Modifier.testTag("jarvis_standby_switch")
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Auto-listen when Jarvis opens",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = autoListenEnabled,
                        onCheckedChange = onToggleAutoListen
                    )
                }
            }

            Button(
                onClick = {
                    onDismiss()
                    JarvisSystemHelper.launchJarvisOverlay(context, autoListen = true)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("launch_jarvis_overlay_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Launch Floating Jarvis Overlay Now")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun JarvisFeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    badgeText: String? = null,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                RoundedCornerShape(18.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (badgeText != null) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldGlow
                )
            }
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }
}
