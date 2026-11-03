package com.watchyourself.watchperiod

import com.watchyourself.watchperiod.domain.DotKind
import com.watchyourself.watchperiod.domain.Hormone
import com.watchyourself.watchperiod.domain.HormoneEstimator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HormoneEstimatorTest {
    private fun level(day: Int, hormone: Hormone, length: Int = 28): Float =
        HormoneEstimator.levels(day, length).getValue(hormone)

    @Test
    fun estrogenHasPreOvulatoryAndMidLutealPeaks() {
        val early = level(2, Hormone.ESTROGEN)
        val preOvulatory = level(12, Hormone.ESTROGEN)
        val earlyLuteal = level(14, Hormone.ESTROGEN)
        val midLuteal = level(20, Hormone.ESTROGEN)
        val lateLuteal = level(27, Hormone.ESTROGEN)

        assertTrue("pre-ovulatory peak should exceed early follicular", preOvulatory > early)
        assertTrue("mid-luteal peak should exceed early luteal", midLuteal > earlyLuteal)
        assertTrue("mid-luteal peak should exceed late luteal", midLuteal > lateLuteal)
    }

    @Test
    fun progesteronePeaksMidLuteal() {
        val follicular = level(5, Hormone.PROGESTERONE)
        val earlyLuteal = level(14, Hormone.PROGESTERONE)
        val midLuteal = level(20, Hormone.PROGESTERONE)
        val lateLuteal = level(27, Hormone.PROGESTERONE)

        assertTrue("mid-luteal progesterone should exceed follicular", midLuteal > follicular)
        assertTrue("mid-luteal progesterone should exceed early luteal", midLuteal > earlyLuteal)
        assertTrue("mid-luteal progesterone should exceed late luteal", midLuteal > lateLuteal)
    }

    @Test
    fun lhSurgesAroundOvulation() {
        assertTrue(level(12, Hormone.LH) > level(5, Hormone.LH))
        assertTrue(level(13, Hormone.LH) > level(5, Hormone.LH))
    }

    @Test
    fun fshHasOvulatorySurge() {
        assertTrue(level(12, Hormone.FSH) > level(8, Hormone.FSH))
    }

    @Test
    fun testosteronePeaksMidcycle() {
        val midcycle = level(13, Hormone.TESTOSTERONE)
        assertTrue(midcycle > level(2, Hormone.TESTOSTERONE))
        assertTrue(midcycle > level(25, Hormone.TESTOSTERONE))
    }

    @Test
    fun prolactinPeaksAroundOvulationAndStaysHigherInTheLutealPhase() {
        val follicular = level(2, Hormone.PROLACTIN)
        val ovulatory = level(13, Hormone.PROLACTIN)
        val luteal = level(20, Hormone.PROLACTIN)

        assertTrue(ovulatory > follicular)
        assertTrue(luteal > follicular)
        assertTrue(ovulatory >= luteal)
    }

    @Test
    fun oxytocinIsHigherAroundOvulationAndInTheLutealPhase() {
        val follicular = level(2, Hormone.OXYTOCIN)

        assertTrue(level(13, Hormone.OXYTOCIN) > follicular)
        assertTrue(level(20, Hormone.OXYTOCIN) > follicular)
    }

    @Test
    fun inhibinAPeaksMidLuteal() {
        val midLuteal = level(20, Hormone.INHIBIN_A)

        assertTrue(midLuteal > level(2, Hormone.INHIBIN_A))
        assertTrue(midLuteal > level(13, Hormone.INHIBIN_A))
        assertTrue(midLuteal > level(16, Hormone.INHIBIN_A))
    }

    @Test
    fun relaxinRisesTowardsTheEndOfTheLutealPhase() {
        assertTrue(level(24, Hormone.RELAXIN) > level(2, Hormone.RELAXIN))
        assertTrue(level(24, Hormone.RELAXIN) > level(13, Hormone.RELAXIN))
    }

    @Test
    fun kisspeptinPeaksWithTheLhSurge() {
        assertTrue(level(12, Hormone.KISSPEPTIN) > level(2, Hormone.KISSPEPTIN))
        assertTrue(level(12, Hormone.KISSPEPTIN) > level(24, Hormone.KISSPEPTIN))
    }

    @Test
    fun prostaglandinsPeakDuringMenstruation() {
        val firstDay = level(0, Hormone.PROSTAGLANDIN)

        assertTrue(firstDay > level(10, Hormone.PROSTAGLANDIN))
        assertTrue(firstDay > level(20, Hormone.PROSTAGLANDIN))
    }

    @Test
    fun cortisolStaysInANarrowBand() {
        val values = (0 until 28).map { level(it, Hormone.CORTISOL) }

        assertTrue(values.max() - values.min() < 0.2f)
    }

    @Test
    fun defaultVisibleIsEveryHormoneThatAffectsWellBeing() {
        assertEquals(Hormone.entries.filter { it.moodRelevant }.toSet(), Hormone.defaultVisible)
        assertTrue(Hormone.ESTROGEN in Hormone.defaultVisible)
        assertTrue(Hormone.CORTISOL in Hormone.defaultVisible)
        assertTrue(Hormone.FSH !in Hormone.defaultVisible)
        assertTrue(Hormone.LH !in Hormone.defaultVisible)
    }

    @Test
    fun levelsStayInRangeForDifferentCycleLengths() {
        listOf(15, 21, 28, 35, 45).forEach { length ->
            for (day in 0 until length) {
                HormoneEstimator.levels(day, length).values.forEach { value ->
                    assertTrue("value $value out of range for $day/$length", value in 0f..1f)
                }
            }
        }
    }

    @Test
    fun ovulationPhaseFollowsCycleLength() {
        assertEquals(DotKind.OVULATION, HormoneEstimator.phaseAt(13, 28))
        assertEquals(DotKind.OVULATION, HormoneEstimator.phaseAt(20, 35))
        assertEquals(DotKind.OVULATION, HormoneEstimator.phaseAt(6, 21))
        assertEquals(DotKind.OVULATION, HormoneEstimator.phaseAt(1, 15))
    }

    @Test
    fun trendIsTheMeanDailyChangeOfTheNextDays() {
        val trend = HormoneEstimator.trend(8, 28, days = 3)
        val expected = (level(11, Hormone.ESTROGEN) - level(8, Hormone.ESTROGEN)) / 3f

        assertEquals(expected, trend.getValue(Hormone.ESTROGEN), 1e-5f)
        assertTrue("estrogen rises towards ovulation", trend.getValue(Hormone.ESTROGEN) > 0f)
        assertTrue("prostaglandins fall after menstruation", trend.getValue(Hormone.PROSTAGLANDIN) < 0f)
    }

    @Test
    fun trendWrapsIntoTheNextCycle() {
        val trend = HormoneEstimator.trend(26, 28, days = 3)
        val expected = (level(1, Hormone.PROGESTERONE) - level(26, Hormone.PROGESTERONE)) / 3f

        assertEquals(expected, trend.getValue(Hormone.PROGESTERONE), 1e-5f)
    }

    @Test
    fun trendOfAFlatStretchIsNearZero() {
        val trend = HormoneEstimator.trend(7, 28, days = 1)

        assertEquals(0f, trend.getValue(Hormone.PROGESTERONE), 1e-6f)
    }
}
