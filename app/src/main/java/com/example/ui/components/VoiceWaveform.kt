package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.NeonCyan
import com.example.viewmodel.AssistantStatus
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun VoiceStatusStrip(
    status: AssistantStatus,
    rmsLevel: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_transition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val statusText = when (status) {
        AssistantStatus.LISTENING -> stringResource(R.string.status_listening)
        AssistantStatus.THINKING -> stringResource(R.string.status_thinking)
        AssistantStatus.SPEAKING -> stringResource(R.string.status_speaking)
        AssistantStatus.ERROR -> stringResource(R.string.status_error)
        AssistantStatus.IDLE -> stringResource(R.string.status_ready)
    }

    val accentColor by animateColorAsState(
        targetValue = when (status) {
            AssistantStatus.LISTENING -> NeonCyan
            AssistantStatus.THINKING -> ElectricPurple
            AssistantStatus.SPEAKING -> EmeraldGlow
            AssistantStatus.ERROR -> CrimsonGlow
            AssistantStatus.IDLE -> MaterialTheme.colorScheme.primary
        },
        label = "status_accent"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.45f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accentColor)
        )

        Text(
            text = statusText,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Canvas(
            modifier = Modifier
                .width(64.dp)
                .height(20.dp)
        ) {
            val barCount = 10
            val barSpacing = 3.dp.toPx()
            val barWidth = (size.width - (barCount - 1) * barSpacing) / barCount
            val centerY = size.height / 2f

            for (i in 0 until barCount) {
                val offsetWave = abs(sin(phase + i * 0.65f))
                val amplitude = when (status) {
                    AssistantStatus.LISTENING -> 0.25f + (0.75f * ((rmsLevel * 0.7f) + offsetWave * 0.3f))
                    AssistantStatus.SPEAKING -> 0.30f + 0.70f * offsetWave
                    AssistantStatus.THINKING -> 0.22f + 0.45f * abs(sin(phase * 1.4f + i * 0.8f))
                    AssistantStatus.ERROR -> 0.18f
                    AssistantStatus.IDLE -> 0.14f + 0.12f * offsetWave
                }.coerceIn(0.12f, 1f)

                val barHeight = size.height * amplitude
                val x = i * (barWidth + barSpacing)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(NeonCyan, ElectricPurple)
                    ),
                    topLeft = Offset(x, centerY - barHeight / 2f),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}
