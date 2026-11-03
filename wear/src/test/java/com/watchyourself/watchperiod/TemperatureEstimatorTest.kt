package com.watchyourself.watchperiod

import com.watchyourself.watchperiod.domain.DotKind
import com.watchyourself.watchperiod.domain.HormoneEstimator
import com.watchyourself.watchperiod.domain.TemperatureEstimator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemperatureEstimatorTest {
    private fun celsius(day: Int, length: Int = 28): Float = TemperatureEstimator.celsius(day, length)

    @Test
    fun curveCoversEveryCycleDayAndStaysInAPlausibleBand() {
        listOf(21, 24, 28, 35, 45).forEach { length ->
            val curve = TemperatureEstimator.curve(length)

            assertEquals(length, curve.size)
            assertTrue(curve.all { it in 35.9f..37.1f })
        }
    }

    @Test
    fun nadirSitsAtOvulation() {
        listOf(24, 28, 35).forEach { length ->
            val nadir = (0 until length).minByOrNull { celsius(it, length) }

            assertEquals(HormoneEstimator.ovulationIndex(length), nadir)
        }
    }

    @Test
    fun lutealPhaseIsAboutHalfADegreeHigherThanFollicular() {
        val follicular = (2..10).map { celsius(it) }.average()
        val luteal = (17..24).map { celsius(it) }.average()

        assertTrue("shift should be within 0.2–0.7 °C", luteal - follicular in 0.2..0.7)
    }

    @Test
    fun temperatureFallsAgainBeforeMenstruation() {
        val midLuteal = celsius(20)
        val lastDay = celsius(27)

        assertTrue(lastDay < midLuteal)
        assertTrue(midLuteal - lastDay in 0.2f..0.5f)
    }

    @Test
    fun shiftFollowsTheOvulationDayOfShorterCycles() {
        val length = 24
        val ovulation = HormoneEstimator.ovulationIndex(length)
        val before = celsius(ovulation - 2, length)
        val after = celsius(ovulation + 3, length)

        assertTrue(after - before >= 0.2f)
    }

    @Test
    fun estimateUsesGermanNumberFormatAndPhaseNote() {
        val estimate = TemperatureEstimator.estimate(20, 28, DotKind.LUTEAL)

        assertTrue(estimate.valueText.contains(','))
        assertTrue(estimate.valueText.endsWith("°C"))
        assertTrue(estimate.shiftText.startsWith("+"))
        assertEquals(DotKind.LUTEAL, estimate.phase)
        assertTrue(estimate.note.isNotBlank())
    }

    @Test
    fun differenceToMaleIsRelativeToTheMaleReference() {
        assertEquals(0f, TemperatureEstimator.differenceToMale(TemperatureEstimator.MALE_REFERENCE), 0.0001f)
        assertEquals(0.4f, TemperatureEstimator.differenceToMale(36.9f), 0.0001f)
        assertEquals(-0.2f, TemperatureEstimator.differenceToMale(36.3f), 0.0001f)
    }

    @Test
    fun predictedPeriodUsesTheMenstruationNote() {
        val estimate = TemperatureEstimator.estimate(0, 28, DotKind.PREDICTED)

        assertEquals(DotKind.PERIOD, estimate.phase)
        assertTrue(estimate.note.startsWith("Menstruation"))
    }
}
