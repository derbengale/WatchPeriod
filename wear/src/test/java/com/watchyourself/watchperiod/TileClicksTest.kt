package com.watchyourself.watchperiod

import com.watchyourself.watchperiod.tile.CENTER_ID
import com.watchyourself.watchperiod.tile.DOT_ID_PREFIX
import com.watchyourself.watchperiod.tile.DOUBLE_TAP_WINDOW_MS
import com.watchyourself.watchperiod.tile.TileState
import com.watchyourself.watchperiod.tile.isArmed
import com.watchyourself.watchperiod.tile.nextTileState
import com.watchyourself.watchperiod.tile.repeatDotTapIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TileClicksTest {

    private val noSelection = TileState.NO_SELECTION
    private val dots = 28

    @Test
    fun noClickKeepsState() {
        assertEquals(TileState(0, noSelection), nextTileState(null, 0, noSelection, dots))
        assertEquals(TileState(2, 5), nextTileState(null, 2, 5, dots))
    }

    @Test
    fun centerTapCyclesViewsAndClearsSelection() {
        assertEquals(TileState(1, noSelection), nextTileState(CENTER_ID, 0, 5, dots))
        assertEquals(TileState(2, noSelection), nextTileState(CENTER_ID, 1, noSelection, dots))
        assertEquals(TileState(0, noSelection), nextTileState(CENTER_ID, 2, 3, dots))
    }

    @Test
    fun dotTapSelectsAndDeselectsDay() {
        assertEquals(TileState(0, 7), nextTileState(DOT_ID_PREFIX + 7, 0, noSelection, dots))
        assertEquals(TileState(0, noSelection), nextTileState(DOT_ID_PREFIX + 7, 0, 7, dots))
    }

    @Test
    fun dotTapKeepsCurrentView() {
        assertEquals(TileState(2, 4), nextTileState(DOT_ID_PREFIX + 4, 2, noSelection, dots))
    }

    @Test
    fun unknownOrInvalidClickIsIgnored() {
        assertEquals(TileState(1, 5), nextTileState("something", 1, 5, dots))
        assertEquals(TileState(1, 5), nextTileState(DOT_ID_PREFIX + "x", 1, 5, dots))
        assertEquals(TileState(1, 5), nextTileState(DOT_ID_PREFIX + 28, 1, 5, dots))
        assertEquals(TileState(1, 5), nextTileState(DOT_ID_PREFIX + -1, 1, 5, dots))
    }

    @Test
    fun staleSelectionAndModeAreSanitized() {
        assertEquals(TileState(0, noSelection), nextTileState(null, 0, 42, dots))
        assertEquals(TileState(1, noSelection), nextTileState(null, 7, noSelection, dots))
        assertEquals(TileState(2, noSelection), nextTileState(null, -1, noSelection, dots))
        assertEquals(TileState(0, noSelection), nextTileState(null, 0, noSelection, 0))
    }

    @Test
    fun repeatDotTapOnlyReportsTheSelectedDot() {
        assertEquals(5, repeatDotTapIndex(DOT_ID_PREFIX + 5, 5))
        assertNull(repeatDotTapIndex(DOT_ID_PREFIX + 5, 6))
        assertNull(repeatDotTapIndex(DOT_ID_PREFIX + 5, TileState.NO_SELECTION))
        assertNull(repeatDotTapIndex(CENTER_ID, 5))
        assertNull(repeatDotTapIndex(null, 5))
        assertNull(repeatDotTapIndex(DOT_ID_PREFIX + "x", 5))
    }

    @Test
    fun centerStaysArmedOnlyForTheDoubleTapWindow() {
        val tap = 1_700_000_000_000L
        assertTrue(isArmed(tap, tap))
        assertTrue(isArmed(tap, tap + DOUBLE_TAP_WINDOW_MS))
        assertFalse(isArmed(tap, tap + DOUBLE_TAP_WINDOW_MS + 1))
        // Not armed at all, or a clock that jumped back, must never look like a double tap.
        assertFalse(isArmed(0L, tap))
        assertFalse(isArmed(tap + 5, tap))
    }
}
