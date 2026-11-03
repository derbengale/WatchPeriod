package com.watchyourself.watchperiod.tile

internal const val CENTER_ID = "center"
internal const val DOT_ID_PREFIX = "dot_"

/** How long after a center tap a second tap still counts as a double tap. */
internal const val DOUBLE_TAP_WINDOW_MS = 800L

/**
 * A tap in the center arms the tile for one more tap: while the tile on screen is armed, its
 * center clickable is a LaunchAction, so a fast second tap opens the app. The tile host starts
 * the activity – this service may not do that itself on Wear OS.
 */
fun isArmed(armedAt: Long, now: Long): Boolean =
    armedAt > 0L && now >= armedAt && now - armedAt <= DOUBLE_TAP_WINDOW_MS

/** What the tile currently shows: the center view and the selected cycle day. */
data class TileState(val mode: Int, val selected: Int) {
    companion object {
        /** No cycle day selected (or the selection is stale). */
        const val NO_SELECTION = -1

        /** 0 = summary, 1 = hormone chart, 2 = mood. */
        const val MODES = 3
    }
}

/**
 * Applies the click that opened the tile, mirroring the app: tapping the center cycles through
 * the views and clears the selection, tapping a dot selects that day (tapping it again clears it).
 */
fun nextTileState(lastClickableId: String?, mode: Int, selected: Int, dotCount: Int): TileState {
    val safeMode = ((mode % TileState.MODES) + TileState.MODES) % TileState.MODES
    val safeSelected = if (selected in 0 until dotCount) selected else TileState.NO_SELECTION
    val unchanged = TileState(safeMode, safeSelected)
    val id = lastClickableId ?: return unchanged
    if (id == CENTER_ID) {
        return TileState((safeMode + 1) % TileState.MODES, TileState.NO_SELECTION)
    }
    if (!id.startsWith(DOT_ID_PREFIX)) return unchanged
    val index = id.removePrefix(DOT_ID_PREFIX).toIntOrNull() ?: return unchanged
    if (index !in 0 until dotCount) return unchanged
    return TileState(safeMode, if (index == safeSelected) TileState.NO_SELECTION else index)
}

/** Index of the dot that is already selected and was tapped again – null otherwise. */
fun repeatDotTapIndex(lastClickableId: String?, selected: Int): Int? {
    val id = lastClickableId ?: return null
    if (!id.startsWith(DOT_ID_PREFIX)) return null
    val index = id.removePrefix(DOT_ID_PREFIX).toIntOrNull() ?: return null
    return if (index == selected) index else null
}
