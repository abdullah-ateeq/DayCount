package com.example.daycount.model

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Result of an inclusive date-range count. Both [earlierDate] and [laterDate] are counted.
 */
data class DayCountResult(
    val earlierDate: LocalDate,
    val laterDate: LocalDate,
    val totalDays: Long,
    val weekdays: Long,
    val weekendDays: Long,
    val startDayOfWeek: DayOfWeek,
    val endDayOfWeek: DayOfWeek,
    val wasReordered: Boolean,
    val isSingleDay: Boolean,
    val includesFullWeek: Boolean,
    val dayOfWeekCounts: Map<DayOfWeek, Long>,
) {
    /** Number of complete seven-day blocks contained in the range. */
    val completeWeeks: Long get() = totalDays / 7

    /** Day-of-week counts ordered Monday → Sunday. */
    val dayOfWeekCountList: List<DayOfWeekCount>
        get() = DayOfWeek.entries.map { DayOfWeekCount(it, dayOfWeekCounts[it] ?: 0L) }
}

data class DayOfWeekCount(
    val dayOfWeek: DayOfWeek,
    val count: Long,
)

/** Whole-number weekday/weekend percentages that always add up to 100 for a non-empty range. */
data class Percentages(
    val weekdays: Int,
    val weekends: Int,
)
