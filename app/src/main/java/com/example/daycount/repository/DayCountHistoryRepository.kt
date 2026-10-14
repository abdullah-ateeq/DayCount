package com.example.daycount.repository

import com.example.daycount.data.HistorySerializer
import com.example.daycount.data.KeyValueStore
import com.example.daycount.model.DayCountHistory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Recent calculations, newest first, capped at [MAX_ENTRIES]. */
class DayCountHistoryRepository(private val store: KeyValueStore) {

    private val _history = MutableStateFlow(load())
    val history: StateFlow<List<DayCountHistory>> = _history.asStateFlow()

    fun load(): List<DayCountHistory> =
        HistorySerializer.decode(store.getString(KEY_HISTORY)).take(MAX_ENTRIES)

    /**
     * Adds [entry] as the newest item. An existing entry for the same date pair is replaced
     * rather than duplicated, and the oldest entries are dropped beyond [MAX_ENTRIES].
     */
    @Synchronized
    fun save(entry: DayCountHistory): List<DayCountHistory> {
        val remaining = _history.value.filterNot {
            it.id == entry.id ||
                (it.firstDate == entry.firstDate && it.secondDate == entry.secondDate)
        }
        return persist((listOf(entry) + remaining).take(MAX_ENTRIES))
    }

    @Synchronized
    fun delete(id: String): List<DayCountHistory> =
        persist(_history.value.filterNot { it.id == id })

    @Synchronized
    fun clear(): List<DayCountHistory> {
        store.remove(KEY_HISTORY)
        _history.value = emptyList()
        return emptyList()
    }

    private fun persist(entries: List<DayCountHistory>): List<DayCountHistory> {
        store.putString(KEY_HISTORY, HistorySerializer.encode(entries))
        _history.value = entries
        return entries
    }

    companion object {
        const val MAX_ENTRIES = 10
        private const val KEY_HISTORY = "history_entries"
    }
}
