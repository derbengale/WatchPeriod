package com.watchyourself.watchperiod.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import java.time.format.DateTimeFormatter
import com.watchyourself.watchperiod.domain.Hormone
import com.watchyourself.watchperiod.domain.HormoneEstimator
import com.watchyourself.watchperiod.domain.TemperatureEstimator
import com.watchyourself.watchperiod.domain.formatShift
import kotlin.math.atan2
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.watchyourself.watchperiod.domain.CycleState
import com.watchyourself.watchperiod.domain.DotKind
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

val PeriodRed = Color(0xFFFF5470)
val PredictedPink = Color(0x88FF5470)
val DotGrey = Color(0xFF3C3C44)
val FollicularGreen = Color(0xFF5BC47A)
val OvulationBlue = Color(0xFF4DB8FF)
val LutealPurple = Color(0xFFA67CFF)
val PremenstrualOrange = Color(0xFFFFA040)

/** Ring radius as a fraction of the smaller side – shared by the ring and the tile. */
const val RingRadiusFraction = 0.84f

/** Rotary pixels that move the selection by one cycle day. */
private const val RotaryStepPx = 48f

/**
 * Outer rim (fraction of the ring radius) where touches never select a day or orbit, so the
 * surrounding pager can swipe sideways to the menu.
 */
private const val DeadZoneFraction = 0.88f

/** Dot diameter for a ring of [minSide] with [n] cycle days – shared by the ring and the tile. */
fun ringDotDiameter(minSide: Float, n: Int): Float = minSide / (if (n > 35) 24f else 18f)

/** Dot color per cycle day – shared by the ring and the tile. */
fun dotColor(kind: DotKind): Color = when (kind) {
    DotKind.PERIOD -> PeriodRed
    DotKind.PREDICTED -> PredictedPink
    DotKind.FOLLICULAR -> FollicularGreen
    DotKind.OVULATION -> OvulationBlue
    DotKind.LUTEAL -> LutealPurple
    DotKind.PREMENSTRUAL -> PremenstrualOrange
    DotKind.NORMAL -> DotGrey
}

fun phaseName(kind: DotKind): String = when (kind) {
    DotKind.PERIOD, DotKind.PREDICTED -> "Menstruation"
    DotKind.FOLLICULAR -> "Follikelphase"
    DotKind.OVULATION -> "Eisprung"
    DotKind.LUTEAL -> "Lutealphase"
    DotKind.PREMENSTRUAL -> "Späte Lutealphase"
    DotKind.NORMAL -> ""
}

/** Dots on a circle (same geometry as the Python prototype), starting at 12 o'clock. */
@Composable
fun CycleRing(
    state: CycleState,
    modifier: Modifier = Modifier,
    onSelectedChange: (Int?) -> Unit = {},
    visibleHormones: List<Hormone> = Hormone.defaultVisible.toList(),
    onHormoneClick: ((Hormone) -> Unit)? = null,
    /** Long press on the ring: opens the day menu for that cycle day (offset from the cycle start). */
    onDayLongPress: ((Int) -> Unit)? = null,
    /** Clear swipe to the right on the ring: opens the menu page. */
    onSwipeRight: (() -> Unit)? = null,
) {
    var selected by remember { mutableStateOf<Int?>(null) }
    androidx.compose.runtime.LaunchedEffect(selected) { onSelectedChange(selected) }
    val n0 = state.dots.size

    // The crown changes the selected day – one day per rotary notch. The ring must own the
    // focus to receive rotary events, so request it when the home screen appears.
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    var rotaryAccumulator by remember { mutableFloatStateOf(0f) }
    fun crownStep(delta: Int) {
        val base = selected ?: state.todayIndex.coerceAtLeast(0)
        selected = (base + delta).coerceAtLeast(0)
    }
    fun indexAt(p: Offset, w: Float, h: Float): Int {
        val a = atan2((p.y - h / 2f).toDouble(), (p.x - w / 2f).toDouble())
        val frac = (a + PI / 2) / (2 * PI)
        return (((frac * n0).roundToInt() % n0) + n0) % n0
    }
    // Closest absolute day offset (may exceed one cycle) that sits on ring position idx.
    fun nearest(idx: Int): Int {
        val cur = selected ?: return idx
        val base = Math.floorDiv(cur, n0) * n0
        return listOf(base - n0 + idx, base + idx, base + n0 + idx)
            .filter { it >= 0 }.minByOrNull { kotlin.math.abs(it - cur) } ?: idx
    }
    var lastIdx by remember { mutableIntStateOf(0) }
    var orbiting by remember { mutableStateOf(true) }
    // Center view, cycled by tapping: 0 = summary, 1 = hormone chart, 2 = mood, 3 = temperature
    var centerMode by remember { mutableIntStateOf(0) }
    Box(
        modifier.fillMaxWidth().aspectRatio(1f)
            .pointerInput(n0) {
                fun inCenter(p: Offset): Boolean {
                    val dx = p.x - size.width / 2f
                    val dy = p.y - size.height / 2f
                    val r = min(size.width, size.height) / 2f * 0.6f
                    return dx * dx + dy * dy < r * r
                }
                fun inDeadZone(p: Offset): Boolean {
                    val dx = p.x - size.width / 2f
                    val dy = p.y - size.height / 2f
                    val r = min(size.width, size.height) / 2f * DeadZoneFraction
                    return dx * dx + dy * dy > r * r
                }
                fun dotTap(p: Offset) {
                    val i = indexAt(p, size.width.toFloat(), size.height.toFloat())
                    selected = if (selected != null && selected!!.mod(n0) == i) null else nearest(i)
                }
                detectTapGestures(
                    onTap = { p ->
                        if (!inDeadZone(p)) {
                            if (inCenter(p)) centerMode = (centerMode + 1) % 4 else dotTap(p)
                        }
                    },
                    onDoubleTap = { p ->
                        if (!inDeadZone(p)) {
                            if (inCenter(p)) selected = null else dotTap(p)
                        }
                    },
                    onLongPress = { p ->
                        if (!inDeadZone(p) && !inCenter(p) && onDayLongPress != null) {
                            val offset = nearest(indexAt(p, size.width.toFloat(), size.height.toFloat()))
                            selected = offset
                            onDayLongPress.invoke(offset)
                        }
                    },
                )
            }
            .pointerInput(n0, onSwipeRight) {
                // Drags that start on the ring band orbit through the days. A clear swipe to the
                // right opens the menu instead. Drags starting in the outer rim (dead zone) are
                // not consumed at all, so the surrounding pager can swipe sideways.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val radius = min(size.width, size.height) / 2f
                    val dist = hypot(
                        (down.position.x - size.width / 2f).toDouble(),
                        (down.position.y - size.height / 2f).toDouble(),
                    )
                    if (dist > radius * DeadZoneFraction) return@awaitEachGesture

                    val slop = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                        ?: return@awaitEachGesture

                    var totalDx = slop.position.x - down.position.x
                    var totalDy = slop.position.y - down.position.y
                    var swipeTriggered = false
                    val selectionAtStart = selected
                    orbiting = dist >= radius * 0.6
                    if (orbiting) {
                        lastIdx = indexAt(slop.position, size.width.toFloat(), size.height.toFloat())
                        selected = nearest(lastIdx)
                    }
                    drag(slop.id) { change ->
                        change.consume()
                        val delta = change.positionChange()
                        totalDx += delta.x
                        totalDy += delta.y
                        if (onSwipeRight != null && !swipeTriggered &&
                            totalDx > size.width * 0.25f && totalDx > kotlin.math.abs(totalDy) * 2f
                        ) {
                            // Menu gesture wins: undo the orbit movement of this drag.
                            swipeTriggered = true
                            selected = selectionAtStart
                            onSwipeRight()
                            return@drag
                        }
                        if (!orbiting || swipeTriggered) return@drag
                        val i = indexAt(change.position, size.width.toFloat(), size.height.toFloat())
                        var d = i - lastIdx
                        if (d > n0 / 2) d -= n0 else if (d < -n0 / 2) d += n0
                        lastIdx = i
                        selected = ((selected ?: i) + d).coerceAtLeast(0)
                    }
                }
            }
            .onRotaryScrollEvent { event ->
                rotaryAccumulator += event.verticalScrollPixels + event.horizontalScrollPixels
                while (rotaryAccumulator >= RotaryStepPx) {
                    rotaryAccumulator -= RotaryStepPx
                    crownStep(1)
                }
                while (rotaryAccumulator <= -RotaryStepPx) {
                    rotaryAccumulator += RotaryStepPx
                    crownStep(-1)
                }
                true
            }
            .focusRequester(focusRequester)
            .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val n = state.dots.size
            val minSide = min(size.width, size.height)
            val radius = minSide / 2f * RingRadiusFraction
            val dot = ringDotDiameter(minSide, n)
            val center = Offset(size.width / 2f, size.height / 2f)
            for (i in 0 until n) {
                val angle = -PI / 2 + 2 * PI * i / n
                val pos = Offset(
                    center.x + radius * cos(angle).toFloat(),
                    center.y + radius * sin(angle).toFloat(),
                )
                val color = dotColor(state.dots[i])
                val predicted = state.dots[i] == DotKind.PREDICTED
                if (selected != null && i == selected!!.mod(n)) {
                    // A calculated period keeps its hollow look, even when selected.
                    if (predicted) {
                        drawCircle(color, dot * 0.6f, pos, style = Stroke(width = dot * 0.2f))
                    } else {
                        drawCircle(color, dot * 0.6f, pos)
                    }
                    drawCircle(Color.White, dot * 0.6f, pos, style = Stroke(width = dot * 0.15f))
                } else if (predicted) {
                    // Hollow dot = a period day the app calculated, not one you logged.
                    drawCircle(color, dot / 2f, pos, style = Stroke(width = dot * 0.2f))
                } else {
                    drawCircle(color, dot / 2f, pos)
                }
                if (i == state.todayIndex) {
                    drawCircle(Color.White, dot * 0.85f, pos, style = Stroke(width = dot * 0.25f))
                }
            }
        }
        Column(
            Modifier.padding(horizontal = if (centerMode == 1) 34.dp else 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (centerMode == 2 && state.todayIndex >= 0) {
                val dayIdx = selected?.mod(n0) ?: state.todayIndex
                val kind = state.dots[dayIdx].let { if (it == DotKind.PREDICTED) DotKind.PERIOD else it }
                val m = com.watchyourself.watchperiod.domain.MoodEstimator.forPhase(kind)
                DayDateLine(state, selected ?: state.todayIndex)
                Text(m.emoji, style = MaterialTheme.typography.displayMedium, maxLines = 1)
                Text(
                    "Stimmung: ${m.mood}",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                Text(
                    "Reizb.: ${m.irritability}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                m.libido?.let { libido ->
                    Text(
                        "Libido: $libido",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            } else if (centerMode == 1 && state.todayIndex >= 0) {
                val dayIdx = selected?.mod(n0) ?: state.todayIndex
                val kind = state.dots[dayIdx]
                val phase = phaseName(if (kind == DotKind.PREDICTED) DotKind.PERIOD else kind) +
                    (if (kind == DotKind.PREDICTED) " · errechnet" else "")
                DayDateLine(state, selected ?: state.todayIndex)
                Text(
                    "Tag ${dayIdx + 1} · $phase",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                HormoneBarChart(
                    HormoneEstimator.levels(dayIdx, state.cycleLength),
                    trend = HormoneEstimator.trend(dayIdx, state.cycleLength),
                    visible = visibleHormones,
                    chartHeight = 38.dp,
                    horizontalPadding = 0.dp,
                    spacing = 3.dp,
                    onHormoneClick = onHormoneClick,
                    onChartTap = { centerMode = (centerMode + 1) % 4 },
                )
            } else if (centerMode == 3 && state.todayIndex >= 0) {
                val dayIdx = selected?.mod(n0) ?: state.todayIndex
                val kind = state.dots[dayIdx]
                DayDateLine(state, selected ?: state.todayIndex)
                val estimate = TemperatureEstimator.estimate(dayIdx, state.cycleLength, kind)
                Text(
                    estimate.valueText,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                Text(
                    "Körpertemperatur · geschätzt",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                Text(
                    "Diff. Mann ${formatShift(TemperatureEstimator.differenceToMale(estimate.celsius))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            } else {
                if (selected == null) DayDateLine(state, state.todayIndex)
                val (title, subtitle) = selected?.let { dayInfo(state, it) } ?: summary(state)
                Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Calendar date of the shown cycle day – shown above every center view (nothing if unknown). */
@Composable
private fun DayDateLine(state: CycleState, offset: Int) {
    val date = state.dateForOffset(offset) ?: return
    Text(
        dayFormat.format(date),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

private val dayFormat = DateTimeFormatter.ofPattern("d. MMM")

/** Info for a tapped cycle day (0-based index). */
fun dayInfo(state: CycleState, offset: Int): Pair<String, String> {
    val n = state.cycleLength
    val index = offset % n
    val cycleNo = offset / n
    val date = state.cycleStart?.plusDays(offset.toLong())
    val raw = state.dots[index]
    val predicted = raw == DotKind.PREDICTED
    val kind = if (predicted) DotKind.PERIOD else raw
    val phase = phaseName(kind) +
        (if (predicted) " · errechnet" else if (raw == DotKind.PERIOD) " · geloggt" else "")
    val extra = buildList {
        if (date != null) add(dayFormat.format(date))
        if (offset == state.todayIndex && state.cyclesSinceLogged == 0) add("Heute")
        if (state.cyclesSinceLogged > 0) add("Zyklus errechnet")
        if (cycleNo > 0) add("Zyklus +$cycleNo")
    }.joinToString(" · ")
    return "Tag ${index + 1}" to (if (extra.isEmpty()) phase else "$extra\n$phase")
}

fun summary(state: CycleState): Pair<String, String> {
    if (state.todayIndex < 0) return "Keine Daten" to "Periode eintragen"
    val raw = state.dots[state.todayIndex]
    val predicted = raw == DotKind.PREDICTED
    val phase = phaseName(if (predicted) DotKind.PERIOD else raw)
    if (predicted) {
        val overdue = state.daysUntilNext?.let { -it } ?: 0
        val whenText = when {
            overdue > 1 -> "seit $overdue Tagen überfällig"
            overdue == 1 -> "seit gestern überfällig"
            else -> "heute erwartet"
        }
        return "Tag ${state.todayIndex + 1}" to
            "Menstruation errechnet\n$whenText · im Tagesmenü bestätigen"
    }
    if (state.periodOngoing) {
        return "Tag ${state.periodDay}" to "Menstruation · geloggt"
    }
    val until = state.daysUntilNext ?: 0
    val next = when {
        until > 1 -> "Nächste in $until Tagen"
        until == 1 -> "Morgen"
        until == 0 -> "Heute erwartet"
        else -> "${-until} Tage überfällig"
    }
    return "Tag ${state.todayIndex + 1}" to "$phase\n$next"
}
