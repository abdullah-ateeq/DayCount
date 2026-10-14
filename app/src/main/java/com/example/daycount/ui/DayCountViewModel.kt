package com.example.daycount.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.daycount.data.SharedPreferencesStore
import com.example.daycount.domain.DayCountCalculator
import com.example.daycount.domain.DayCountFormatter
import com.example.daycount.model.AppSettings
import com.example.daycount.model.CalculationState
import com.example.daycount.model.DateRange
import com.example.daycount.model.DayCountHistory
import com.example.daycount.model.DayCountResult
import com.example.daycount.model.DayCountUiState
import com.example.daycount.model.QuickRange
import com.example.daycount.model.ThemeMode
import com.example.daycount.repository.DayCountHistoryRepository
import com.example.daycount.repository.SettingsRepository
import com.example.daycount.util.DateRangePresets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.util.UUID

/**
 * Owns the selected dates, the current result and history. The result is recomputed every
 * time the dates change, so it always matches the visible range; COUNT DAYS is the explicit
 * action that records the range in history.
 */
class DayCountViewModel(
    private val historyRepository: DayCountHistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val today: () -> LocalDate = { LocalDate.now() },
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val formatterProvider: () -> DayCountFormatter = { DayCountFormatter() },
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings

    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<DayCountUiState> = _uiState.asStateFlow()

    init {
        recalculate()
    }

    private fun initialState(): DayCountUiState {
        val now = today()
        val (savedFirst, savedSecond) =
            if (settings.value.preserveLastDates) settingsRepository.loadLastDates() else null to null
        return DayCountUiState(
            firstDate = savedFirst ?: now,
            secondDate = savedSecond ?: now,
            history = historyRepository.history.value,
        )
    }

    // Date selection

    fun selectFirstDate(date: LocalDate) = setDates(date, _uiState.value.secondDate)

    fun selectSecondDate(date: LocalDate) = setDates(_uiState.value.firstDate, date)

    fun setFirstDateToToday() = selectFirstDate(today())

    fun setSecondDateToToday() = selectSecondDate(today())

    fun swapDates() {
        val state = _uiState.value
        setDates(state.secondDate, state.firstDate)
    }

    fun applyQuickRange(range: QuickRange) {
        val dates = DateRangePresets.quickRange(range, today())
        setDates(dates.first, dates.second)
    }

    fun shiftWeek(direction: Int) = shiftRange { DateRangePresets.shiftWeeks(it, direction.toLong()) }

    fun shiftMonth(direction: Int) = shiftRange { DateRangePresets.shiftMonths(it, direction.toLong()) }

    /** Start and end become today; the one-day result is shown. History is untouched. */
    fun reset() {
        val now = today()
        setDates(now, now)
    }

    fun loadFromHistory(entry: DayCountHistory) = setDates(entry.firstDate, entry.secondDate)

    private fun shiftRange(transform: (DateRange) -> DateRange) {
        val state = _uiState.value
        val now = today()
        val current = DateRange(state.firstDate ?: now, state.secondDate ?: now)
        val shifted = runCatching { transform(current) }.getOrElse {
            _uiState.update {
                it.copy(
                    errorMessage = MESSAGE_UNABLE,
                    calculationState = CalculationState.Error(MESSAGE_UNABLE),
                )
            }
            return
        }
        setDates(shifted.first, shifted.second)
    }

    private fun setDates(first: LocalDate?, second: LocalDate?) {
        _uiState.update {
            it.copy(
                firstDate = first,
                secondDate = second,
                result = null,
                isSavedToHistory = false,
                calculationState = CalculationState.Idle,
            )
        }
        settingsRepository.saveLastDates(first, second)
        recalculate()
    }

    // Calculation

    /**
     * Explicit COUNT DAYS. Recalculates and records the range in history. Repeated taps on an
     * unchanged range never create duplicate entries.
     *
     * @return true when a valid result is available.
     */
    fun countDays(): Boolean {
        if (_uiState.value.isCalculating) return false
        val result = recalculate() ?: return false
        val state = _uiState.value
        val first = state.firstDate ?: return false
        val second = state.secondDate ?: return false

        val history = if (state.isSavedToHistory) {
            historyRepository.history.value
        } else {
            historyRepository.save(
                DayCountHistory(
                    id = newId(),
                    firstDate = first,
                    secondDate = second,
                    totalDays = result.totalDays,
                    weekdays = result.weekdays,
                    weekendDays = result.weekendDays,
                    calculatedAt = clock(),
                )
            )
        }
        _uiState.update {
            it.copy(
                history = history,
                isSavedToHistory = true,
                countRequestId = it.countRequestId + 1,
            )
        }
        return true
    }

    private fun recalculate(): DayCountResult? {
        val state = _uiState.value
        val first = state.firstDate
        val second = state.secondDate
        if (first == null || second == null) {
            _uiState.update {
                it.copy(
                    result = null,
                    errorMessage = MESSAGE_SELECT_BOTH,
                    isEndBeforeStart = false,
                    calculationState = CalculationState.Error(MESSAGE_SELECT_BOTH),
                )
            }
            return null
        }

        _uiState.update { it.copy(isCalculating = true) }
        val result = try {
            DayCountCalculator.calculate(first, second)
        } catch (_: RuntimeException) {
            null
        }

        if (result == null) {
            _uiState.update {
                it.copy(
                    result = null,
                    errorMessage = MESSAGE_UNABLE,
                    isEndBeforeStart = false,
                    isCalculating = false,
                    calculationState = CalculationState.Error(MESSAGE_UNABLE),
                )
            }
            return null
        }

        val endBeforeStart = settings.value.preserveDateOrder && second.isBefore(first)
        _uiState.update {
            it.copy(
                result = result,
                errorMessage = if (endBeforeStart) MESSAGE_END_BEFORE_START else null,
                isEndBeforeStart = endBeforeStart,
                isCalculating = false,
                calculationState = CalculationState.Calculated,
            )
        }
        return result
    }

    // History

    fun deleteHistory(id: String) {
        val history = historyRepository.delete(id)
        _uiState.update { it.copy(history = history) }
    }

    fun clearHistory() {
        val history = historyRepository.clear()
        _uiState.update { it.copy(history = history, isSavedToHistory = false) }
    }

    // Copy and share

    fun copyText(): String? = _uiState.value.result?.let { formatterProvider().copyText(it) }

    fun shareText(): String? = _uiState.value.result?.let { formatterProvider().shareText(it) }

    // Settings

    fun setThemeMode(mode: ThemeMode) = settingsRepository.setThemeMode(mode)
    fun setDynamicColor(enabled: Boolean) = settingsRepository.setDynamicColor(enabled)
    fun setHapticFeedback(enabled: Boolean) = settingsRepository.setHapticFeedback(enabled)
    fun setAnimations(enabled: Boolean) = settingsRepository.setAnimations(enabled)
    fun setConfirmHistoryDelete(enabled: Boolean) = settingsRepository.setConfirmHistoryDelete(enabled)
    fun setShowPercentages(enabled: Boolean) = settingsRepository.setShowPercentages(enabled)
    fun setShowDayBreakdown(enabled: Boolean) = settingsRepository.setShowDayBreakdown(enabled)

    fun setPreserveDateOrder(enabled: Boolean) {
        settingsRepository.setPreserveDateOrder(enabled)
        recalculate()
    }

    fun setPreserveLastDates(enabled: Boolean) {
        settingsRepository.setPreserveLastDates(enabled)
        val state = _uiState.value
        settingsRepository.saveLastDates(state.firstDate, state.secondDate)
    }

    fun resetSettings() {
        settingsRepository.resetSettings()
        recalculate()
    }

    companion object {
        const val MESSAGE_SELECT_BOTH = "Please select both dates."
        const val MESSAGE_END_BEFORE_START = "End date is before start date."
        const val MESSAGE_UNABLE = "Unable to calculate the selected date range."

        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val store = SharedPreferencesStore.create(context)
                DayCountViewModel(
                    historyRepository = DayCountHistoryRepository(store),
                    settingsRepository = SettingsRepository(store),
                )
            }
        }
    }
}
