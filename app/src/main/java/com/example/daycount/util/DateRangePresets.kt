package com.example.daycount.util

import com.example.daycount.model.DateRange
import com.example.daycount.model.QuickRange
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Quick ranges and range shifting, all relative to a supplied "today". */
object DateRangePresets {

    fun quickRange(range: QuickRange, today: LocalDate): DateRange = when (range) {
        QuickRange.TODAY -> today(today)
        QuickRange.THIS_WEEK -> thisWeek(today)
        QuickRange.THIS_MONTH -> thisMonth(today)
        QuickRange.THIS_YEAR -> thisYear(today)
    }

    fun today(today: LocalDate): DateRange = DateRange(today, today)

    /** Monday → Sunday of the week containing [today]. */
    fun thisWeek(today: LocalDate): DateRange {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return DateRange(monday, monday.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)))
    }

    fun thisMonth(today: LocalDate): DateRange = DateRange(
        today.with(TemporalAdjusters.firstDayOfMonth()),
        today.with(TemporalAdjusters.lastDayOfMonth()),
    )

    fun thisYear(today: LocalDate): DateRange = DateRange(
        today.with(TemporalAdjusters.firstDayOfYear()),
        today.with(TemporalAdjusters.lastDayOfYear()),
    )

    /** Moves both dates by [weeks] weeks (negative moves backwards). */
    fun shiftWeeks(range: DateRange, weeks: Long): DateRange =
        DateRange(range.first.plusWeeks(weeks), range.second.plusWeeks(weeks))

    /**
     * Moves the range by [months] calendar months. A range that starts on the first day of a
     * month and ends on the last day of a month stays month-aligned (e.g. all of February →
     * all of March). Other ranges move each date by the same number of months, clamping to
     * the end of shorter months. The selected order of the two fields is kept.
     */
    fun shiftMonths(range: DateRange, months: Long): DateRange {
        val reversed = range.second.isBefore(range.first)
        val earlier = if (reversed) range.second else range.first
        val later = if (reversed) range.first else range.second

        val monthAligned = earlier.dayOfMonth == 1 && later.dayOfMonth == later.lengthOfMonth()
        val newEarlier: LocalDate
        val newLater: LocalDate
        if (monthAligned) {
            newEarlier = earlier.plusMonths(months).with(TemporalAdjusters.firstDayOfMonth())
            newLater = later.plusMonths(months).with(TemporalAdjusters.lastDayOfMonth())
        } else {
            newEarlier = earlier.plusMonths(months)
            newLater = later.plusMonths(months)
        }
        return if (reversed) DateRange(newLater, newEarlier) else DateRange(newEarlier, newLater)
    }
}
