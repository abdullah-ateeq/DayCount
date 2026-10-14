package com.example.daycount.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/** Minimal key-value persistence used by the repositories, so they stay testable on the JVM. */
interface KeyValueStore {
    fun getString(key: String): String?
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putString(key: String, value: String?)
    fun putBoolean(key: String, value: Boolean)
    fun remove(vararg keys: String)
}

class SharedPreferencesStore(private val preferences: SharedPreferences) : KeyValueStore {

    override fun getString(key: String): String? = preferences.getString(key, null)

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        preferences.getBoolean(key, defaultValue)

    override fun putString(key: String, value: String?) {
        preferences.edit {
            if (value == null) remove(key) else putString(key, value)
        }
    }

    override fun putBoolean(key: String, value: Boolean) {
        preferences.edit { putBoolean(key, value) }
    }

    override fun remove(vararg keys: String) {
        preferences.edit { keys.forEach(::remove) }
    }

    companion object {
        private const val PREFERENCES_NAME = "daycount_preferences"

        fun create(context: Context): SharedPreferencesStore = SharedPreferencesStore(
            context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        )
    }
}
