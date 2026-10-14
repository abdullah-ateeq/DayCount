package com.example.daycount.domain

import com.example.daycount.model.DayCountHistory
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class DayCountFormatterTest {

    private val locale = Locale.UK
    private val formatter = DayCountFormatter(
        locale = locale,
        fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale),
        longDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", locale),
        shortDateFormatter = DateTimeFormatter.ofPattern("d MMM", locale),
    )

    private val week = DayCountCalculator.calculate(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 11))

    // 48. Copy formatting
    @Test
    fun copyText_matchesSpecifiedLayout() {
        val expected = """
            DayCount Result

            From: Monday, 5 October 2026
            To: Sunday, 11 October 2026

            Total days: 7
            Weekdays: 5
            Weekend days: 2

            Range: Inclusive
        """.trimIndent()
        assertEquals(expected, formatter.copyText(week))
    }

    @Test
    fun copyText_usesNormalizedOrderForReversedDates() {
        val reversed = DayCountCalculator.calculate(LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 5))
        assertEquals(formatter.copyText(week), formatter.copyText(reversed))
    }

    // 49. Share formatting
    @Test
    fun shareText_matchesSpecifiedLayout() {
        val expected = """
            DayCount

            5 October 2026 → 11 October 2026

            7 total days
            5 weekdays
            2 weekend days

            Inclusive date range
        """.trimIndent()
        assertEquals(expected, formatter.shareText(week))
    }

    @Test
    fun shareText_usesSingularForOneDay() {
        val single = DayCountCalculator.calculate(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 5))
        val text = formatter.shareText(single)
        assertEquals(true, text.contains("1 total day\n"))
        assertEquals(true, text.contains("1 weekday\n"))
        assertEquals(true, text.contains("0 weekend days\n"))
    }

    @Test
    fun largeNumbers_useGrouping() {
        val big = DayCountCalculator.calculate(LocalDate.of(2000, 1, 1), LocalDate.of(2399, 12, 31))
        assertEquals("Total days: 146,097", formatter.copyText(big).lines().first { it.startsWith("Total") })
    }

    @Test
    fun summaries_showNoWhenZero() {
        assertEquals("5 WEEKDAYS", formatter.weekdaySummary(5))
        assertEquals("NO WEEKDAYS", formatter.weekdaySummary(0))
        assertEquals("2 WEEKEND DAYS", formatter.weekendSummary(2))
        assertEquals("NO WEEKEND DAYS", formatter.weekendSummary(0))
        assertEquals("1 WEEKEND DAY", formatter.weekendSummary(1))
    }

    @Test
    fun resultLabels() {
        assertEquals("TOTAL DAYS", formatter.totalLabel(7))
        assertEquals("TOTAL DAY", formatter.totalLabel(1))
        assertEquals("7 days inclusive", formatter.inclusiveRange(7))
        assertEquals("1 day inclusive", formatter.inclusiveRange(1))
    }

    @Test
    fun percentLabels() {
        val percentages = formatter.percentages(week)
        assertEquals("71% weekdays", formatter.weekdayPercent(percentages))
        assertEquals("29% weekends", formatter.weekendPercent(percentages))
    }

    @Test
    fun accessibilityDescription() {
        assertEquals("7 total days, 5 weekdays, 2 weekend days.", formatter.resultAccessibility(week))
    }

    @Test
    fun weekStructure() {
        assertEquals("1 complete week", formatter.weekStructure(week))
        val tenDays = DayCountCalculator.calculate(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 14))
        assertEquals("1 complete week + 3 days", formatter.weekStructure(tenDays))
        val single = DayCountCalculator.calculate(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 5))
        assertEquals("Single-day range", formatter.weekStructure(single))
    }

    @Test
    fun historyItemFormatting() {
        val entry = DayCountHistory(
            id = "a",
            firstDate = LocalDate.of(2026, 10, 5),
            secondDate = LocalDate.of(2026, 10, 11),
            totalDays = 7,
            weekdays = 5,
            weekendDays = 2,
            calculatedAt = 0,
        )
        assertEquals("5 Oct → 11 Oct", formatter.historyRange(entry))
        assertEquals("7 days", formatter.historyTotal(entry))
        assertEquals("5 weekdays · 2 weekends", formatter.historyBreakdown(entry))
    }

    @Test
    fun dayNames() {
        assertEquals("Monday", formatter.dayName(LocalDate.of(2026, 10, 5)))
        assertEquals("Monday, 5 October 2026", formatter.fullDate(LocalDate.of(2026, 10, 5)))
    }
}
