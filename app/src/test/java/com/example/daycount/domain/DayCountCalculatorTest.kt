package com.example.daycount.domain

import com.example.daycount.model.DayCountResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class DayCountCalculatorTest {

    private fun date(year: Int, month: Int, day: Int) = LocalDate.of(year, month, day)

    /** Calculates and checks the invariants that must hold for every result. */
    private fun count(first: LocalDate, second: LocalDate): DayCountResult {
        val result = DayCountCalculator.calculate(first, second)
        assertEquals("weekdays + weekendDays == totalDays", result.totalDays, result.weekdays + result.weekendDays)
        assertEquals("day-of-week counts sum to total", result.totalDays, result.dayOfWeekCounts.values.sum())
        assertTrue("never zero or negative", result.totalDays >= 1)
        return result
    }

    private fun assertCounts(result: DayCountResult, total: Long, weekdays: Long, weekends: Long) {
        assertEquals("total", total, result.totalDays)
        assertEquals("weekdays", weekdays, result.weekdays)
        assertEquals("weekends", weekends, result.weekendDays)
    }

    /** Reference implementation: walks every date. Only used for small ranges in tests. */
    private fun bruteForce(first: LocalDate, second: LocalDate): Triple<Long, Long, Long> {
        val (earlier, later) = if (second < first) second to first else first to second
        var weekdays = 0L
        var weekends = 0L
        var day = earlier
        while (!day.isAfter(later)) {
            if (day.dayOfWeek == DayOfWeek.SATURDAY || day.dayOfWeek == DayOfWeek.SUNDAY) weekends++ else weekdays++
            day = day.plusDays(1)
        }
        return Triple(weekdays + weekends, weekdays, weekends)
    }

    @Test
    fun exampleDatesHaveExpectedWeekdays() {
        assertEquals(DayOfWeek.MONDAY, date(2026, 10, 5).dayOfWeek)
        assertEquals(DayOfWeek.FRIDAY, date(2026, 10, 9).dayOfWeek)
        assertEquals(DayOfWeek.SATURDAY, date(2026, 10, 10).dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, date(2026, 10, 11).dayOfWeek)
        assertEquals(DayOfWeek.THURSDAY, date(2026, 12, 31).dayOfWeek)
    }

    // 1. Same date, 2. One weekday
    @Test
    fun sameDateWeekday_countsOneWeekday() {
        val result = count(date(2026, 10, 5), date(2026, 10, 5))
        assertCounts(result, total = 1, weekdays = 1, weekends = 0)
        assertTrue(result.isSingleDay)
        assertFalse(result.wasReordered)
    }

    // 3. One Saturday
    @Test
    fun sameDateSaturday_countsOneWeekendDay() {
        assertCounts(count(date(2026, 10, 10), date(2026, 10, 10)), total = 1, weekdays = 0, weekends = 1)
    }

    // 4. One Sunday
    @Test
    fun sameDateSunday_countsOneWeekendDay() {
        assertCounts(count(date(2026, 10, 11), date(2026, 10, 11)), total = 1, weekdays = 0, weekends = 1)
    }

    // 5. Monday–Friday
    @Test
    fun workWeek_mondayToFriday() {
        assertCounts(count(date(2026, 10, 5), date(2026, 10, 9)), total = 5, weekdays = 5, weekends = 0)
    }

    // 6. Saturday–Sunday
    @Test
    fun weekend_saturdayToSunday() {
        assertCounts(count(date(2026, 10, 10), date(2026, 10, 11)), total = 2, weekdays = 0, weekends = 2)
    }

    // 7. Full seven-day week
    @Test
    fun fullWeek_mondayToSunday() {
        val result = count(date(2026, 10, 5), date(2026, 10, 11))
        assertCounts(result, total = 7, weekdays = 5, weekends = 2)
        assertTrue(result.includesFullWeek)
        assertEquals(DayOfWeek.MONDAY, result.startDayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, result.endDayOfWeek)
    }

    @Test
    fun anySevenConsecutiveDays_areFiveWeekdaysAndTwoWeekendDays() {
        val monday = date(2026, 10, 5)
        for (offset in 0L..6L) {
            val start = monday.plusDays(offset)
            assertCounts(count(start, start.plusDays(6)), total = 7, weekdays = 5, weekends = 2)
        }
    }

    @Test
    fun wednesdayToTuesday_isStillFullWeek() {
        assertCounts(count(date(2026, 10, 7), date(2026, 10, 13)), total = 7, weekdays = 5, weekends = 2)
    }

    // 8. Full fourteen-day range
    @Test
    fun twoWeeks_mondayToSunday() {
        assertCounts(count(date(2026, 10, 5), date(2026, 10, 18)), total = 14, weekdays = 10, weekends = 4)
    }

    @Test
    fun anyFourteenConsecutiveDays_areTenWeekdaysAndFourWeekendDays() {
        for (offset in 0L..6L) {
            val start = date(2026, 10, 5).plusDays(offset)
            assertCounts(count(start, start.plusDays(13)), total = 14, weekdays = 10, weekends = 4)
        }
    }

    @Test
    fun wednesdayToFriday() {
        assertCounts(count(date(2026, 10, 7), date(2026, 10, 9)), total = 3, weekdays = 3, weekends = 0)
    }

    // 9. Friday–Monday
    @Test
    fun fridayToMonday() {
        assertCounts(count(date(2026, 10, 9), date(2026, 10, 12)), total = 4, weekdays = 2, weekends = 2)
    }

    // 10. Saturday–Monday
    @Test
    fun saturdayToMonday() {
        assertCounts(count(date(2026, 10, 10), date(2026, 10, 12)), total = 3, weekdays = 1, weekends = 2)
    }

    // 11. Sunday–Monday
    @Test
    fun sundayToMonday() {
        assertCounts(count(date(2026, 10, 11), date(2026, 10, 12)), total = 2, weekdays = 1, weekends = 1)
    }

    // 12. Month boundary
    @Test
    fun monthBoundary() {
        assertCounts(count(date(2026, 9, 30), date(2026, 10, 2)), total = 3, weekdays = 3, weekends = 0)
    }

    // 13. Year boundary, 17. December 31, 18. January 1
    @Test
    fun yearBoundary() {
        assertCounts(count(date(2026, 12, 31), date(2027, 1, 2)), total = 3, weekdays = 2, weekends = 1)
    }

    @Test
    fun december31_singleDay() {
        assertCounts(count(date(2026, 12, 31), date(2026, 12, 31)), total = 1, weekdays = 1, weekends = 0)
    }

    @Test
    fun january1_singleDay() {
        // 1 January 2027 is a Friday; 1 January 2028 is a Saturday.
        assertCounts(count(date(2027, 1, 1), date(2027, 1, 1)), total = 1, weekdays = 1, weekends = 0)
        assertCounts(count(date(2028, 1, 1), date(2028, 1, 1)), total = 1, weekdays = 0, weekends = 1)
    }

    @Test
    fun wholeYear2026() {
        // 2026 starts and ends on Thursday: 52 weeks + 1 extra weekday.
        assertCounts(count(date(2026, 1, 1), date(2026, 12, 31)), total = 365, weekdays = 261, weekends = 104)
    }

    // 14. Leap year
    @Test
    fun leapDayRange() {
        assertCounts(count(date(2024, 2, 28), date(2024, 3, 1)), total = 3, weekdays = 3, weekends = 0)
    }

    @Test
    fun leapYear_hasThreeHundredSixtySixDays() {
        assertCounts(count(date(2024, 1, 1), date(2024, 12, 31)), total = 366, weekdays = 262, weekends = 104)
    }

    @Test
    fun nonLeapFebruary_skipsToMarch() {
        assertCounts(count(date(2026, 2, 28), date(2026, 3, 1)), total = 2, weekdays = 0, weekends = 2)
    }

    // 15. February 29
    @Test
    fun february29_singleDay() {
        val result = count(date(2024, 2, 29), date(2024, 2, 29))
        assertCounts(result, total = 1, weekdays = 1, weekends = 0)
        assertEquals(DayOfWeek.THURSDAY, result.startDayOfWeek)
    }

    // 16. January 31
    @Test
    fun january31_toFebruary1() {
        // 31 Jan 2026 is Saturday, 1 Feb 2026 is Sunday.
        assertCounts(count(date(2026, 1, 31), date(2026, 2, 1)), total = 2, weekdays = 0, weekends = 2)
    }

    // 19. Reversed dates, 20. Date normalization
    @Test
    fun reversedDates_areNormalized() {
        val result = count(date(2026, 10, 11), date(2026, 10, 5))
        assertCounts(result, total = 7, weekdays = 5, weekends = 2)
        assertTrue(result.wasReordered)
        assertEquals(date(2026, 10, 5), result.earlierDate)
        assertEquals(date(2026, 10, 11), result.laterDate)
        assertEquals(DayOfWeek.MONDAY, result.startDayOfWeek)
    }

    @Test
    fun normalize_returnsEarlierThenLater() {
        val a = date(2026, 10, 20)
        val b = date(2026, 10, 10)
        assertEquals(b to a, DayCountCalculator.normalize(a, b))
        assertEquals(b to a, DayCountCalculator.normalize(b, a))
        assertEquals(a to a, DayCountCalculator.normalize(a, a))
    }

    @Test
    fun reversedAndForward_giveSameCounts() {
        val forward = count(date(2026, 10, 10), date(2026, 10, 20))
        val reversed = count(date(2026, 10, 20), date(2026, 10, 10))
        assertEquals(forward.copy(wasReordered = true), reversed)
    }

    // 21. Inclusive counting
    @Test
    fun inclusiveCount_neverReturnsZero() {
        assertEquals(1L, DayCountCalculator.inclusiveTotalDays(date(2026, 10, 1), date(2026, 10, 1)))
        assertEquals(1L, count(date(2026, 10, 1), date(2026, 10, 1)).totalDays)
    }

    // 22. Total days
    @Test
    fun totalDays_isChronoUnitDaysPlusOne() {
        val start = date(2026, 1, 15)
        val end = date(2026, 11, 3)
        assertEquals(ChronoUnit.DAYS.between(start, end) + 1, count(start, end).totalDays)
    }

    // 23. Weekday count, 24. Weekend count, 25. Invariant — cross-checked against brute force
    @Test
    fun matchesBruteForce_forEveryStartDayAndLengthUpToTenWeeks() {
        val monday = date(2026, 10, 5)
        for (startOffset in 0L..6L) {
            val start = monday.plusDays(startOffset)
            for (length in 0L..70L) {
                val end = start.plusDays(length)
                val result = count(start, end)
                val (total, weekdays, weekends) = bruteForce(start, end)
                assertCounts(result, total, weekdays, weekends)
            }
        }
    }

    @Test
    fun matchesBruteForce_forPseudoRandomRanges() {
        val random = java.util.Random(42)
        val base = date(1990, 1, 1)
        repeat(300) {
            val first = base.plusDays(random.nextInt(20_000).toLong())
            val second = first.plusDays(random.nextInt(2_000).toLong() - 1_000)
            val (total, weekdays, weekends) = bruteForce(first, second)
            assertCounts(count(first, second), total, weekdays, weekends)
        }
    }

    // 26. Day-of-week distribution
    @Test
    fun dayOfWeekDistribution_fullWeek_isOneEach() {
        val result = count(date(2026, 10, 5), date(2026, 10, 11))
        DayOfWeek.entries.forEach { assertEquals(1L, result.dayOfWeekCounts[it]) }
        assertEquals(DayOfWeek.entries, result.dayOfWeekCountList.map { it.dayOfWeek })
    }

    @Test
    fun dayOfWeekDistribution_mondayToTuesday() {
        val result = count(date(2026, 10, 5), date(2026, 10, 6))
        assertEquals(1L, result.dayOfWeekCounts[DayOfWeek.MONDAY])
        assertEquals(1L, result.dayOfWeekCounts[DayOfWeek.TUESDAY])
        listOf(
            DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY,
        ).forEach { assertEquals(0L, result.dayOfWeekCounts[it]) }
    }

    @Test
    fun dayOfWeekDistribution_weekendEntriesMatchWeekendCount() {
        val result = count(date(2026, 1, 1), date(2026, 12, 31))
        val weekendSum = result.dayOfWeekCounts.filterKeys { DayCountCalculator.isWeekend(it) }.values.sum()
        assertEquals(result.weekendDays, weekendSum)
        assertEquals(53L, result.dayOfWeekCounts[DayOfWeek.THURSDAY])
        assertEquals(52L, result.dayOfWeekCounts[DayOfWeek.FRIDAY])
    }

    // 27. Single-day detection
    @Test
    fun singleDayDetection() {
        assertTrue(count(date(2026, 10, 5), date(2026, 10, 5)).isSingleDay)
        assertFalse(count(date(2026, 10, 5), date(2026, 10, 6)).isSingleDay)
    }

    // 28. Full-week detection
    @Test
    fun fullWeekDetection() {
        assertFalse(count(date(2026, 10, 5), date(2026, 10, 10)).includesFullWeek)
        assertTrue(count(date(2026, 10, 5), date(2026, 10, 11)).includesFullWeek)
        assertEquals(2L, count(date(2026, 10, 5), date(2026, 10, 20)).completeWeeks)
    }

    // 29. Percentage calculation
    @Test
    fun percentages_roundToWholeNumbersAndSumTo100() {
        val week = DayCountCalculator.percentages(count(date(2026, 10, 5), date(2026, 10, 11)))
        assertEquals(71, week.weekdays)
        assertEquals(29, week.weekends)

        val october = count(date(2026, 10, 1), date(2026, 10, 31))
        assertCounts(october, total = 31, weekdays = 22, weekends = 9)
        val octoberPercent = DayCountCalculator.percentages(october)
        assertEquals(71, octoberPercent.weekdays)
        assertEquals(29, octoberPercent.weekends)
    }

    @Test
    fun percentages_singleDay_is100And0() {
        val weekday = DayCountCalculator.percentages(count(date(2026, 10, 5), date(2026, 10, 5)))
        assertEquals(100, weekday.weekdays)
        assertEquals(0, weekday.weekends)
        val weekend = DayCountCalculator.percentages(count(date(2026, 10, 10), date(2026, 10, 10)))
        assertEquals(0, weekend.weekdays)
        assertEquals(100, weekend.weekends)
    }

    @Test
    fun percentages_alwaysSumTo100() {
        val start = date(2026, 10, 5)
        for (length in 0L..400L) {
            val p = DayCountCalculator.percentages(count(start, start.plusDays(length)))
            assertEquals(100, p.weekdays + p.weekends)
        }
    }

    // 30. Zero weekend days
    @Test
    fun zeroWeekendDays() {
        val result = count(date(2026, 10, 6), date(2026, 10, 8))
        assertEquals(0L, result.weekendDays)
        assertEquals(3L, result.weekdays)
    }

    // 31. Zero weekday days
    @Test
    fun zeroWeekdays() {
        val result = count(date(2026, 10, 10), date(2026, 10, 11))
        assertEquals(0L, result.weekdays)
        assertEquals(2L, result.weekendDays)
    }

    // Large ranges
    @Test
    fun multiYearRange() {
        // 2000-01-01 (Saturday) → 2025-12-31 (Wednesday)
        val start = date(2000, 1, 1)
        val end = date(2025, 12, 31)
        val result = count(start, end)
        val (total, weekdays, weekends) = bruteForce(start, end)
        assertCounts(result, total, weekdays, weekends)
        assertEquals(9497L, result.totalDays)
    }

    @Test
    fun fourHundredYearCycle_isExactWeeks() {
        // The Gregorian calendar repeats every 400 years = 146,097 days = 20,871 weeks.
        val result = count(date(2000, 1, 1), date(2399, 12, 31))
        assertCounts(result, total = 146_097, weekdays = 104_355, weekends = 41_742)
        DayOfWeek.entries.forEach { assertEquals(20_871L, result.dayOfWeekCounts[it]) }
    }

    @Test
    fun entireLocalDateRange_isFastAndConsistent() {
        val startNanos = System.nanoTime()
        val result = count(LocalDate.MIN, LocalDate.MAX)
        val elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000

        assertEquals(ChronoUnit.DAYS.between(LocalDate.MIN, LocalDate.MAX) + 1, result.totalDays)
        assertTrue("Calculation took ${elapsedMillis}ms", elapsedMillis < 200)
        // Within any range, weekdays are 5/7 of the total, give or take the leftover days.
        val expectedWeekdays = result.totalDays / 7 * 5
        assertTrue(result.weekdays - expectedWeekdays in 0..5)
    }

    @Test
    fun manyLargeCalculations_runInConstantTime() {
        val startNanos = System.nanoTime()
        repeat(10_000) { i ->
            DayCountCalculator.calculate(LocalDate.of(-100_000 + i, 1, 1), LocalDate.of(100_000 + i, 6, 30))
        }
        val elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000
        assertTrue("10,000 large calculations took ${elapsedMillis}ms", elapsedMillis < 2_000)
    }

    @Test
    fun weekendDefinition_isSaturdayAndSundayOnly() {
        assertTrue(DayCountCalculator.isWeekend(DayOfWeek.SATURDAY))
        assertTrue(DayCountCalculator.isWeekend(DayOfWeek.SUNDAY))
        listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
            .forEach { assertTrue(DayCountCalculator.isWeekday(it)) }
    }
}
