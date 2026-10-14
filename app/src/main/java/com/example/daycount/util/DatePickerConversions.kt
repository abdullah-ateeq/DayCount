package com.example.daycount.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** The Material 3 date picker works in UTC midnight milliseconds. */
fun LocalDate.toPickerMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun Long.pickerMillisToLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
