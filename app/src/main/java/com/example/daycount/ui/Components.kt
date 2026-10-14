package com.example.daycount.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.daycount.domain.DayCountFormatter

/** Formatter bound to the current configuration locale. */
@Composable
fun rememberDayCountFormatter(): DayCountFormatter {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { DayCountFormatter(locale) }
}

/** Centers content and caps its width so layouts stay readable on tablets and in landscape. */
@Composable
fun ContentWidth(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(), content = content)
    }
}

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier.semantics { heading() },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String?,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = message?.let { { Text(it) } },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/**
 * Staged reveal: stage 0 starts immediately and each later stage starts slightly after the
 * previous one. The whole sequence takes [durationMillis]. Values are read lazily so the
 * animation runs in the draw phase without recomposing the content.
 */
class StagedReveal internal constructor(private val progress: () -> Float) {
    fun stage(index: Int): Float =
        ((progress() - index * STAGE_OFFSET) / STAGE_LENGTH).coerceIn(0f, 1f)

    private companion object {
        const val STAGE_OFFSET = 0.15f
        const val STAGE_LENGTH = 0.55f
    }
}

@Composable
fun rememberStagedReveal(key: Any?, enabled: Boolean, durationMillis: Int = 480): StagedReveal {
    val progress = remember { Animatable(1f) }
    LaunchedEffect(key, enabled) {
        if (enabled && key != null) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMillis, easing = LinearEasing))
        } else {
            progress.snapTo(1f)
        }
    }
    return remember(progress) { StagedReveal { progress.value } }
}

fun Modifier.revealStage(reveal: StagedReveal, index: Int): Modifier = graphicsLayer {
    val value = reveal.stage(index)
    alpha = value
    translationY = (1f - value) * 14.dp.toPx()
}
