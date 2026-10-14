package com.example.daycount.model

import java.time.LocalDate

/**
 * A saved calculation. Dates are kept in the order the user selected them.
 */
data class DayCountHistory(
    val id: String,
    val firstDate: LocalDate,
    val secondDate: LocalDate,
    val totalDays: Long,
    val weekdays: Long,
    val weekendDays: Long,
    val calculatedAt: Long,
)
