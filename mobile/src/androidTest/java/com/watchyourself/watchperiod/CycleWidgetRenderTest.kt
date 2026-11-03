package com.watchyourself.watchperiod

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.watchyourself.watchperiod.domain.CycleCalculator
import com.watchyourself.watchperiod.domain.Period
import com.watchyourself.watchperiod.domain.Settings
import com.watchyourself.watchperiod.widget.CycleWidgetProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Smoke test for the home-screen widget renderer – the ring must contain visible dots. */
@RunWith(AndroidJUnit4::class)
class CycleWidgetRenderTest {

    @Test
    fun ringIsDrawnWithTodayHighlight() {
        val today = LocalDate.of(2026, 11, 3)
        val state = CycleCalculator.compute(
            listOf(
                Period(1, today.minusDays(3), null),
                Period(2, today.minusDays(31), today.minusDays(27)),
            ),
            Settings(),
            today,
        )

        val bitmap = CycleWidgetProvider.renderRing(state, 480)

        assertEquals(480, bitmap.width)
        // Day 1 sits at 12 o'clock, today's dot is highlighted with a white core.
        val centre = 240
        val top = (240f - 240f * 0.80f).toInt()
        assertNotEquals(0, bitmap.getPixel(centre, top))
        val todayX = (centre + 240f * 0.80f * kotlin.math.cos(-Math.PI / 2 + 2 * Math.PI * 3 / 28)).toInt()
        val todayY = (centre + 240f * 0.80f * kotlin.math.sin(-Math.PI / 2 + 2 * Math.PI * 3 / 28)).toInt()
        assertEquals(0xFFFFFFFF.toInt(), bitmap.getPixel(todayX, todayY))
    }
}
