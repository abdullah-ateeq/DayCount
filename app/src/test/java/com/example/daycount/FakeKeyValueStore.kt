package com.example.daycount

import com.example.daycount.data.KeyValueStore

/** In-memory stand-in for SharedPreferences. Sharing one instance simulates an app restart. */
class FakeKeyValueStore : KeyValueStore {
    val values = mutableMapOf<String, Any>()

    override fun getString(key: String): String? = values[key] as? String

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        values[key] as? Boolean ?: defaultValue

    override fun putString(key: String, value: String?) {
        if (value == null) values.remove(key) else values[key] = value
    }

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
    }

    override fun remove(vararg keys: String) {
        keys.forEach { values.remove(it) }
    }
}
