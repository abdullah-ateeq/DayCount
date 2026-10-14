package com.example.daycount.data

import com.example.daycount.model.DayCountHistory
import java.time.LocalDate

/**
 * Stores history as one entry per line with pipe-separated fields:
 * `id|firstDate|secondDate|totalDays|weekdays|weekendDays|calculatedAt`.
 * Dates are ISO-8601 (`2026-10-05`); counts and timestamps are numeric.
 */
object HistorySerializer {

    private const val FIELD_SEPARATOR = "|"
    private const val ENTRY_SEPARATOR = "\n"
    private const val FIELD_COUNT = 7

    fun encode(entries: List<DayCountHistory>): String =
        entries.joinToString(ENTRY_SEPARATOR) { entry ->
            listOf(
                entry.id,
                entry.firstDate.toString(),
                entry.secondDate.toString(),
                entry.totalDays.toString(),
                entry.weekdays.toString(),
                entry.weekendDays.toString(),
                entry.calculatedAt.toString(),
            ).joinToString(FIELD_SEPARATOR)
        }

    /** Decodes stored history, skipping any malformed lines instead of failing. */
    fun decode(raw: String?): List<DayCountHistory> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(ENTRY_SEPARATOR)
            .filter { it.isNotBlank() }
            .mapNotNull(::decodeEntry)
    }

    private fun decodeEntry(line: String): DayCountHistory? = runCatching {
        val fields = line.split(FIELD_SEPARATOR)
        require(fields.size == FIELD_COUNT)
        DayCountHistory(
            id = fields[0].also { require(it.isNotBlank()) },
            firstDate = LocalDate.parse(fields[1]),
            secondDate = LocalDate.parse(fields[2]),
            totalDays = fields[3].toLong(),
            weekdays = fields[4].toLong(),
            weekendDays = fields[5].toLong(),
            calculatedAt = fields[6].toLong(),
        )
    }.getOrNull()
}
