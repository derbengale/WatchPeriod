package com.watchyourself.watchperiod

import com.watchyourself.watchperiod.domain.CycleCalculator
import com.watchyourself.watchperiod.domain.CycleState
import com.watchyourself.watchperiod.domain.DotKind
import com.watchyourself.watchperiod.domain.Period
import com.watchyourself.watchperiod.domain.Settings
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CycleCalculatorTest {
    private val today = LocalDate.of(2026, 10, 29)

    @Test
    fun noData() {
        val s = CycleCalculator.compute(emptyList(), Settings(), today)
        assertEquals(28, s.dots.size)
        assertEquals(-1, s.todayIndex)
    }

    @Test
    fun averagesGapsAndPredicts() {
        val periods = listOf(
            Period(3, LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 24)),
            Period(2, LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 24)), // 30
            Period(1, LocalDate.of(2026, 8, 24), LocalDate.of(2026, 8, 28)), // 27
        )
        val s = CycleCalculator.compute(periods, Settings(), today)
        assertEquals(29, s.cycleLength) // round(28.5)
        assertEquals(15, s.ovulationDay)
        assertEquals(9, s.todayIndex)
        assertEquals(LocalDate.of(2026, 11, 18), s.nextPeriod)
        assertEquals(DotKind.PERIOD, s.dots[4])
        assertEquals(DotKind.FOLLICULAR, s.dots[5])
    }

    @Test
    fun openPeriod() {
        val s = CycleCalculator.compute(listOf(Period(1, LocalDate.of(2026, 10, 28), null)), Settings(), today)
        assertEquals(true, s.periodOngoing)
        assertEquals(2, s.periodDay)
        assertEquals(DotKind.PREDICTED, s.dots[2])
    }

    @Test
    fun phasesAreAnchoredToOvulation() {
        fun stateFor(length: Int): CycleState {
            val period = Period(1, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5))
            return CycleCalculator.compute(
                listOf(period),
                Settings(autoCycle = false, cycleLength = length),
                LocalDate.of(2026, 10, 10),
            )
        }

        val c28 = stateFor(28)
        assertEquals(14, c28.ovulationDay)
        assertEquals(DotKind.FOLLICULAR, c28.dots[12])
        assertEquals(DotKind.OVULATION, c28.dots[13])
        assertEquals(DotKind.LUTEAL, c28.dots[14])
        assertEquals(DotKind.PREMENSTRUAL, c28.dots[22])

        val c35 = stateFor(35)
        assertEquals(21, c35.ovulationDay)
        assertEquals(DotKind.OVULATION, c35.dots[20])
        assertEquals(DotKind.LUTEAL, c35.dots[21])
        assertEquals(DotKind.PREMENSTRUAL, c35.dots[29])

        val c21 = stateFor(21)
        assertEquals(7, c21.ovulationDay)
        assertEquals(DotKind.OVULATION, c21.dots[6])
        assertEquals(DotKind.LUTEAL, c21.dots[7])
        assertEquals(DotKind.PREMENSTRUAL, c21.dots[15])
    }

    @Test
    fun projectedCycleMarksTheCalculatedPeriod() {
        val period = Period(1, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5))
        val s = CycleCalculator.compute(
            listOf(period),
            Settings(autoCycle = false, cycleLength = 28, periodLength = 5),
            LocalDate.of(2026, 9, 30),
        )

        // 29 days after the logged start: the app shows cycle 2, day 2.
        assertEquals(1, s.cyclesSinceLogged)
        assertTrue(s.periodPredicted)
        assertEquals(1, s.todayIndex)
        assertEquals(LocalDate.of(2026, 9, 29), s.cycleStart)
        assertEquals(LocalDate.of(2026, 9, 30), s.dateForOffset(1))
        // The calculated start is hollow/predicted, not silently logged as a period.
        assertEquals(DotKind.PREDICTED, s.dots[0])
        assertEquals(DotKind.PREDICTED, s.dots[4])
        assertEquals(DotKind.FOLLICULAR, s.dots[5])
        assertEquals(5, s.predictedPeriodDays)
        assertEquals(-1, s.daysUntilNext)
    }

    @Test
    fun forgottenOpenPeriodStopsBeingOngoing() {
        val s = CycleCalculator.compute(
            listOf(Period(1, LocalDate.of(2026, 9, 1), null)),
            Settings(autoCycle = false, cycleLength = 28, periodLength = 5),
            LocalDate.of(2026, 9, 20),
        )

        assertEquals(false, s.periodOngoing)
        assertEquals(null, s.periodDay)
        assertEquals(DotKind.PERIOD, s.dots[4])
        assertEquals(DotKind.FOLLICULAR, s.dots[5])
        assertEquals(0, s.cyclesSinceLogged)
    }

    @Test
    fun offsetForMapsDatesIntoTheShownCycle() {
        val s = CycleCalculator.compute(
            listOf(Period(1, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5))),
            Settings(autoCycle = false, cycleLength = 28),
            LocalDate.of(2026, 9, 30),
        )
        assertEquals(0, s.offsetFor(LocalDate.of(2026, 9, 29)))
        assertEquals(2, s.offsetFor(LocalDate.of(2026, 10, 1)))
        assertEquals(null, s.offsetFor(LocalDate.of(2026, 9, 28)))
    }

    @Test
    fun shortCycleStaysInBounds() {
        val s = CycleCalculator.compute(
            listOf(Period(1, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3))),
            Settings(autoCycle = false, cycleLength = 15),
            LocalDate.of(2026, 10, 10),
        )
        assertEquals(15, s.dots.size)
        assertEquals(2, s.ovulationDay)
        assertTrue(s.dots.all { it != DotKind.NORMAL })
    }

    @Test
    fun calculatedPeriodExposesLengthAndEnd() {
        val s = CycleCalculator.compute(
            listOf(Period(1, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5))),
            Settings(autoCycle = false, cycleLength = 28, periodLength = 5),
            LocalDate.of(2026, 10, 10),
        )
        assertEquals(5, s.periodLength)
        assertEquals(LocalDate.of(2026, 10, 29), s.nextPeriod)
        assertEquals(LocalDate.of(2026, 11, 2), s.nextPeriodEnd)
    }

    @Test
    fun openPeriodAlsoHasACalculatedNextEnd() {
        val s = CycleCalculator.compute(
            listOf(Period(1, LocalDate.of(2026, 10, 28), null)),
            Settings(autoCycle = false, cycleLength = 28, periodLength = 5),
            LocalDate.of(2026, 10, 29),
        )
        assertEquals(5, s.periodLength)
        assertEquals(LocalDate.of(2026, 11, 25), s.nextPeriod)
        assertEquals(LocalDate.of(2026, 11, 29), s.nextPeriodEnd)
    }
}
