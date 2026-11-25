package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ChatMessage
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.DeepViolet
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.NeonCyan

@Composable
fun ChatBubble(
    message: ChatMessage,
    isCurrentlySpeaking: Boolean,
    onSpeakClick: () -> Unit,
    onStopSpeakClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        contentAlignment = alignment,
        modifier = modifier.fillMaxWidth()
    ) {
        val bubbleShape = if (message.isUser) {
            RoundedCornerShape(topStart = 22.dp, topEnd = 6.dp, bottomStart = 22.dp, bottomEnd = 22.dp)
        } else {
            RoundedCornerShape(topStart = 6.dp, topEnd = 22.dp, bottomStart = 22.dp, bottomEnd = 22.dp)
        }

        val borderColor = when {
            message.isError -> CrimsonGlow.copy(alpha = 0.65f)
            message.isUser -> ElectricPurple.copy(alpha = 0.55f)
            else -> NeonCyan.copy(alpha = 0.35f)
        }

        val containerModifier = if (message.isUser) {
            Modifier.background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        DeepViolet.copy(alpha = 0.92f),
                        ElectricPurple.copy(alpha = 0.78f)
                    )
                ),
                shape = bubbleShape
            )
        } else {
            Modifier.background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.90f),
                shape = bubbleShape
            )
        }

        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .clip(bubbleShape)
                .then(containerModifier)
                .border(width = 1.dp, color = borderColor, shape = bubbleShape)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Optional verified phone action badge
            message.actionBadge?.let { badge ->
                val badgeTint = if (badge.succeeded) EmeraldGlow else AmberGlow
                Surface(
                    color = badgeTint.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (badge.succeeded) {
                                Icons.Rounded.CheckCircle
                            } else {
                                Icons.Rounded.ErrorOutline
                            },
                            contentDescription = null,
                            tint = badgeTint,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = badge.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeTint
                        )
                    }
                }
            }

            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (message.isUser) Color.White else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (message.isVoiceInput) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = null,
                            tint = if (message.isUser) Color.White.copy(alpha = 0.8f) else NeonCyan,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Text(
                        text = message.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (message.isUser) {
                            Color.White.copy(alpha = 0.72f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                if (!message.isUser && !message.isError) {
                    IconButton(
                        onClick = if (isCurrentlySpeaking) onStopSpeakClick else onSpeakClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(36.dp)
                            .testTag("speak_message_button")
                    ) {
                        Icon(
                            imageVector = if (isCurrentlySpeaking) {
                                Icons.Rounded.StopCircle
                            } else {
                                Icons.AutoMirrored.Rounded.VolumeUp
                            },
                            contentDescription = if (isCurrentlySpeaking) {
                                stringResource(R.string.cd_stop_speaking_button)
                            } else {
                                stringResource(R.string.cd_speak_message)
                            },
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
