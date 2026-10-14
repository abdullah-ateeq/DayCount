package com.example.daycount.model

import java.time.LocalDate

/** A pair of selected dates. [first] is the start field, [second] is the end field. */
data class DateRange(
    val first: LocalDate,
    val second: LocalDate,
)

enum class QuickRange {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR,
}
