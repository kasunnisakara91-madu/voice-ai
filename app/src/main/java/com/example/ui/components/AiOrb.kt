package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.DeepViolet
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PlasmaPink
import com.example.viewmodel.AssistantStatus
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AiOrb(
    status: AssistantStatus,
    rmsLevel: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 176.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ai_orb_transition")

    val rotationDuration = when (status) {
        AssistantStatus.THINKING -> 2200
        AssistantStatus.LISTENING -> 4200
        AssistantStatus.SPEAKING -> 3600
        else -> 9000
    }

    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = rotationDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle"
    )

    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_scale"
    )

    val smoothRms by animateFloatAsState(
        targetValue = if (status == AssistantStatus.LISTENING) rmsLevel else 0f,
        animationSpec = spring(stiffness = 380f),
        label = "smooth_rms"
    )

    val primaryGlow = when (status) {
        AssistantStatus.LISTENING -> NeonCyan
        AssistantStatus.THINKING -> ElectricPurple
        AssistantStatus.SPEAKING -> Color(0xFF38BDF8)
        AssistantStatus.ERROR -> CrimsonGlow
        AssistantStatus.IDLE -> NeonCyan
    }

    val secondaryGlow = when (status) {
        AssistantStatus.LISTENING -> ElectricPurple
        AssistantStatus.THINKING -> PlasmaPink
        AssistantStatus.SPEAKING -> ElectricPurple
        AssistantStatus.ERROR -> PlasmaPink
        AssistantStatus.IDLE -> DeepViolet
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = NeonCyan),
                onClick = onClick
            )
            .testTag("ai_orb_button")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension * 0.30f
            val dynamicRadius = baseRadius * breathingScale + (this.size.minDimension * 0.08f * smoothRms)

            // Outer ambient radial halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlow.copy(alpha = 0.32f + smoothRms * 0.2f),
                        secondaryGlow.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = this.size.minDimension * 0.50f
                ),
                radius = this.size.minDimension * 0.50f,
                center = center
            )

            // Rotating outer neon ring
            rotate(degrees = orbitAngle, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            primaryGlow,
                            secondaryGlow,
                            Color.Transparent,
                            primaryGlow.copy(alpha = 0.7f),
                            primaryGlow
                        ),
                        center = center
                    ),
                    startAngle = 0f,
                    sweepAngle = 290f,
                    useCenter = false,
                    topLeft = Offset(
                        center.x - dynamicRadius * 1.28f,
                        center.y - dynamicRadius * 1.28f
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        dynamicRadius * 2.56f,
                        dynamicRadius * 2.56f
                    ),
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Counter-rotating inner orbital arc
            rotate(degrees = -orbitAngle * 1.35f, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            secondaryGlow,
                            primaryGlow,
                            Color.Transparent,
                            secondaryGlow
                        ),
                        center = center
                    ),
                    startAngle = 40f,
                    sweepAngle = 220f,
                    useCenter = false,
                    topLeft = Offset(
                        center.x - dynamicRadius * 1.08f,
                        center.y - dynamicRadius * 1.08f
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        dynamicRadius * 2.16f,
                        dynamicRadius * 2.16f
                    ),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Core glowing plasma sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.92f),
                        primaryGlow.copy(alpha = 0.85f),
                        secondaryGlow.copy(alpha = 0.90f),
                        Color(0xFF140A2E)
                    ),
                    center = Offset(center.x - dynamicRadius * 0.18f, center.y - dynamicRadius * 0.22f),
                    radius = dynamicRadius * 1.05f
                ),
                radius = dynamicRadius * 0.90f,
                center = center
            )

            // Orbiting energy particles
            for (i in 0 until 3) {
                val angleRad = Math.toRadians((orbitAngle + i * 120f).toDouble())
                val orbitDist = dynamicRadius * 1.28f
                val px = center.x + (cos(angleRad) * orbitDist).toFloat()
                val py = center.y + (sin(angleRad) * orbitDist).toFloat()
                drawCircle(
                    color = if (i % 2 == 0) primaryGlow else Color.White,
                    radius = 3.dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }

        val centerIcon = when (status) {
            AssistantStatus.LISTENING -> Icons.Rounded.GraphicEq
            AssistantStatus.THINKING -> Icons.Rounded.AutoAwesome
            AssistantStatus.SPEAKING -> Icons.Rounded.VolumeUp
            else -> Icons.Rounded.Mic
        }

        Icon(
            imageVector = centerIcon,
            contentDescription = stringResource(R.string.cd_mic_button),
            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f),
            modifier = Modifier.size(size * 0.20f)
        )
    }
}
