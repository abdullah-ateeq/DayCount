package com.example.daycount.domain

import com.example.daycount.model.DayCountHistory
import com.example.daycount.model.DayCountResult
import com.example.daycount.model.Percentages
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/**
 * Turns calculation results into display, accessibility, copy and share text.
 * Dates follow the given locale; date formatters can be overridden for deterministic tests.
 */
class DayCountFormatter(
    private val locale: Locale = Locale.getDefault(),
    private val fullDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale),
    private val longDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale),
    private val shortDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale),
) {

    fun fullDate(date: LocalDate): String = fullDateFormatter.format(date)

    fun longDate(date: LocalDate): String = longDateFormatter.format(date)

    fun shortDate(date: LocalDate): String = shortDateFormatter.format(date)

    fun dayName(date: LocalDate): String = dayName(date.dayOfWeek)

    fun dayName(dayOfWeek: DayOfWeek): String =
        dayOfWeek.getDisplayName(TextStyle.FULL, locale)

    fun shortDayName(dayOfWeek: DayOfWeek): String =
        dayOfWeek.getDisplayName(TextStyle.SHORT, locale)

    fun count(value: Long): String = NumberFormat.getIntegerInstance(locale).format(value)

    // Result labels

    fun totalDays(value: Long): String = "${count(value)} ${plural(value, "total day", "total days")}"

    fun weekdays(value: Long): String = "${count(value)} ${plural(value, "weekday", "weekdays")}"

    fun weekendDays(value: Long): String =
        "${count(value)} ${plural(value, "weekend day", "weekend days")}"

    /** "5 WEEKDAYS" or "NO WEEKDAYS". */
    fun weekdaySummary(value: Long): String =
        if (value > 0) weekdays(value).uppercase(locale) else "NO WEEKDAYS"

    /** "2 WEEKEND DAYS" or "NO WEEKEND DAYS". */
    fun weekendSummary(value: Long): String =
        if (value > 0) weekendDays(value).uppercase(locale) else "NO WEEKEND DAYS"

    fun weekdayLabel(value: Long): String = when {
        value == 0L -> "NO WEEKDAYS"
        value == 1L -> "WEEKDAY"
        else -> "WEEKDAYS"
    }

    fun weekendLabel(value: Long): String = when {
        value == 0L -> "NO WEEKEND DAYS"
        value == 1L -> "WEEKEND DAY"
        else -> "WEEKEND DAYS"
    }

    fun totalLabel(value: Long): String = if (value == 1L) "TOTAL DAY" else "TOTAL DAYS"

    /** "7 days inclusive". */
    fun inclusiveRange(totalDays: Long): String =
        "${count(totalDays)} ${plural(totalDays, "day", "days")} inclusive"

    fun weekStructure(result: DayCountResult): String? {
        if (result.isSingleDay) return "Single-day range"
        if (!result.includesFullWeek) return null
        val weeks = result.completeWeeks
        val remaining = result.totalDays % 7
        val weeksText = "${count(weeks)} complete ${plural(weeks, "week", "weeks")}"
        return if (remaining == 0L) weeksText
        else "$weeksText + ${count(remaining)} ${plural(remaining, "day", "days")}"
    }

    // Percentages

    fun percentages(result: DayCountResult): Percentages = DayCountCalculator.percentages(result)

    fun weekdayPercent(percentages: Percentages): String = "${percentages.weekdays}% weekdays"

    fun weekendPercent(percentages: Percentages): String = "${percentages.weekends}% weekends"

    // History

    fun historyRange(entry: DayCountHistory): String =
        "${shortDate(entry.firstDate)} → ${shortDate(entry.secondDate)}"

    fun historyTotal(entry: DayCountHistory): String =
        "${count(entry.totalDays)} ${plural(entry.totalDays, "day", "days")}"

    fun historyBreakdown(entry: DayCountHistory): String =
        "${weekdays(entry.weekdays)} · ${count(entry.weekendDays)} " +
            plural(entry.weekendDays, "weekend", "weekends")

    fun historyAccessibility(entry: DayCountHistory): String =
        "${fullDate(entry.firstDate)} to ${fullDate(entry.secondDate)}. " +
            "${totalDays(entry.totalDays)}, ${weekdays(entry.weekdays)}, " +
            "${weekendDays(entry.weekendDays)}."

    // Accessibility

    fun resultAccessibility(result: DayCountResult): String =
        "${totalDays(result.totalDays)}, ${weekdays(result.weekdays)}, " +
            "${weekendDays(result.weekendDays)}."

    // Copy and share

    fun copyText(result: DayCountResult): String = buildString {
        appendLine("DayCount Result")
        appendLine()
        appendLine("From: ${fullDate(result.earlierDate)}")
        appendLine("To: ${fullDate(result.laterDate)}")
        appendLine()
        appendLine("Total days: ${count(result.totalDays)}")
        appendLine("Weekdays: ${count(result.weekdays)}")
        appendLine("Weekend days: ${count(result.weekendDays)}")
        appendLine()
        append("Range: Inclusive")
    }

    fun shareText(result: DayCountResult): String = buildString {
        appendLine("DayCount")
        appendLine()
        appendLine("${longDate(result.earlierDate)} → ${longDate(result.laterDate)}")
        appendLine()
        appendLine(totalDays(result.totalDays))
        appendLine(weekdays(result.weekdays))
        appendLine(weekendDays(result.weekendDays))
        appendLine()
        append("Inclusive date range")
    }

    private fun plural(value: Long, singular: String, plural: String): String =
        if (value == 1L) singular else plural
}
