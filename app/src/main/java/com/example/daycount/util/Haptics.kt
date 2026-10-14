package com.example.daycount.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** Haptic feedback that respects the user's Haptic Feedback setting. */
class Haptics(private val feedback: HapticFeedback, private val enabled: Boolean) {
    fun confirm() {
        if (enabled) feedback.performHapticFeedback(HapticFeedbackType.Confirm)
    }

    fun tick() {
        if (enabled) feedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
    }
}

@Composable
fun rememberHaptics(enabled: Boolean): Haptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback, enabled) { Haptics(feedback, enabled) }
}
