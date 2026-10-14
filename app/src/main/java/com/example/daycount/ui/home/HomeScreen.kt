package com.example.daycount.ui.home

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.daycount.model.QuickRange
import com.example.daycount.ui.ContentWidth
import com.example.daycount.ui.DayCountViewModel
import com.example.daycount.ui.SectionLabel
import com.example.daycount.ui.rememberDayCountFormatter
import com.example.daycount.ui.rememberStagedReveal
import com.example.daycount.util.ShareUtils
import com.example.daycount.util.pickerMillisToLocalDate
import com.example.daycount.util.rememberHaptics
import com.example.daycount.util.toPickerMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class DateField { FIRST, SECOND }

private val PickerYearRange = 1000..3000

@Composable
fun HomeScreen(
    viewModel: DayCountViewModel,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val formatter = rememberDayCountFormatter()
    val haptics = rememberHaptics(settings.hapticFeedback)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val resultRequester = remember { BringIntoViewRequester() }
    var pickerTarget by rememberSaveable { mutableStateOf<DateField?>(null) }

    val result = uiState.result
    val reveal = rememberStagedReveal(
        key = result?.let { it to uiState.countRequestId },
        enabled = settings.animations,
    )

    fun showMessage(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    ContentWidth(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Header(
                onReset = {
                    haptics.tick()
                    viewModel.reset()
                },
            )

            Spacer(Modifier.height(20.dp))

            DateCard(
                label = "START DATE",
                accessibilityName = "Start date",
                date = uiState.firstDate,
                formatter = formatter,
                onClick = { pickerTarget = DateField.FIRST },
                onToday = {
                    haptics.tick()
                    viewModel.setFirstDateToToday()
                },
            )
            SwapButton(
                onClick = {
                    haptics.tick()
                    viewModel.swapDates()
                },
                modifier = Modifier.padding(vertical = 6.dp),
            )
            DateCard(
                label = "END DATE",
                accessibilityName = "End date",
                date = uiState.secondDate,
                formatter = formatter,
                onClick = { pickerTarget = DateField.SECOND },
                onToday = {
                    haptics.tick()
                    viewModel.setSecondDateToToday()
                },
            )

            AnimatedVisibility(visible = uiState.isEndBeforeStart, enter = fadeIn(), exit = fadeOut()) {
                MessageCard(
                    title = "END DATE IS BEFORE START DATE",
                    message = "Counts below use the absolute range.",
                    isError = true,
                    modifier = Modifier.padding(top = 12.dp),
                    action = {
                        TextButton(onClick = {
                            haptics.tick()
                            viewModel.swapDates()
                        }) { Text("SWAP") }
                    },
                )
            }

            Spacer(Modifier.height(20.dp))
            QuickRanges(
                onQuickRange = {
                    haptics.tick()
                    viewModel.applyQuickRange(it)
                },
                onShiftWeek = {
                    haptics.tick()
                    viewModel.shiftWeek(it)
                },
                onShiftMonth = {
                    haptics.tick()
                    viewModel.shiftMonth(it)
                },
            )

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (viewModel.countDays()) {
                        haptics.confirm()
                        scope.launch {
                            delay(60)
                            resultRequester.bringIntoView()
                        }
                    }
                },
                enabled = uiState.canCount && !uiState.isCalculating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics { contentDescription = "Count days in selected range." },
            ) {
                Text("COUNT DAYS", style = MaterialTheme.typography.titleMedium)
            }
            AnimatedVisibility(visible = uiState.isSavedToHistory, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Saved to recent calculations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            val errorMessage = uiState.errorMessage
            if (result == null) {
                MessageCard(
                    message = errorMessage ?: "Select a start and end date to count the days.",
                    isError = errorMessage != null,
                )
            } else {
                if (errorMessage != null && !uiState.isEndBeforeStart) {
                    MessageCard(
                        message = errorMessage,
                        isError = true,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.bringIntoViewRequester(resultRequester),
                ) {
                    MainResultCard(
                        result = result,
                        showReordered = result.wasReordered && !settings.preserveDateOrder,
                        formatter = formatter,
                        reveal = reveal,
                    )
                    BreakdownCard(
                        result = result,
                        showPercentages = settings.showPercentages,
                        formatter = formatter,
                        reveal = reveal,
                    )
                }
                Spacer(Modifier.height(12.dp))
                RangeDetailsCard(result = result, formatter = formatter)
                if (settings.showDayBreakdown) {
                    Spacer(Modifier.height(12.dp))
                    DayBreakdownCard(result = result, formatter = formatter)
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            val text = viewModel.copyText() ?: return@OutlinedButton
                            if (ShareUtils.copyToClipboard(context, text)) {
                                haptics.tick()
                                // Android 13+ shows its own clipboard confirmation.
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                    showMessage("Result copied")
                                }
                            } else {
                                showMessage("Unable to copy the result.")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .semantics { contentDescription = "Copy date count result." },
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("COPY RESULT")
                    }
                    Button(
                        onClick = {
                            val text = viewModel.shareText() ?: return@Button
                            if (!ShareUtils.shareText(context, text)) {
                                showMessage("No app available to share with.")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .semantics { contentDescription = "Share date count result." },
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("SHARE RESULT")
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    pickerTarget?.let { target ->
        val initial = if (target == DateField.FIRST) uiState.firstDate else uiState.secondDate
        DayCountDatePickerDialog(
            title = if (target == DateField.FIRST) "Start date" else "End date",
            initialDate = initial,
            onDismiss = { pickerTarget = null },
            onConfirm = { date ->
                pickerTarget = null
                haptics.tick()
                if (target == DateField.FIRST) viewModel.selectFirstDate(date) else viewModel.selectSecondDate(date)
            },
        )
    }
}

@Composable
private fun Header(onReset: () -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "DayCount",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = "Count every day in a date range.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(
            onClick = onReset,
            modifier = Modifier.semantics { contentDescription = "Reset both dates to today." },
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("RESET")
        }
    }
}

@Composable
private fun QuickRanges(
    onQuickRange: (QuickRange) -> Unit,
    onShiftWeek: (Int) -> Unit,
    onShiftMonth: (Int) -> Unit,
) {
    SectionLabel("Quick ranges")
    Spacer(Modifier.height(8.dp))
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        listOf(
            QuickRange.TODAY to "TODAY",
            QuickRange.THIS_WEEK to "THIS WEEK",
            QuickRange.THIS_MONTH to "THIS MONTH",
            QuickRange.THIS_YEAR to "THIS YEAR",
        ).forEach { (range, label) ->
            AssistChip(
                onClick = { onQuickRange(range) },
                label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                shape = MaterialTheme.shapes.small,
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                modifier = Modifier.semantics {
                    contentDescription = "Set range to ${label.lowercase()}"
                },
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ShiftControl(
            label = "WEEK",
            previousDescription = "Previous week",
            nextDescription = "Next week",
            onPrevious = { onShiftWeek(-1) },
            onNext = { onShiftWeek(1) },
            modifier = Modifier.weight(1f),
        )
        ShiftControl(
            label = "MONTH",
            previousDescription = "Previous month",
            nextDescription = "Next month",
            onPrevious = { onShiftMonth(-1) },
            onNext = { onShiftMonth(1) },
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayCountDatePickerDialog(
    title: String,
    initialDate: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val initialMillis = initialDate
        ?.takeIf { it.year in PickerYearRange }
        ?.toPickerMillis()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        yearRange = PickerYearRange,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onConfirm(it.pickerMillisToLocalDate()) } },
                enabled = state.selectedDateMillis != null,
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    ) {
        DatePicker(
            state = state,
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                )
            },
        )
    }
}

