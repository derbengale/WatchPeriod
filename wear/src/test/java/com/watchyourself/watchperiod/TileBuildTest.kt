package com.watchyourself.watchperiod

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.tiles.TileBuilders
import com.watchyourself.watchperiod.domain.CycleCalculator
import com.watchyourself.watchperiod.domain.Hormone
import com.watchyourself.watchperiod.domain.Period
import com.watchyourself.watchperiod.domain.Settings
import com.watchyourself.watchperiod.presentation.trendGlyph
import com.watchyourself.watchperiod.tile.TileState
import com.watchyourself.watchperiod.tile.buildTile
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The tile is rendered from protobuf builders only, so the render path runs on the JVM. */
class TileBuildTest {

    private val today = LocalDate.of(2026, 10, 30)
    private val state = CycleCalculator.compute(
        listOf(Period(1, LocalDate.of(2026, 10, 3), null)),
        Settings(),
        today,
    )

    @Test
    fun rendersSummaryView() {
        val tile = buildTile(450f, state, mode = 0, selected = TileState.NO_SELECTION)
        assertNotNull(tile.tileTimeline)
        assertNotNull(tile.resourcesVersion)
        assertTrue(tile.freshnessIntervalMillis > 0)
    }

    @Test
    fun rendersHormoneAndMoodViews() {
        assertNotNull(buildTile(450f, state, mode = 1, selected = TileState.NO_SELECTION).tileTimeline)
        assertNotNull(buildTile(450f, state, mode = 2, selected = TileState.NO_SELECTION).tileTimeline)
    }

    @Test
    fun rendersSelectedDay() {
        assertNotNull(buildTile(450f, state, mode = 0, selected = 3).tileTimeline)
    }

    @Test
    fun rendersWithoutAnyLoggedData() {
        val empty = CycleCalculator.compute(emptyList(), Settings(), today)
        assertNotNull(buildTile(450f, empty, mode = 0, selected = TileState.NO_SELECTION).tileTimeline)
    }

    @Test
    fun rendersOnSmallRoundScreen() {
        assertNotNull(buildTile(192f, state, mode = 0, selected = 1).tileTimeline)
    }

    @Test
    fun ringStartsAtTwelveOClock() {
        val tile = buildTile(450f, state, mode = 0, selected = TileState.NO_SELECTION)
        val timeline = requireNotNull(tile.tileTimeline)
        val layout = requireNotNull(timeline.timelineEntries.first().layout)
        val root = requireNotNull(layout.root)
        val arc = (root as LayoutElementBuilders.Box).contents.first() as LayoutElementBuilders.Arc

        // ARC_ANCHOR_CENTER would put day 1 at 6 o'clock; the app ring starts at 12.
        assertEquals(LayoutElementBuilders.ARC_ANCHOR_START, arc.anchorType?.value)
    }

    @Test
    fun hormoneChartUsesTheConfiguredSelection() {
        val onlyEstrogen = setOf(Hormone.ESTROGEN)
        assertNotNull(buildTile(450f, state, mode = 1, selected = TileState.NO_SELECTION, visible = onlyEstrogen).tileTimeline)
        assertNotNull(buildTile(450f, state, mode = 1, selected = TileState.NO_SELECTION, visible = emptySet()).tileTimeline)
    }

    @Test
    fun centerCyclesUntilTheTileIsArmedForADoubleTap() {
        // Idle tile: the center reaches the service and cycles the view.
        assertTrue(centerAction(buildTile(450f, state, mode = 0, selected = TileState.NO_SELECTION)) is ActionBuilders.LoadAction)

        // One tap ago the service armed the tile, so the second tap opens the app via the host.
        val armed = centerAction(
            buildTile(450f, state, mode = 0, selected = TileState.NO_SELECTION, centerOpensApp = true)
        )
        assertTrue(armed is ActionBuilders.LaunchAction)
        val activity = requireNotNull((armed as ActionBuilders.LaunchAction).androidActivity)
        assertEquals("com.watchyourself.watchperiod", activity.packageName)
        assertEquals("com.watchyourself.watchperiod.presentation.MainActivity", activity.className)
    }

    @Test
    fun trendGlyphsShowDirection() {
        assertEquals("\u2191", trendGlyph(0.06f))
        assertEquals("\u2197", trendGlyph(0.02f))
        assertEquals("", trendGlyph(0.001f))
        assertEquals("\u2198", trendGlyph(-0.02f))
        assertEquals("\u2193", trendGlyph(-0.06f))
    }

    private fun centerAction(tile: TileBuilders.Tile): ActionBuilders.Action {
        val timeline = requireNotNull(tile.tileTimeline)
        val layout = requireNotNull(timeline.timelineEntries.first().layout)
        val root = requireNotNull(layout.root) as LayoutElementBuilders.Box
        val hitArea = root.contents[1] as LayoutElementBuilders.Box
        val clickable = requireNotNull(requireNotNull(hitArea.modifiers).clickable)
        return requireNotNull(clickable.onClick)
    }
}
