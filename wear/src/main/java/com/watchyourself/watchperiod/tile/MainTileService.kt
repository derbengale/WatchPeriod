package com.watchyourself.watchperiod.tile

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.graphics.toArgb
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.degrees
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Arc
import androidx.wear.protolayout.LayoutElementBuilders.ArcLine
import androidx.wear.protolayout.LayoutElementBuilders.ArcLayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.ModifiersBuilders.ArcModifiers
import androidx.wear.protolayout.ModifiersBuilders.Background
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.Corner
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.ModifiersBuilders.Semantics
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.EventBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import androidx.wear.tiles.tooling.preview.Preview
import androidx.wear.tiles.tooling.preview.TilePreviewData
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.watchyourself.watchperiod.data.PeriodRepository
import com.watchyourself.watchperiod.domain.CycleCalculator
import com.watchyourself.watchperiod.domain.CycleState
import com.watchyourself.watchperiod.domain.DotKind
import com.watchyourself.watchperiod.domain.Hormone
import com.watchyourself.watchperiod.domain.HormoneEstimator
import com.watchyourself.watchperiod.domain.MoodEstimator
import com.watchyourself.watchperiod.domain.Period
import com.watchyourself.watchperiod.domain.Settings
import com.watchyourself.watchperiod.presentation.RingRadiusFraction
import com.watchyourself.watchperiod.presentation.dayInfo
import com.watchyourself.watchperiod.presentation.dotColor
import com.watchyourself.watchperiod.presentation.hormoneColor
import com.watchyourself.watchperiod.presentation.phaseName
import com.watchyourself.watchperiod.presentation.ringDotDiameter
import com.watchyourself.watchperiod.presentation.summary
import com.watchyourself.watchperiod.presentation.trendColor
import com.watchyourself.watchperiod.presentation.trendGlyph
import java.time.LocalDate

private const val RESOURCES_VERSION = "0"
private const val REFRESH_MS = 60L * 60L * 1000L
private const val PREFS = "tile"
private const val KEY_MODE = "mode"
private const val KEY_SELECTED = "selected"
private const val KEY_ARMED_AT = "armed_at"

private const val APP_PACKAGE = "com.watchyourself.watchperiod"
private const val APP_ACTIVITY = "com.watchyourself.watchperiod.presentation.MainActivity"
private const val EXTRA_DAY = "watchperiod.day"

// Wear Material3 type scale – the same tokens the app's MaterialTheme uses.
private const val SIZE_TITLE = 18f // titleLarge
private const val SIZE_BODY = 12f // bodySmall
private const val SIZE_LABEL = 13f // labelSmall
private const val SIZE_LABEL_SMALL = 9f // labelSmall, compacted
private const val SIZE_DISPLAY = 30f // displayMedium

// Spacing copied from CycleRing and HormoneBarChart.
private const val TEXT_INSET_DP = 48f // 2 × 24dp column padding, same as the app ring
private const val CHART_INSET_DP = 68f // 2 × 34dp column padding
private const val CHART_HEIGHT_DP = 38f
private const val CHART_ARROW_DP = 11f // fixed slot for the trend arrow – keeps bars in place
private const val CHART_BAR_AREA_DP = CHART_HEIGHT_DP - CHART_ARROW_DP
private const val CHART_SPACING_DP = 3f
private const val CHART_COMPACT_SPACING_DP = 2f
private const val CHART_CORNER_DP = 4f
private const val DOT_ARC_DEGREES = 0.3f

// Halo widths used by the app ring (white ring behind today's / the selected dot).
private const val HALO_SELECTED = 1.35f
private const val HALO_TODAY = 1.95f

/**
 * Tappable square in the middle. The corners must stay inside the ring band, otherwise the
 * clickable swallows the taps on the dots: 0.5 × screen has a corner distance of 0.354 × screen,
 * while the innermost dot edge sits at ~0.366 × screen.
 */
private const val CENTER_HIT_FRACTION = 0.5f

private const val WHITE = 0xFFFFFFFF.toInt()
private const val MUTED = 0xFFB0B0B8.toInt()

/** Nearly transparent line that only exists to give each dot a tappable sector. */
private const val TAP_AREA = 0x02000000

class MainTileService : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val repo = PeriodRepository(this)
        val settings = repo.settings()
        val state = repo.state()
        val storedMode = prefs.getInt(KEY_MODE, 0)
        val storedSelected = prefs.getInt(KEY_SELECTED, TileState.NO_SELECTION)
        val clickedId = requestParams.currentState.lastClickableId
        val now = System.currentTimeMillis()
        val storedArmedAt = prefs.getLong(KEY_ARMED_AT, 0L)

        // Every tap is a plain LoadAction, so all of them reach this service: the center cycles
        // the view, a dot selects its day. In addition, a center tap *arms* the tile for
        // DOUBLE_TAP_WINDOW_MS – the next render then puts a LaunchAction on the center, so a
        // second, fast tap opens the app. The host launches the activity, because a tile service
        // in the background may not start it itself on Wear OS.
        val next = nextTileState(clickedId, storedMode, storedSelected, state.dots.size)
        val nextArmedAt = when (clickedId) {
            CENTER_ID -> now
            null -> storedArmedAt // a plain refresh keeps the arming until it expires
            else -> 0L // a dot tap clears the selection and disarms
        }
        val armed = isArmed(nextArmedAt, now)
        val armedAtToStore = if (armed) nextArmedAt else 0L
        if (next.mode != storedMode || next.selected != storedSelected || armedAtToStore != storedArmedAt) {
            prefs.edit()
                .putInt(KEY_MODE, next.mode)
                .putInt(KEY_SELECTED, next.selected)
                .putLong(KEY_ARMED_AT, armedAtToStore)
                .apply()
        }
        if (clickedId == CENTER_ID) scheduleDisarm(now)
        return Futures.immediateFuture(
            tile(
                requestParams,
                state,
                next.mode,
                next.selected,
                visible = settings.visibleHormones,
                centerOpensApp = armed,
            )
        )
    }

    override fun onTileResourcesRequest(requestParams: ResourcesRequest): ListenableFuture<Resources> =
        Futures.immediateFuture(resources())

    /** Asks the tile host for a fresh tile, so the center falls back to the view cycle. */
    private fun requestRefresh() {
        val context = applicationContext
        runCatching { TileService.getUpdater(context).requestUpdate(MainTileService::class.java) }
    }

    /**
     * Drops the arming once the double-tap window has passed. Without this a tile that stays on
     * screen would keep the LaunchAction on the center and never cycle the views again.
     */
    private fun scheduleDisarm(armedAt: Long) {
        val context = applicationContext
        Handler(Looper.getMainLooper()).postDelayed({
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            // A newer tap re-armed the tile in the meantime – keep that one alive.
            if (prefs.getLong(KEY_ARMED_AT, 0L) != armedAt) return@postDelayed
            prefs.edit().putLong(KEY_ARMED_AT, 0L).apply()
            requestRefresh()
        }, DOUBLE_TAP_WINDOW_MS + 100L)
    }

    override fun onTileEnterEvent(event: EventBuilders.TileEnterEvent) {
        // The tile is visible again (e.g. after using the app): render it without the arming.
        requestRefresh()
    }

    override fun onTileLeaveEvent(event: EventBuilders.TileLeaveEvent) {
        // Off screen – drop the arming so the next visit starts with the view cycle again.
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(KEY_ARMED_AT, 0L).apply()
    }
}

private fun resources(): Resources = Resources.Builder().setVersion(RESOURCES_VERSION).build()

private fun text(s: String, size: Float, color: Int = WHITE, maxLines: Int = 3): LayoutElement =
    Text.Builder()
        .setText(s)
        .setMaxLines(maxLines)
        .setMultilineAlignment(LayoutElementBuilders.TEXT_ALIGN_CENTER)
        .setFontStyle(
            FontStyle.Builder()
                .setSize(sp(size))
                .setColor(argb(color))
                .setWeight(LayoutElementBuilders.FONT_WEIGHT_MEDIUM)
                .build()
        )
        .build()

private fun dotLine(length: Float, thickness: Float, color: Int, modifiers: ArcModifiers): ArcLayoutElement =
    ArcLine.Builder()
        .setLength(degrees(length))
        .setThickness(dp(thickness))
        .setColor(argb(color))
        .setModifiers(modifiers)
        .build()

/** Round dots on a circle – geometry and highlights mirror `CycleRing` in the app. */
private fun ring(state: CycleState, screen: Float, selected: Int): LayoutElement {
    val n = state.dots.size
    val radius = screen / 2f * RingRadiusFraction
    val dot = ringDotDiameter(screen, n)
    // The arc draws its content inside `size - 2 * padding` and the thickest line (today's halo)
    // touches that edge, so the dot centres end up half a halo width further in. Compensate it,
    // otherwise the whole ring sits ~13 % closer to the middle than the app's ring.
    val inset = (screen / 2f - radius - dot * HALO_TODAY / 2f).coerceAtLeast(0f)
    val slot = 360f / n
    // Each day owns its whole sector: an invisible band left and right of the dot. An ArcLine of
    // 0.3° is only ~1 px long, so the painted dot alone is far too small to hit with a finger.
    val halfSlot = (slot - DOT_ARC_DEGREES) / 2f
    // Anchor at the start of the content, so day 1's centre sits exactly on 12 o'clock – the same
    // orientation the app ring uses. Without this the arc content is centred on 0°, which puts
    // day 1 at 6 o'clock. The content starts half a slot early because it begins with a band.
    val arc = Arc.Builder()
        .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START)
        .setAnchorAngle(degrees(-slot / 2f))
        .setModifiers(
            Modifiers.Builder()
                .setPadding(Padding.Builder().setAll(dp(inset)).build())
                .build()
        )
    for (i in 0 until n) {
        val isSelected = i == selected
        val isToday = i == state.todayIndex
        // Re-tapping the selected dot opens that day's menu in the app.
        val action: ActionBuilders.Action =
            if (isSelected) dayLaunchAction(state, i) else ActionBuilders.LoadAction.Builder().build()
        val clickable = Clickable.Builder()
            .setId(DOT_ID_PREFIX + i)
            .setOnClick(action)
            .build()
        val modifiers = ArcModifiers.Builder()
            .setClickable(clickable)
            .setSemantics(Semantics.Builder().setContentDescription("Tag ${i + 1}").build())
            .build()
        arc.addContent(dotLine(halfSlot, dot * HALO_TODAY, TAP_AREA, modifiers))
        // The app draws a white ring behind today's and the selected dot.
        if (isSelected || isToday) {
            val halo = dot * if (isSelected) HALO_SELECTED else HALO_TODAY
            arc.addContent(dotLine(DOT_ARC_DEGREES, halo, WHITE, modifiers))
        }
        val body = dot * if (isSelected) 1.2f else 1f
        arc.addContent(dotLine(DOT_ARC_DEGREES, body, dotColor(state.dots[i]).toArgb(), modifiers))
        arc.addContent(dotLine(halfSlot, dot * HALO_TODAY, TAP_AREA, modifiers))
    }
    return arc.build()
}

/** Launch action that opens the app on the day menu for this cycle day. */
private fun dayLaunchAction(state: CycleState, index: Int): ActionBuilders.Action {
    val date = state.dateForOffset(index)
    if (date == null || date.isAfter(LocalDate.now())) return ActionBuilders.LoadAction.Builder().build()
    return appLaunchAction(date.toString())
}

private fun appLaunchAction(day: String? = null): ActionBuilders.Action {
    val activity = ActionBuilders.AndroidActivity.Builder()
        .setPackageName(APP_PACKAGE)
        .setClassName(APP_ACTIVITY)
    if (day != null) {
        activity.addKeyToExtraMapping(
            EXTRA_DAY,
            ActionBuilders.AndroidStringExtra.Builder().setValue(day).build(),
        )
    }
    return ActionBuilders.LaunchAction.Builder().setAndroidActivity(activity.build()).build()
}

private fun bars(state: CycleState, day: Int, screen: Float, visible: Set<Hormone>): LayoutElement {
    val levels = HormoneEstimator.levels(day, state.cycleLength)
    val trend = HormoneEstimator.trend(day, state.cycleLength)
    val shown = Hormone.ordered(visible).ifEmpty { Hormone.ordered(Hormone.defaultVisible) }
    val compact = shown.size > 6
    val spacing = if (compact) CHART_COMPACT_SPACING_DP else CHART_SPACING_DP
    val labelSize = if (compact) SIZE_LABEL_SMALL else SIZE_LABEL
    val count = shown.size
    val barWidth = (screen - CHART_INSET_DP - (count - 1) * spacing) / count
    val row = Row.Builder().setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_BOTTOM)
    val labels = Row.Builder()
    shown.forEachIndexed { i, h ->
        val v = (levels[h] ?: 0f).coerceIn(0.03f, 1f)
        val rate = trend[h] ?: 0f
        if (i > 0) {
            row.addContent(Spacer.Builder().setWidth(dp(spacing)).build())
            labels.addContent(Spacer.Builder().setWidth(dp(spacing)).build())
        }
        // Every bar cell has the same fixed height: arrow slot on top, then the bar area.
        // Without the fixed height the whole chart and the text above it jumped around.
        val arrow = Box.Builder()
            .setWidth(dp(barWidth))
            .setHeight(dp(CHART_ARROW_DP))
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .addContent(text(trendGlyph(rate), SIZE_LABEL_SMALL, trendColor(rate).toArgb(), maxLines = 1))
            .build()
        val bar = Box.Builder()
            .setWidth(dp(barWidth))
            .setHeight(dp((CHART_BAR_AREA_DP * v).coerceAtLeast(1f)))
            .setModifiers(
                Modifiers.Builder()
                    .setBackground(
                        Background.Builder().setColor(argb(hormoneColor(h).toArgb()))
                            .setCorner(Corner.Builder().setRadius(dp(CHART_CORNER_DP)).build()).build()
                    ).build()
            ).build()
        val barArea = Box.Builder()
            .setWidth(dp(barWidth))
            .setHeight(dp(CHART_BAR_AREA_DP))
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_BOTTOM)
            .addContent(bar)
            .build()
        row.addContent(
            Column.Builder()
                .setWidth(dp(barWidth))
                .setHeight(dp(CHART_HEIGHT_DP))
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .addContent(arrow)
                .addContent(barArea)
                .build()
        )
        labels.addContent(
            Box.Builder().setWidth(dp(barWidth))
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .addContent(text(h.short, labelSize, hormoneColor(h).toArgb(), maxLines = 1)).build()
        )
    }
    return Column.Builder()
        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
        .addContent(text("Tag ${day + 1} · ${phaseName(state.dots[day])}", SIZE_LABEL, MUTED, maxLines = 1))
        .addContent(Spacer.Builder().setHeight(dp(CHART_SPACING_DP)).build())
        .addContent(row.build())
        .addContent(labels.build())
        .build()
}

private fun centerContent(
    state: CycleState,
    mode: Int,
    selected: Int,
    screen: Float,
    visible: Set<Hormone>,
): LayoutElement {
    val col = Column.Builder().setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
    val hasData = state.todayIndex >= 0
    val day = if (selected in state.dots.indices) selected else state.todayIndex
    if (hasData && mode == 1) return col.addContent(bars(state, day, screen, visible)).build()
    if (hasData && mode == 2) {
        val kind = state.dots[day].let { if (it == DotKind.PREDICTED) DotKind.PERIOD else it }
        val m = MoodEstimator.forPhase(kind)
        col
            .addContent(text(m.emoji, SIZE_DISPLAY, maxLines = 1))
            .addContent(text("Stimmung: ${m.mood}", SIZE_LABEL, maxLines = 1))
            .addContent(text("Reizb.: ${m.irritability}", SIZE_LABEL, MUTED, maxLines = 1))
        m.libido?.let { libido ->
            col.addContent(text("Libido: $libido", SIZE_LABEL, MUTED, maxLines = 1))
        }
        return col.build()
    }
    val showHint = selected in state.dots.indices
    val (title, subtitle) = if (showHint) dayInfo(state, selected) else summary(state)
    val content = col
        .addContent(text(title, SIZE_TITLE))
        .addContent(text(subtitle, SIZE_BODY, MUTED))
    if (showHint) content.addContent(text("Punkt nochmal: Menü", SIZE_LABEL, MUTED, maxLines = 1))
    return content.build()
}

private fun tile(
    requestParams: RequestBuilders.TileRequest,
    state: CycleState,
    mode: Int,
    selected: Int,
    visible: Set<Hormone> = Hormone.defaultVisible,
    centerOpensApp: Boolean = false,
): TileBuilders.Tile =
    buildTile(
        requestParams.deviceConfiguration.screenWidthDp.toFloat(),
        state,
        mode,
        selected,
        visible,
        centerOpensApp,
    )

/** Pure render path – no service context needed, so it can be unit tested. */
internal fun buildTile(
    screenWidthDp: Float,
    state: CycleState,
    mode: Int,
    selected: Int,
    visible: Set<Hormone> = Hormone.defaultVisible,
    centerOpensApp: Boolean = false,
): TileBuilders.Tile {
    val screen = screenWidthDp
    // Armed by the previous tap: the next tap in the middle opens the app (double tap).
    val click = Clickable.Builder()
        .setId(CENTER_ID)
        .setOnClick(
            if (centerOpensApp) appLaunchAction() else ActionBuilders.LoadAction.Builder().build()
        )
        .build()

    // Invisible tap target in the middle – small enough to leave the dots tappable.
    val hitArea = Box.Builder()
        .setWidth(dp(screen * CENTER_HIT_FRACTION))
        .setHeight(dp(screen * CENTER_HIT_FRACTION))
        .setModifiers(Modifiers.Builder().setClickable(click).build())
        .build()

    // Content on top of the tap target: draws over the whole circle but stays click-through,
    // so texts and charts keep the app's 24dp side padding instead of being squeezed.
    val content = Box.Builder()
        .setWidth(expand())
        .setHeight(expand())
        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
        .setModifiers(
            Modifiers.Builder().setPadding(Padding.Builder().setStart(dp(TEXT_INSET_DP / 2f)).setEnd(dp(TEXT_INSET_DP / 2f)).build()).build()
        )
        .addContent(centerContent(state, mode, selected, screen, visible))
        .build()

    val root = Box.Builder()
        .setWidth(expand())
        .setHeight(expand())
        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
        .addContent(ring(state, screen, selected))
        .addContent(hitArea)
        .addContent(content)
        .build()

    return TileBuilders.Tile.Builder()
        .setResourcesVersion(RESOURCES_VERSION)
        .setFreshnessIntervalMillis(REFRESH_MS)
        .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(root))
        .build()
}

private fun previewState(): CycleState = CycleCalculator.compute(
    listOf(Period(0, LocalDate.now().minusDays(10), null)),
    Settings(),
    LocalDate.now(),
)

@Preview(device = WearDevices.SMALL_ROUND)
@Preview(device = WearDevices.LARGE_ROUND)
fun tilePreview(context: Context) = TilePreviewData({ resources() }) {
    tile(it, previewState(), 0, TileState.NO_SELECTION)
}

@Preview(device = WearDevices.LARGE_ROUND)
fun tilePreviewHormones(context: Context) = TilePreviewData({ resources() }) {
    tile(it, previewState(), 1, TileState.NO_SELECTION)
}

@Preview(device = WearDevices.LARGE_ROUND)
fun tilePreviewSelection(context: Context) = TilePreviewData({ resources() }) {
    tile(it, previewState(), 0, 3)
}
