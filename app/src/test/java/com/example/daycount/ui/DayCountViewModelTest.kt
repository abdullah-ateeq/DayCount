package com.example.daycount.ui

import com.example.daycount.FakeKeyValueStore
import com.example.daycount.domain.DayCountFormatter
import com.example.daycount.model.CalculationState
import com.example.daycount.model.QuickRange
import com.example.daycount.model.ThemeMode
import com.example.daycount.repository.DayCountHistoryRepository
import com.example.daycount.repository.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class DayCountViewModelTest {

    private val today = LocalDate.of(2026, 10, 14)
    private val monday = LocalDate.of(2026, 10, 5)
    private val sunday = LocalDate.of(2026, 10, 11)
    private val store = FakeKeyValueStore()
    private var idCounter = 0

    private fun createViewModel(currentDay: LocalDate = today) = DayCountViewModel(
        historyRepository = DayCountHistoryRepository(store),
        settingsRepository = SettingsRepository(store),
        today = { currentDay },
        clock = { 1_000L },
        newId = { "id-${idCounter++}" },
        formatterProvider = {
            DayCountFormatter(
                locale = Locale.UK,
                fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.UK),
                longDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK),
            )
        },
    )

    private val DayCountViewModel.state get() = uiState.value

    @Test
    fun startsWithTodayAndOneDayResult() {
        val viewModel = createViewModel()
        assertEquals(today, viewModel.state.firstDate)
        assertEquals(today, viewModel.state.secondDate)
        assertEquals(1L, viewModel.state.result?.totalDays)
        assertEquals(CalculationState.Calculated, viewModel.state.calculationState)
    }

    @Test
    fun selectingDates_updatesResultImmediately() {
        val viewModel = createViewModel()
        viewModel.selectFirstDate(monday)
        viewModel.selectSecondDate(sunday)
        val result = viewModel.state.result!!
        assertEquals(monday, result.earlierDate)
        assertEquals(sunday, result.laterDate)
        assertEquals(7L, result.totalDays)
        assertEquals(5L, result.weekdays)
        assertEquals(2L, result.weekendDays)
    }

    @Test
    fun resultAlwaysMatchesCurrentDates() {
        val viewModel = createViewModel()
        viewModel.selectFirstDate(monday)
        viewModel.selectSecondDate(sunday)
        viewModel.countDays()
        viewModel.selectSecondDate(monday.plusDays(1))
        val state = viewModel.state
        assertEquals(2L, state.result?.totalDays)
        assertEquals(state.secondDate, state.result?.laterDate)
        assertFalse("a changed range is not marked as saved", state.isSavedToHistory)
    }

    @Test
    fun swap_exchangesDatesAndKeepsCountCorrect() {
        val viewModel = createViewModel()
        viewModel.selectFirstDate(monday)
        viewModel.selectSecondDate(sunday)

        viewModel.swapDates()

        assertEquals(sunday, viewModel.state.firstDate)
        assertEquals(monday, viewModel.state.secondDate)
        val result = viewModel.state.result!!
        assertEquals(7L, result.totalDays)
        assertEquals(5L, result.weekdays)
        assertEquals(2L, result.weekendDays)
        assertTrue(result.wasReordered)
    }

    @Test
    fun reversedDates_withPreserveOrderOff_areReorderedWithoutError() {
        val viewModel = createViewModel()
        viewModel.selectFirstDate(sunday)
        viewModel.selectSecondDate(monday)
        val state = viewModel.state
        assertTrue(state.result!!.wasReordered)
        assertEquals(7L, state.result!!.totalDays)
        assertFalse(state.isEndBeforeStart)
        assertNull(state.errorMessage)
    }

    @Test
    fun reversedDates_withPreserveOrderOn_warnButStillCountAbsoluteRange() {
        val viewModel = createViewModel()
        viewModel.setPreserveDateOrder(true)
        viewModel.selectFirstDate(sunday)
        viewModel.selectSecondDate(monday)

        val state = viewModel.state
        assertTrue(state.isEndBeforeStart)
        assertEquals(DayCountViewModel.MESSAGE_END_BEFORE_START, state.errorMessage)
        assertEquals(7L, state.result?.totalDays)
        assertEquals(sunday, state.firstDate)

        viewModel.swapDates()
        assertFalse(viewModel.state.isEndBeforeStart)
        assertNull(viewModel.state.errorMessage)
    }

    @Test
    fun countDays_savesOneHistoryEntryPerCalculation() {
        val viewModel = createViewModel()
        viewModel.selectFirstDate(monday)
        viewModel.selectSecondDate(sunday)
        assertTrue(viewModel.countDays())
        assertEquals(1, viewModel.state.history.size)
        assertTrue(viewModel.state.isSavedToHistory)

        viewModel.selectSecondDate(sunday.plusDays(7))
        viewModel.countDays()
        assertEquals(2, viewModel.state.history.size)
        assertEquals(14L, viewModel.state.history.first().totalDays)
    }

    @Test
    fun countDays_eleventhCalculationDropsOldest() {
        val viewModel = createViewModel()
        viewModel.selectFirstDate(monday)
        for (length in 0L..10L) {
            viewModel.selectSecondDate(monday.plusDays(length))
            viewModel.countDays()
        }
        val history = viewModel.state.history
        assertEquals(10, history.size)
        assertEquals(11L, history.first().totalDays)
        assertTrue(history.none { it.totalDays == 1L })
    }

    // 50. Rapid calculation protection
    @Test
    fun rapidRepeatedCounts_doNotDuplicateHistory() {
        val viewModel = createViewModel()
        viewModel.selectFirstDate(monday)
        viewModel.selectSecondDate(sunday)
        repeat(20) { viewModel.countDays() }
        assertEquals(1, viewModel.state.history.size)
        assertEquals(1, DayCountHistoryRepository(store).history.value.size)
        assertFalse(viewModel.state.isCalculating)
    }

    @Test
    fun countDays_replaysRevealButKeepsSameResult() {
        val viewModel = createViewModel()
        val before = viewModel.state.countRequestId
        viewModel.countDays()
        viewModel.countDays()
        assertEquals(before + 2, viewModel.state.countRequestId)
        assertEquals(1L, viewModel.state.result?.totalDays)
    }

    // 47. Reset
    @Test
    fun reset_restoresTodayAndKeepsHistory() {
        val viewModel = createViewModel()
        viewModel.selectFirstDate(monday)
        viewModel.selectSecondDate(sunday)
        viewModel.countDays()

        viewModel.reset()

        val state = viewModel.state
        assertEquals(today, state.firstDate)
        assertEquals(today, state.secondDate)
        assertEquals(1L, state.result?.totalDays)
        assertTrue(state.result!!.isSingleDay)
        assertEquals(1, state.history.size)
    }

    @Test
    fun quickRanges_populateBothDates() {
        val viewModel = createViewModel()
        viewModel.applyQuickRange(QuickRange.THIS_WEEK)
        assertEquals(LocalDate.of(2026, 10, 12), viewModel.state.firstDate)
        assertEquals(LocalDate.of(2026, 10, 18), viewModel.state.secondDate)
        assertEquals(7L, viewModel.state.result?.totalDays)

        viewModel.applyQuickRange(QuickRange.THIS_MONTH)
        assertEquals(31L, viewModel.state.result?.totalDays)
        assertEquals(22L, viewModel.state.result?.weekdays)

        viewModel.applyQuickRange(QuickRange.THIS_YEAR)
        assertEquals(365L, viewModel.state.result?.totalDays)

        viewModel.applyQuickRange(QuickRange.TODAY)
        assertEquals(1L, viewModel.state.result?.totalDays)
    }

    @Test
    fun weekAndMonthShifts() {
        val viewModel = createViewModel()
        viewModel.selectFirstDate(monday)
        viewModel.selectSecondDate(sunday)

        viewModel.shiftWeek(-1)
        assertEquals(LocalDate.of(2026, 9, 28), viewModel.state.firstDate)
        assertEquals(LocalDate.of(2026, 10, 4), viewModel.state.secondDate)

        viewModel.shiftWeek(1)
        viewModel.shiftWeek(1)
        assertEquals(LocalDate.of(2026, 10, 12), viewModel.state.firstDate)

        viewModel.applyQuickRange(QuickRange.THIS_MONTH)
        viewModel.shiftMonth(1)
        assertEquals(LocalDate.of(2026, 11, 1), viewModel.state.firstDate)
        assertEquals(LocalDate.of(2026, 11, 30), viewModel.state.secondDate)
        viewModel.shiftMonth(-2)
        assertEquals(LocalDate.of(2026, 9, 30), viewModel.state.secondDate)
    }

    @Test
    fun shiftBeyondSupportedDates_showsFriendlyError() {
        val viewModel = createViewModel()
        viewModel.selectFirstDate(LocalDate.MAX)
        viewModel.selectSecondDate(LocalDate.MAX)
        viewModel.shiftWeek(1)
        assertEquals(DayCountViewModel.MESSAGE_UNABLE, viewModel.state.errorMessage)
        assertEquals(LocalDate.MAX, viewModel.state.firstDate)
    }

    @Test
    fun historyTap_loadsDatesAndRecalculates() {
        val viewModel = createViewModel()
        viewModel.selectFirstDate(monday)
        viewModel.selectSecondDate(sunday)
        viewModel.countDays()
        val entry = viewModel.state.history.first()
        viewModel.reset()

        viewModel.loadFromHistory(entry)

        assertEquals(monday, viewModel.state.firstDate)
        assertEquals(sunday, viewModel.state.secondDate)
        assertEquals(7L, viewModel.state.result?.totalDays)
        assertEquals(1, viewModel.state.history.size)
    }

    @Test
    fun deleteAndClearHistory() {
        val viewModel = createViewModel()
        viewModel.selectFirstDate(monday)
        listOf(0L, 1L, 2L).forEach {
            viewModel.selectSecondDate(monday.plusDays(it))
            viewModel.countDays()
        }
        viewModel.deleteHistory(viewModel.state.history[1].id)
        assertEquals(listOf(3L, 1L), viewModel.state.history.map { it.totalDays })

        viewModel.clearHistory()
        assertTrue(viewModel.state.history.isEmpty())
        assertTrue(DayCountHistoryRepository(store).history.value.isEmpty())
    }

    // Settings test: survives "close and reopen"
    @Test
    fun settings_persistAcrossViewModelInstances() {
        createViewModel().apply {
            setThemeMode(ThemeMode.DARK)
            setShowPercentages(false)
            setShowPercentages(true)
            setShowDayBreakdown(true)
        }
        val reopened = createViewModel().settings.value
        assertEquals(ThemeMode.DARK, reopened.themeMode)
        assertTrue(reopened.showPercentages)
        assertTrue(reopened.showDayBreakdown)
    }

    // 46. Last date persistence
    @Test
    fun lastDates_restoredWhenPreserveLastDatesOn() {
        createViewModel().apply {
            selectFirstDate(monday)
            selectSecondDate(sunday)
        }
        val reopened = createViewModel(currentDay = LocalDate.of(2026, 12, 1))
        assertEquals(monday, reopened.state.firstDate)
        assertEquals(sunday, reopened.state.secondDate)
        assertEquals(7L, reopened.state.result?.totalDays)
    }

    @Test
    fun lastDates_ignoredWhenPreserveLastDatesOff() {
        createViewModel().apply {
            setPreserveLastDates(false)
            selectFirstDate(monday)
            selectSecondDate(sunday)
        }
        val newToday = LocalDate.of(2026, 12, 1)
        val reopened = createViewModel(currentDay = newToday)
        assertEquals(newToday, reopened.state.firstDate)
        assertEquals(newToday, reopened.state.secondDate)
    }

    @Test
    fun resetSettings_clearsPreserveOrderWarning() {
        val viewModel = createViewModel()
        viewModel.setPreserveDateOrder(true)
        viewModel.selectFirstDate(sunday)
        viewModel.selectSecondDate(monday)
        assertTrue(viewModel.state.isEndBeforeStart)

        viewModel.resetSettings()

        assertFalse(viewModel.settings.value.preserveDateOrder)
        assertFalse(viewModel.state.isEndBeforeStart)
        assertNull(viewModel.state.errorMessage)
    }

    @Test
    fun copyAndShareText_describeCurrentResult() {
        val viewModel = createViewModel()
        viewModel.selectFirstDate(monday)
        viewModel.selectSecondDate(sunday)
        val copy = viewModel.copyText()
        val share = viewModel.shareText()
        assertNotNull(copy)
        assertTrue(copy!!.contains("From: Monday, 5 October 2026"))
        assertTrue(copy.contains("Total days: 7"))
        assertTrue(share!!.contains("5 October 2026 → 11 October 2026"))
        assertTrue(share.contains("2 weekend days"))
    }
}
