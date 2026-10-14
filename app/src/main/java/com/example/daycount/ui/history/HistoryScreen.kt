package com.example.daycount.ui.history

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.daycount.domain.DayCountFormatter
import com.example.daycount.model.DayCountHistory
import com.example.daycount.repository.DayCountHistoryRepository
import com.example.daycount.ui.ConfirmDialog
import com.example.daycount.ui.ContentWidth
import com.example.daycount.ui.DayCountViewModel
import com.example.daycount.ui.rememberDayCountFormatter
import com.example.daycount.util.rememberHaptics
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(
    viewModel: DayCountViewModel,
    onOpenEntry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val formatter = rememberDayCountFormatter()
    val haptics = rememberHaptics(settings.hapticFeedback)
    var pendingDelete by remember { mutableStateOf<DayCountHistory?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    val requestDelete: (DayCountHistory) -> Unit = { entry ->
        if (settings.confirmHistoryDelete) {
            pendingDelete = entry
        } else {
            haptics.tick()
            viewModel.deleteHistory(entry.id)
        }
    }

    ContentWidth(modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "History",
                            style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            text = "RECENT CALCULATIONS · LAST ${DayCountHistoryRepository.MAX_ENTRIES}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (uiState.history.isNotEmpty()) {
                        TextButton(onClick = { confirmClear = true }) { Text("CLEAR ALL") }
                    }
                }
            }

            if (uiState.history.isEmpty()) {
                item(key = "empty") { EmptyHistory() }
            } else {
                items(uiState.history, key = { it.id }) { entry ->
                    SwipeableHistoryItem(
                        entry = entry,
                        formatter = formatter,
                        onOpen = {
                            haptics.tick()
                            viewModel.loadFromHistory(entry)
                            onOpenEntry()
                        },
                        onDelete = { requestDelete(entry) },
                        modifier = Modifier.animateItem(),
                    )
                }
                item(key = "hint") {
                    Text(
                        text = "Tap an entry to load it. Swipe left or use the delete icon to remove it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    )
                }
            }
        }
    }

    pendingDelete?.let { entry ->
        ConfirmDialog(
            title = "Delete this calculation?",
            message = formatter.historyRange(entry),
            confirmLabel = "Delete",
            onConfirm = {
                haptics.tick()
                viewModel.deleteHistory(entry.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "Clear all recent calculations?",
            message = null,
            confirmLabel = "Clear",
            onConfirm = {
                haptics.confirm()
                viewModel.clearHistory()
                confirmClear = false
            },
            onDismiss = { confirmClear = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableHistoryItem(
    entry: DayCountHistory,
    formatter: DayCountFormatter,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnDelete by rememberUpdatedState(onDelete)
    val dismissState = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        onDismiss = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) currentOnDelete()
            // The row springs back; it disappears once the entry is removed from history.
            scope.launch { dismissState.reset() }
        },
        backgroundContent = {
            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
            ) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
        modifier = modifier,
    ) {
        HistoryItem(entry = entry, formatter = formatter, onOpen = onOpen, onDelete = onDelete)
    }
}

@Composable
private fun HistoryItem(
    entry: DayCountHistory,
    formatter: DayCountFormatter,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val calculatedAt = remember(entry.calculatedAt) {
        DateUtils.getRelativeTimeSpanString(
            entry.calculatedAt,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        ).toString()
    }
    Surface(
        onClick = onOpen,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = formatter.historyAccessibility(entry)
                role = Role.Button
                onClick(label = "Load this range") { onOpen(); true }
                customActions = listOf(
                    CustomAccessibilityAction("Delete calculation") { onDelete(); true }
                )
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = formatter.historyRange(entry),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatter.historyTotal(entry),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = formatter.historyBreakdown(entry),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = calculatedAt,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete calculation",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyHistory() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.extraLarge),
        ) {
            Icon(
                Icons.Outlined.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("No calculations yet", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Tap COUNT DAYS on Home to save a range here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
