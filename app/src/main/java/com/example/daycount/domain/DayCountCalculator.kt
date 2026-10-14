package com.example.daycount.domain

import com.example.daycount.model.DayCountResult
import com.example.daycount.model.Percentages
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

/**
 * Inclusive date-range counter.
 *
 * Weekdays are always Monday–Friday and weekend days are always Saturday–Sunday,
 * independent of locale. The count runs in constant time: complete seven-day blocks
 * contribute 5 weekdays and 2 weekend days each, and only the 0–6 remaining days are
 * inspected individually.
 */
object DayCountCalculator {

    private const val DAYS_PER_WEEK = 7L
    private const val WEEKDAYS_PER_WEEK = 5L
    private const val WEEKEND_DAYS_PER_WEEK = 2L

    private val WEEKEND: Set<DayOfWeek> = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    fun isWeekend(dayOfWeek: DayOfWeek): Boolean = dayOfWeek in WEEKEND

    fun isWeekday(dayOfWeek: DayOfWeek): Boolean = !isWeekend(dayOfWeek)

    /** Returns the two dates as (earlier, later). */
    fun normalize(firstDate: LocalDate, secondDate: LocalDate): Pair<LocalDate, LocalDate> =
        if (secondDate.isBefore(firstDate)) secondDate to firstDate else firstDate to secondDate

    /** Inclusive day count: the same date counts as 1. Order of arguments does not matter. */
    fun inclusiveTotalDays(firstDate: LocalDate, secondDate: LocalDate): Long {
        val (earlier, later) = normalize(firstDate, secondDate)
        return ChronoUnit.DAYS.between(earlier, later) + 1
    }

    fun calculate(firstDate: LocalDate, secondDate: LocalDate): DayCountResult {
        val (earlier, later) = normalize(firstDate, secondDate)
        val totalDays = ChronoUnit.DAYS.between(earlier, later) + 1
        val startDayOfWeek = earlier.dayOfWeek

        val completeWeeks = totalDays / DAYS_PER_WEEK
        val remainingDays = (totalDays % DAYS_PER_WEEK).toInt()

        var weekdays = completeWeeks * WEEKDAYS_PER_WEEK
        var weekendDays = completeWeeks * WEEKEND_DAYS_PER_WEEK
        val dayOfWeekCounts = DayOfWeek.entries.associateWithTo(LinkedHashMap()) { completeWeeks }

        // The leftover days start on the same weekday as the range itself.
        for (offset in 0 until remainingDays) {
            val day = startDayOfWeek.plus(offset.toLong())
            if (isWeekend(day)) weekendDays++ else weekdays++
            dayOfWeekCounts[day] = dayOfWeekCounts.getValue(day) + 1
        }

        check(weekdays + weekendDays == totalDays) {
            "Weekday/weekend split does not match total days"
        }
        check(dayOfWeekCounts.values.sum() == totalDays) {
            "Day-of-week distribution does not match total days"
        }

        return DayCountResult(
            earlierDate = earlier,
            laterDate = later,
            totalDays = totalDays,
            weekdays = weekdays,
            weekendDays = weekendDays,
            startDayOfWeek = startDayOfWeek,
            endDayOfWeek = later.dayOfWeek,
            wasReordered = secondDate.isBefore(firstDate),
            isSingleDay = totalDays == 1L,
            includesFullWeek = totalDays >= DAYS_PER_WEEK,
            dayOfWeekCounts = dayOfWeekCounts,
        )
    }

    /**
     * Whole-number percentages that add up to 100. A category that is present is never
     * shown as 0%, and a category that is absent is never shown as 100% of its counterpart.
     */
    fun percentages(result: DayCountResult): Percentages {
        if (result.totalDays <= 0) return Percentages(0, 0)
        var weekdayPercent = (result.weekdays * 100.0 / result.totalDays).roundToLong().toInt()
        if (result.weekdays > 0 && weekdayPercent == 0) weekdayPercent = 1
        if (result.weekendDays > 0 && weekdayPercent == 100) weekdayPercent = 99
        return Percentages(weekdays = weekdayPercent, weekends = 100 - weekdayPercent)
    }
}
