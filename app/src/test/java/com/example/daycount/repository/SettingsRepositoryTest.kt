package com.example.daycount.repository

import com.example.daycount.FakeKeyValueStore
import com.example.daycount.model.AppSettings
import com.example.daycount.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SettingsRepositoryTest {

    private val store = FakeKeyValueStore()

    @Test
    fun defaults() {
        val settings = SettingsRepository(store).settings.value
        assertEquals(AppSettings(), settings)
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertEquals(true, settings.dynamicColor)
        assertEquals(true, settings.hapticFeedback)
        assertEquals(true, settings.animations)
        assertEquals(true, settings.confirmHistoryDelete)
        assertEquals(false, settings.preserveDateOrder)
        assertEquals(true, settings.preserveLastDates)
        assertEquals(true, settings.showPercentages)
        assertEquals(false, settings.showDayBreakdown)
    }

    // 45. Settings persistence
    @Test
    fun everySetting_survivesRestart() {
        SettingsRepository(store).apply {
            setThemeMode(ThemeMode.DARK)
            setDynamicColor(false)
            setHapticFeedback(false)
            setAnimations(false)
            setConfirmHistoryDelete(false)
            setPreserveDateOrder(true)
            setPreserveLastDates(false)
            setShowPercentages(false)
            setShowDayBreakdown(true)
        }

        val expected = AppSettings(
            themeMode = ThemeMode.DARK,
            dynamicColor = false,
            hapticFeedback = false,
            animations = false,
            confirmHistoryDelete = false,
            preserveDateOrder = true,
            preserveLastDates = false,
            showPercentages = false,
            showDayBreakdown = true,
        )
        assertEquals(expected, SettingsRepository(store).settings.value)
    }

    @Test
    fun resetSettings_restoresDefaultsButKeepsDates() {
        val repository = SettingsRepository(store)
        repository.setThemeMode(ThemeMode.LIGHT)
        repository.setShowDayBreakdown(true)
        repository.saveLastDates(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 11))

        repository.resetSettings()

        assertEquals(AppSettings(), repository.settings.value)
        assertEquals(AppSettings(), SettingsRepository(store).settings.value)
        assertEquals(LocalDate.of(2026, 10, 5) to LocalDate.of(2026, 10, 11), repository.loadLastDates())
    }

    // 46. Last date persistence
    @Test
    fun lastDates_storedAsIsoStrings() {
        SettingsRepository(store).saveLastDates(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 11))
        assertEquals("2026-10-05", store.getString("last_first_date"))
        assertEquals("2026-10-11", store.getString("last_second_date"))
        assertEquals(
            LocalDate.of(2026, 10, 5) to LocalDate.of(2026, 10, 11),
            SettingsRepository(store).loadLastDates(),
        )
    }

    @Test
    fun corruptValues_fallBackToDefaults() {
        store.putString("theme_mode", "PURPLE")
        store.putString("last_first_date", "not-a-date")
        val repository = SettingsRepository(store)
        assertEquals(ThemeMode.SYSTEM, repository.settings.value.themeMode)
        assertNull(repository.loadLastDates().first)
    }
}
