package com.example.daycount.repository

import com.example.daycount.data.KeyValueStore
import com.example.daycount.model.AppSettings
import com.example.daycount.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class SettingsRepository(private val store: KeyValueStore) {

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }
    fun setDynamicColor(enabled: Boolean) = update { it.copy(dynamicColor = enabled) }
    fun setHapticFeedback(enabled: Boolean) = update { it.copy(hapticFeedback = enabled) }
    fun setAnimations(enabled: Boolean) = update { it.copy(animations = enabled) }
    fun setConfirmHistoryDelete(enabled: Boolean) = update { it.copy(confirmHistoryDelete = enabled) }
    fun setPreserveDateOrder(enabled: Boolean) = update { it.copy(preserveDateOrder = enabled) }
    fun setPreserveLastDates(enabled: Boolean) = update { it.copy(preserveLastDates = enabled) }
    fun setShowPercentages(enabled: Boolean) = update { it.copy(showPercentages = enabled) }
    fun setShowDayBreakdown(enabled: Boolean) = update { it.copy(showDayBreakdown = enabled) }

    /** Restores every setting to its default. Saved dates and history are untouched. */
    fun resetSettings() = update { AppSettings() }

    @Synchronized
    fun update(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_settings.value)
        write(updated)
        _settings.value = updated
    }

    fun saveLastDates(firstDate: LocalDate?, secondDate: LocalDate?) {
        store.putString(KEY_LAST_FIRST_DATE, firstDate?.toString())
        store.putString(KEY_LAST_SECOND_DATE, secondDate?.toString())
    }

    fun loadLastDates(): Pair<LocalDate?, LocalDate?> =
        parseDate(store.getString(KEY_LAST_FIRST_DATE)) to
            parseDate(store.getString(KEY_LAST_SECOND_DATE))

    private fun read(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            themeMode = store.getString(KEY_THEME_MODE)
                ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                ?: defaults.themeMode,
            dynamicColor = store.getBoolean(KEY_DYNAMIC_COLOR, defaults.dynamicColor),
            hapticFeedback = store.getBoolean(KEY_HAPTICS, defaults.hapticFeedback),
            animations = store.getBoolean(KEY_ANIMATIONS, defaults.animations),
            confirmHistoryDelete = store.getBoolean(KEY_CONFIRM_DELETE, defaults.confirmHistoryDelete),
            preserveDateOrder = store.getBoolean(KEY_PRESERVE_ORDER, defaults.preserveDateOrder),
            preserveLastDates = store.getBoolean(KEY_PRESERVE_LAST_DATES, defaults.preserveLastDates),
            showPercentages = store.getBoolean(KEY_SHOW_PERCENTAGES, defaults.showPercentages),
            showDayBreakdown = store.getBoolean(KEY_SHOW_DAY_BREAKDOWN, defaults.showDayBreakdown),
        )
    }

    private fun write(settings: AppSettings) {
        store.putString(KEY_THEME_MODE, settings.themeMode.name)
        store.putBoolean(KEY_DYNAMIC_COLOR, settings.dynamicColor)
        store.putBoolean(KEY_HAPTICS, settings.hapticFeedback)
        store.putBoolean(KEY_ANIMATIONS, settings.animations)
        store.putBoolean(KEY_CONFIRM_DELETE, settings.confirmHistoryDelete)
        store.putBoolean(KEY_PRESERVE_ORDER, settings.preserveDateOrder)
        store.putBoolean(KEY_PRESERVE_LAST_DATES, settings.preserveLastDates)
        store.putBoolean(KEY_SHOW_PERCENTAGES, settings.showPercentages)
        store.putBoolean(KEY_SHOW_DAY_BREAKDOWN, settings.showDayBreakdown)
    }

    private fun parseDate(value: String?): LocalDate? =
        value?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
        const val KEY_HAPTICS = "haptic_feedback"
        const val KEY_ANIMATIONS = "animations"
        const val KEY_CONFIRM_DELETE = "confirm_history_delete"
        const val KEY_PRESERVE_ORDER = "preserve_date_order"
        const val KEY_PRESERVE_LAST_DATES = "preserve_last_dates"
        const val KEY_SHOW_PERCENTAGES = "show_percentages"
        const val KEY_SHOW_DAY_BREAKDOWN = "show_day_breakdown"
        const val KEY_LAST_FIRST_DATE = "last_first_date"
        const val KEY_LAST_SECOND_DATE = "last_second_date"
    }
}
