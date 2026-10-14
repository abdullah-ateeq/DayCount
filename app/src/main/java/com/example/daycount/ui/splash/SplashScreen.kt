package com.example.daycount.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val DayLabels = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

private const val ANIMATION_MILLIS = 1000
private const val HOLD_MILLIS = 200L
private const val STATIC_MILLIS = 700L

/**
 * Seven day blocks appear, separate into 5 weekdays + 2 weekend days, then the app name
 * fades in. Total time ≈ 1.2 s (0.7 s static when animations are off).
 */
@Composable
fun SplashScreen(animationsEnabled: Boolean, onFinished: () -> Unit) {
    val progress = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    val currentOnFinished by rememberUpdatedState(onFinished)

    LaunchedEffect(Unit) {
        if (animationsEnabled) {
            progress.animateTo(1f, tween(ANIMATION_MILLIS, easing = LinearEasing))
            delay(HOLD_MILLIS)
        } else {
            delay(STATIC_MILLIS)
        }
        currentOnFinished()
    }

    val p = progress.value
    fun window(start: Float, length: Float) = ((p - start) / length).coerceIn(0f, 1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clearAndSetSemantics { contentDescription = "DayCount. Count every day in a date range." },
    ) {
        DayBlocks(appear = { index -> window(index * 0.05f, 0.2f) }, group = window(0.45f, 0.2f))

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.graphicsLayer { alpha = window(0.55f, 0.2f) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("5 WEEKDAYS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                "  +  ",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("2 WEEKENDS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
        }

        Spacer(Modifier.height(36.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                val value = window(0.7f, 0.3f)
                alpha = value
                translationY = (1f - value) * 10.dp.toPx()
            },
        ) {
            Text(
                text = "DayCount",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Count every day in a date range.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DayBlocks(
    appear: (Int) -> Float,
    group: Float,
    blockWidth: Dp = 36.dp,
    blockHeight: Dp = 44.dp,
) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        DayLabels.forEachIndexed { index, label ->
            val weekend = index >= 5
            if (index == 5) Spacer(Modifier.width(4.dp + 10.dp * group))
            val accent = if (weekend) colors.tertiary else colors.primary
            val onAccent = if (weekend) colors.onTertiary else colors.onPrimary
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .graphicsLayer {
                        val value = appear(index)
                        alpha = value
                        translationY = (1f - value) * 12.dp.toPx()
                    }
                    .size(width = blockWidth, height = blockHeight)
                    .background(lerp(colors.surfaceContainerHighest, accent, group), RoundedCornerShape(10.dp)),
            ) {
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = lerp(colors.onSurfaceVariant, onAccent, group),
                )
            }
            if (index < DayLabels.lastIndex && index != 4) Spacer(Modifier.width(4.dp))
        }
    }
}

/** Static version of the splash mark, used on the About screen. */
@Composable
fun DayBlocksMark(modifier: Modifier = Modifier) {
    Box(modifier.clearAndSetSemantics { }) {
        DayBlocks(appear = { 1f }, group = 1f, blockWidth = 30.dp, blockHeight = 36.dp)
    }
}
