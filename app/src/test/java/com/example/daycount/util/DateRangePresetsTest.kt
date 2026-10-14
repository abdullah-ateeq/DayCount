package com.example.daycount.util

import com.example.daycount.model.DateRange
import com.example.daycount.model.QuickRange
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateRangePresetsTest {

    private val today = LocalDate.of(2026, 10, 14) // Wednesday

    private fun range(a: LocalDate, b: LocalDate) = DateRange(a, b)

    // 32. Quick Today range
    @Test
    fun today() {
        assertEquals(range(today, today), DateRangePresets.quickRange(QuickRange.TODAY, today))
    }

    // 33. This Week
    @Test
    fun thisWeek_isMondayToSunday() {
        val expected = range(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 18))
        assertEquals(expected, DateRangePresets.quickRange(QuickRange.THIS_WEEK, today))
        // Sunday and Monday belong to the same Monday-based week.
        assertEquals(expected, DateRangePresets.thisWeek(LocalDate.of(2026, 10, 18)))
        assertEquals(expected, DateRangePresets.thisWeek(LocalDate.of(2026, 10, 12)))
    }

    @Test
    fun thisWeek_acrossYearBoundary() {
        assertEquals(
            range(LocalDate.of(2026, 12, 28), LocalDate.of(2027, 1, 3)),
            DateRangePresets.thisWeek(LocalDate.of(2027, 1, 1)),
        )
    }

    // 34. This Month
    @Test
    fun thisMonth() {
        assertEquals(
            range(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)),
            DateRangePresets.quickRange(QuickRange.THIS_MONTH, today),
        )
        assertEquals(
            range(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29)),
            DateRangePresets.thisMonth(LocalDate.of(2024, 2, 10)),
        )
    }

    // 35. This Year
    @Test
    fun thisYear() {
        assertEquals(
            range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)),
            DateRangePresets.quickRange(QuickRange.THIS_YEAR, today),
        )
    }

    // 36. Previous Week
    @Test
    fun previousWeek() {
        val shifted = DateRangePresets.shiftWeeks(range(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 11)), -1)
        assertEquals(range(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 4)), shifted)
    }

    // 37. Next Week
    @Test
    fun nextWeek() {
        val shifted = DateRangePresets.shiftWeeks(range(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 11)), 1)
        assertEquals(range(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 18)), shifted)
    }

    // 38. Previous Month
    @Test
    fun previousMonth_keepsWholeMonthRangesAligned() {
        val october = range(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))
        assertEquals(
            range(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)),
            DateRangePresets.shiftMonths(october, -1),
        )
        val march = range(LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31))
        assertEquals(
            range(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29)),
            DateRangePresets.shiftMonths(march, -1),
        )
    }

    @Test
    fun previousMonth_partialRangeMovesEachDate() {
        val partial = range(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 11))
        assertEquals(
            range(LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 11)),
            DateRangePresets.shiftMonths(partial, -1),
        )
    }

    // 39. Next Month
    @Test
    fun nextMonth_keepsWholeMonthRangesAligned() {
        val september = range(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))
        assertEquals(
            range(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)),
            DateRangePresets.shiftMonths(september, 1),
        )
        val january = range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))
        assertEquals(
            range(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)),
            DateRangePresets.shiftMonths(january, 1),
        )
    }

    @Test
    fun nextMonth_clampsToShorterMonth() {
        val partial = range(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 30))
        assertEquals(
            range(LocalDate.of(2026, 2, 15), LocalDate.of(2026, 2, 28)),
            DateRangePresets.shiftMonths(partial, 1),
        )
    }

    @Test
    fun nextMonth_crossesYearBoundary() {
        val december = range(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31))
        assertEquals(
            range(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 31)),
            DateRangePresets.shiftMonths(december, 1),
        )
    }

    @Test
    fun shiftMonths_keepsSelectedOrder() {
        val reversed = range(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 1))
        assertEquals(
            range(LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 1)),
            DateRangePresets.shiftMonths(reversed, 1),
        )
    }

    @Test
    fun pickerConversion_roundTrips() {
        listOf(LocalDate.of(2026, 10, 5), LocalDate.of(2024, 2, 29), LocalDate.of(1900, 1, 1)).forEach {
            assertEquals(it, it.toPickerMillis().pickerMillisToLocalDate())
        }
    }
}
