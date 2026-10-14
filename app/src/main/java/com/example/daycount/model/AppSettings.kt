package com.example.daycount.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val hapticFeedback: Boolean = true,
    val animations: Boolean = true,
    val confirmHistoryDelete: Boolean = true,
    val preserveDateOrder: Boolean = false,
    val preserveLastDates: Boolean = true,
    val showPercentages: Boolean = true,
    val showDayBreakdown: Boolean = false,
)
