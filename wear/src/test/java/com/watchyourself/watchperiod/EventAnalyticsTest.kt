package com.watchyourself.watchperiod

import com.watchyourself.watchperiod.domain.CycleEvent
import com.watchyourself.watchperiod.domain.DotKind
import com.watchyourself.watchperiod.domain.EventAnalytics
import com.watchyourself.watchperiod.domain.EventType
import com.watchyourself.watchperiod.domain.Period
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EventAnalyticsTest {

    private val day1 = LocalDate.of(2026, 3, 1)
    private val day29 = LocalDate.of(2026, 3, 29)
    private val today = LocalDate.of(2026, 4, 5)
    private val periods = listOf(
        Period(2, day29, null),
        Period(1, day1, LocalDate.of(2026, 3, 5)),
    )

    @Test
    fun dayIndexCountsFromTheMostRecentPeriodStart() {
        assertEquals(0, EventAnalytics.dayIndex(periods, day1, 28))
        assertEquals(27, EventAnalytics.dayIndex(periods, LocalDate.of(2026, 3, 28), 28))
        assertEquals(0, EventAnalytics.dayIndex(periods, day29, 28))
        assertEquals(7, EventAnalytics.dayIndex(periods, today, 28))
    }

    @Test
    fun dayIndexIsNullBeforeTheFirstLoggedPeriod() {
        assertNull(EventAnalytics.dayIndex(periods, LocalDate.of(2026, 2, 20), 28))
        assertNull(EventAnalytics.dayIndex(emptyList(), today, 28))
    }

    @Test
    fun lateEventsAreClampedIntoTherRing() {
        val long = EventAnalytics.dayIndex(listOf(Period(1, day1, null)), day1.plusDays(45), 28)
        assertEquals(27, long)
    }

    @Test
    fun analyseCountsEventsPerCycleDayAndCycle() {
        val events = listOf(
            CycleEvent(1, LocalDate.of(2026, 3, 27), EventType.ANXIETY),
            CycleEvent(2, today, EventType.ANXIETY),
        )
        val stats = EventAnalytics.analyse(events, periods, 28, today)
        assertEquals(2, stats.cycles)
        assertEquals(2, stats.total(EventType.ANXIETY))
        // Day 26 was only covered by the March cycle; day 7 was covered by both cycles.
        assertEquals(1f, stats.rate(EventType.ANXIETY)[26], 0.001f)
        assertEquals(0f, stats.rate(EventType.ANXIETY)[27], 0.001f)
        assertEquals(0.5f, stats.rate(EventType.ANXIETY)[7], 0.001f)
        assertEquals(0, stats.total(EventType.CONFLICT))
    }

    @Test
    fun eventsBeforeTheFirstPeriodAreIgnored() {
        val events = listOf(CycleEvent(1, LocalDate.of(2026, 2, 1), EventType.SEX))
        val stats = EventAnalytics.analyse(events, periods, 28, today)
        assertEquals(0, stats.total(EventType.SEX))
        assertTrue(stats.isEmpty)
    }

    @Test
    fun withoutDataTheCurveStaysEmpty() {
        val stats = EventAnalytics.analyse(emptyList(), emptyList(), 28, today)
        assertEquals(0, stats.cycles)
        for (type in EventType.entries) {
            val values = EventAnalytics.observed(stats, type)
            assertEquals(28, values.size)
            assertTrue(values.all { it == 0f })
        }
        assertNull(EventAnalytics.sensitiveDays(stats))
    }

    @Test
    fun ownEventsShapeTheCurve() {
        val start = LocalDate.of(2026, 3, 1)
        val cycles = listOf(Period(1, start, LocalDate.of(2026, 3, 5)))
        val events = listOf(
            CycleEvent(1, LocalDate.of(2026, 3, 3), EventType.CONFLICT), // cycle day 3
            CycleEvent(2, LocalDate.of(2026, 3, 21), EventType.CONFLICT), // cycle day 21
            CycleEvent(3, LocalDate.of(2026, 3, 21), EventType.CONFLICT), // same day again
        )
        val stats = EventAnalytics.analyse(events, cycles, 28, LocalDate.of(2026, 4, 5))
        val curve = EventAnalytics.observed(stats, EventType.CONFLICT)

        assertEquals(1f, curve[20], 0.001f)
        assertEquals(0.5f, curve[2], 0.001f)
        assertTrue(curve.indices.filter { it != 2 && it != 20 }.all { curve[it] == 0f })
        assertTrue(EventAnalytics.observed(stats, EventType.ANXIETY).all { it == 0f })
    }

    @Test
    fun phaseIntensitiesMarkThePremenstrualWindow() {
        val values = FloatArray(28) { if (it >= 22) 1f else 0.1f }
        val phases = EventAnalytics.phaseIntensities(values, 28)
        assertEquals(DotKind.PREMENSTRUAL, phases.first().phase)
        assertTrue(phases.first().relative > 1f)
    }

    @Test
    fun sensitiveDaysComeFromOwnEventsOnly() {
        val start = LocalDate.of(2026, 3, 1)
        val cycles = listOf(Period(1, start, null))
        val events = (24..26).flatMapIndexed { i, dayIndex ->
            val date = start.plusDays(dayIndex.toLong())
            listOf(
                CycleEvent((i * 2 + 1).toLong(), date, EventType.ANXIETY),
                CycleEvent((i * 2 + 2).toLong(), date, EventType.CONFLICT),
            )
        }
        val stats = EventAnalytics.analyse(events, cycles, 28, LocalDate.of(2026, 3, 30))
        val range = EventAnalytics.sensitiveDays(stats)
        assertNotNull(range)
        assertEquals(25..27, range)
    }

    @Test
    fun moodIsStoredAsValueAndAveragedPerCycleDay() {
        val events = listOf(
            CycleEvent(1, LocalDate.of(2026, 3, 3), EventType.MOOD, 2),
            CycleEvent(2, LocalDate.of(2026, 3, 31), EventType.MOOD, 0),
        )
        val stats = EventAnalytics.analyse(events, periods, 28, LocalDate.of(2026, 4, 5))

        // Both dates are cycle day 3 (index 2): mean of 2 and 0.
        assertEquals(1f, stats.mood(2) ?: -99f, 0.001f)
        assertEquals(2, stats.moodDays())
        assertEquals(1f, stats.moodAverage ?: -99f, 0.001f)
        // Mood curve: −2 → 0, +2 → 1, so the mean of 1 becomes 0.75.
        assertEquals(0.75f, EventAnalytics.observed(stats, EventType.MOOD)[2], 0.001f)
        val logged = EventAnalytics.logged(stats, EventType.MOOD)
        assertNotNull(logged)
        assertEquals(true, logged!![2])
        assertEquals(false, logged[1])
        assertNull(EventAnalytics.logged(stats, EventType.CONFLICT))
    }

    @Test
    fun correlationsReportPearsonParameters() {
        val start = LocalDate.of(2026, 3, 1)
        val events = (18..21).mapIndexed { i, day ->
            CycleEvent((i + 1).toLong(), start.plusDays(day.toLong()), EventType.CONFLICT)
        }
        val stats = EventAnalytics.analyse(
            events, listOf(Period(1, start, null)), 28, LocalDate.of(2026, 3, 29),
        )
        val correlations = EventAnalytics.correlations(stats, EventType.CONFLICT)

        assertTrue(correlations.isNotEmpty())
        assertEquals(3, correlations.size)
        assertTrue(correlations.all { it.r in -1f..1f && it.r2 in 0f..1f && it.n == 28 })
        assertTrue(correlations.any { it.r > 0f })
    }

    @Test
    fun moodCorrelationUsesOnlyDaysWithEntries() {
        val start = LocalDate.of(2026, 3, 1)
        val events = listOf(
            CycleEvent(1, start.plusDays(2), EventType.MOOD, 2),
            CycleEvent(2, start.plusDays(10), EventType.MOOD, 1),
            CycleEvent(3, start.plusDays(20), EventType.MOOD, -1),
            CycleEvent(4, start.plusDays(25), EventType.MOOD, -2),
        )
        val stats = EventAnalytics.analyse(
            events, listOf(Period(1, start, null)), 28, LocalDate.of(2026, 3, 29),
        )
        val correlations = EventAnalytics.correlations(stats, EventType.MOOD)

        assertTrue(correlations.isNotEmpty())
        assertTrue(correlations.all { it.n == 4 })
    }

    @Test
    fun tooLittleDataYieldsNoCorrelation() {
        val start = LocalDate.of(2026, 3, 1)
        val stats = EventAnalytics.analyse(
            listOf(CycleEvent(1, start.plusDays(3), EventType.ANXIETY)),
            listOf(Period(1, start, null)),
            28,
            LocalDate.of(2026, 3, 29),
        )
        assertTrue(EventAnalytics.correlations(stats, EventType.ANXIETY).isEmpty())
    }
}
