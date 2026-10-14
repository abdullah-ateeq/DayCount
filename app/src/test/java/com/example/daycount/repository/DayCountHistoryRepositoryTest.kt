package com.example.daycount.repository

import com.example.daycount.FakeKeyValueStore
import com.example.daycount.data.HistorySerializer
import com.example.daycount.model.DayCountHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DayCountHistoryRepositoryTest {

    private val store = FakeKeyValueStore()
    private val repository = DayCountHistoryRepository(store)

    private fun entry(index: Int, first: LocalDate = LocalDate.of(2026, 1, 1).plusDays(index.toLong())) =
        DayCountHistory(
            id = "id-$index",
            firstDate = first,
            secondDate = first.plusDays(6),
            totalDays = 7,
            weekdays = 5,
            weekendDays = 2,
            calculatedAt = 1_000L + index,
        )

    // 40. History save
    @Test
    fun save_addsNewestFirst() {
        repository.save(entry(1))
        assertEquals(1, repository.history.value.size)
        repository.save(entry(2))
        assertEquals(listOf("id-2", "id-1"), repository.history.value.map { it.id })
    }

    // 41. History load
    @Test
    fun load_restoresEntriesFromStorage() {
        repository.save(entry(1))
        repository.save(entry(2))

        val reopened = DayCountHistoryRepository(store)
        assertEquals(repository.history.value, reopened.history.value)
        assertEquals(LocalDate.of(2026, 1, 3), reopened.history.value.first().firstDate)
    }

    @Test
    fun storage_usesIsoDatesAndNumbers() {
        repository.save(entry(1))
        val raw = store.getString("history_entries")!!
        assertEquals("id-1|2026-01-02|2026-01-08|7|5|2|1001", raw)
    }

    // 42. History limit
    @Test
    fun save_keepsOnlyTenAndDropsOldest() {
        (1..10).forEach { repository.save(entry(it)) }
        assertEquals(10, repository.history.value.size)

        repository.save(entry(11))
        val ids = repository.history.value.map { it.id }
        assertEquals(DayCountHistoryRepository.MAX_ENTRIES, ids.size)
        assertEquals("id-11", ids.first())
        assertTrue("oldest removed", "id-1" !in ids)
        assertEquals(10, DayCountHistoryRepository(store).history.value.size)
    }

    @Test
    fun save_sameDateRangeReplacesInsteadOfDuplicating() {
        val first = entry(1)
        repository.save(first)
        repository.save(entry(2))
        repository.save(first.copy(id = "again", calculatedAt = 5_000))

        val history = repository.history.value
        assertEquals(2, history.size)
        assertEquals("again", history.first().id)
    }

    // 43. History deletion
    @Test
    fun delete_removesOnlyThatEntry() {
        (1..3).forEach { repository.save(entry(it)) }
        repository.delete("id-2")
        assertEquals(listOf("id-3", "id-1"), repository.history.value.map { it.id })
        assertEquals(listOf("id-3", "id-1"), DayCountHistoryRepository(store).history.value.map { it.id })
    }

    // 44. History clear
    @Test
    fun clear_removesEverything() {
        (1..3).forEach { repository.save(entry(it)) }
        repository.clear()
        assertTrue(repository.history.value.isEmpty())
        assertTrue(DayCountHistoryRepository(store).history.value.isEmpty())
    }

    @Test
    fun decode_skipsMalformedLines() {
        val good = HistorySerializer.encode(listOf(entry(1)))
        val decoded = HistorySerializer.decode("$good\nnot-a-valid-line\nx|2026-13-01|2026-01-01|1|1|0|0")
        assertEquals(listOf(entry(1)), decoded)
        assertTrue(HistorySerializer.decode(null).isEmpty())
        assertTrue(HistorySerializer.decode("").isEmpty())
    }
}
