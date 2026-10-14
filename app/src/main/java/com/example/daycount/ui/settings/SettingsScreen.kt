package com.example.daycount.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.daycount.model.ThemeMode
import com.example.daycount.ui.ConfirmDialog
import com.example.daycount.ui.ContentWidth
import com.example.daycount.ui.DayCountViewModel
import com.example.daycount.ui.SectionLabel
import com.example.daycount.ui.theme.supportsDynamicColor
import com.example.daycount.util.rememberHaptics

@Composable
fun SettingsScreen(viewModel: DayCountViewModel, modifier: Modifier = Modifier) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = rememberHaptics(settings.hapticFeedback)
    var confirmClearHistory by remember { mutableStateOf(false) }
    var confirmResetSettings by remember { mutableStateOf(false) }

    fun toggle(action: (Boolean) -> Unit): (Boolean) -> Unit = { value ->
        haptics.tick()
        action(value)
    }

    ContentWidth(modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() },
            )

            SettingsGroup("Appearance") {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text("Theme", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    val options = listOf(
                        ThemeMode.SYSTEM to "System",
                        ThemeMode.LIGHT to "Light",
                        ThemeMode.DARK to "Dark",
                    )
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        options.forEachIndexed { index, (mode, label) ->
                            SegmentedButton(
                                selected = settings.themeMode == mode,
                                onClick = {
                                    haptics.tick()
                                    viewModel.setThemeMode(mode)
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                            ) { Text(label) }
                        }
                    }
                }
                GroupDivider()
                val dynamicSupported = supportsDynamicColor()
                SwitchRow(
                    title = "Dynamic color",
                    subtitle = if (dynamicSupported) "Use colors from your wallpaper"
                    else "Requires Android 12 or later",
                    checked = settings.dynamicColor && dynamicSupported,
                    enabled = dynamicSupported,
                    onCheckedChange = toggle(viewModel::setDynamicColor),
                )
            }

            SettingsGroup("Interaction") {
                SwitchRow(
                    title = "Haptic feedback",
                    subtitle = "Subtle vibration on actions",
                    checked = settings.hapticFeedback,
                    onCheckedChange = toggle(viewModel::setHapticFeedback),
                )
                GroupDivider()
                SwitchRow(
                    title = "Animations",
                    subtitle = "Reveal results and play the splash animation",
                    checked = settings.animations,
                    onCheckedChange = toggle(viewModel::setAnimations),
                )
            }

            SettingsGroup("Calculation") {
                SwitchRow(
                    title = "Preserve date order",
                    subtitle = if (settings.preserveDateOrder) "Warn when the end date is before the start date"
                    else "Reversed dates are reordered automatically",
                    checked = settings.preserveDateOrder,
                    onCheckedChange = toggle(viewModel::setPreserveDateOrder),
                )
                GroupDivider()
                SwitchRow(
                    title = "Preserve last dates",
                    subtitle = "Restore the last selected dates when the app opens",
                    checked = settings.preserveLastDates,
                    onCheckedChange = toggle(viewModel::setPreserveLastDates),
                )
                GroupDivider()
                SwitchRow(
                    title = "Show percentages",
                    subtitle = "Weekday and weekend share of the range",
                    checked = settings.showPercentages,
                    onCheckedChange = toggle(viewModel::setShowPercentages),
                )
                GroupDivider()
                SwitchRow(
                    title = "Show day breakdown",
                    subtitle = "Count of each day from Monday to Sunday",
                    checked = settings.showDayBreakdown,
                    onCheckedChange = toggle(viewModel::setShowDayBreakdown),
                )
            }

            SettingsGroup("History") {
                SwitchRow(
                    title = "Confirm history delete",
                    subtitle = "Ask before removing a calculation",
                    checked = settings.confirmHistoryDelete,
                    onCheckedChange = toggle(viewModel::setConfirmHistoryDelete),
                )
                GroupDivider()
                ActionRow(
                    title = "Clear history",
                    subtitle = when (val size = uiState.history.size) {
                        0 -> "No recent calculations"
                        1 -> "1 recent calculation"
                        else -> "$size recent calculations"
                    },
                    actionLabel = "CLEAR",
                    enabled = uiState.history.isNotEmpty(),
                    onClick = { confirmClearHistory = true },
                )
            }

            SettingsGroup("Reset") {
                ActionRow(
                    title = "Reset settings",
                    subtitle = "Restore every setting to its default",
                    actionLabel = "RESET",
                    enabled = true,
                    onClick = { confirmResetSettings = true },
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (confirmClearHistory) {
        ConfirmDialog(
            title = "Clear all recent calculations?",
            message = null,
            confirmLabel = "Clear",
            onConfirm = {
                haptics.confirm()
                viewModel.clearHistory()
                confirmClearHistory = false
            },
            onDismiss = { confirmClearHistory = false },
        )
    }

    if (confirmResetSettings) {
        ConfirmDialog(
            title = "Reset all settings?",
            message = "History and selected dates are kept.",
            confirmLabel = "Reset",
            onConfirm = {
                haptics.confirm()
                viewModel.resetSettings()
                confirmResetSettings = false
            },
            onDismiss = { confirmResetSettings = false },
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column {
        SectionLabel(title, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun ActionRow(
    title: String,
    subtitle: String,
    actionLabel: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onClick, enabled = enabled) {
            Text(actionLabel, color = if (enabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
        }
    }
}
