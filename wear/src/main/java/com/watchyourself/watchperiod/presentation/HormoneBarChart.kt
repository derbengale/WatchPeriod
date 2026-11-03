package com.watchyourself.watchperiod.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.watchyourself.watchperiod.domain.Hormone
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

fun hormoneColor(h: Hormone): Color = when (h) {
    Hormone.TESTOSTERONE -> Color(0xFF4DB8FF)
    Hormone.FSH -> Color(0xFF5BC47A)
    Hormone.LH -> Color(0xFFFFD34D)
    Hormone.PROGESTERONE -> Color(0xFFA67CFF)
    Hormone.ESTROGEN -> Color(0xFFFF5470)
    Hormone.PROLACTIN -> Color(0xFF4FD1C5)
    Hormone.OXYTOCIN -> Color(0xFFFF8FB1)
    Hormone.INHIBIN_A -> Color(0xFF8FA8C8)
    Hormone.RELAXIN -> Color(0xFFB49CFF)
    Hormone.KISSPEPTIN -> Color(0xFFFFB86B)
    Hormone.CORTISOL -> Color(0xFFD4A017)
    Hormone.PROSTAGLANDIN -> Color(0xFFEF5350)
}

/** Mean daily change that maps to the steepest angle (up / down) and the strongest colour. */
const val TrendFullScale = 0.06f

/** Smaller changes count as "unchanged" – the arrow stays hidden. */
const val TrendMinChange = 0.005f

/** Edge length of the tiny trend arrow above a bar. */
private val trendArrowSize = 8.dp

private val TrendRiseWeak = Color(0xFF8FE3A6)
private val TrendRiseStrong = Color(0xFF00E676)
private val TrendFallWeak = Color(0xFFFFC48A)
private val TrendFallStrong = Color(0xFFFF5252)

/** Near-black contour that keeps the tiny arrow readable on top of a bar. */
private val TrendArrowOutline = Color(0xE6000000)

/** Trend colour for a mean daily change: green = rising, orange/red = falling. */
fun trendColor(rate: Float): Color {
    val t = (abs(rate) / TrendFullScale).coerceIn(0f, 1f)
    return if (rate >= 0f) lerp(TrendRiseWeak, TrendRiseStrong, t)
    else lerp(TrendFallWeak, TrendFallStrong, t)
}

/**
 * Glyph for the trend in places where a drawn vector is not possible (the tile).
 * Same thresholds as [TrendArrow]: hidden below [TrendMinChange], steep changes get the
 * straight arrow.
 */
fun trendGlyph(rate: Float): String = when {
    abs(rate) < TrendMinChange -> ""
    rate >= TrendFullScale * 0.5f -> "\u2191" // ↑
    rate > 0f -> "\u2197" // ↗
    rate <= -TrendFullScale * 0.5f -> "\u2193" // ↓
    else -> "\u2198" // ↘
}

/**
 * Tiny arrow above the top of a bar. Its direction sweeps continuously through 180° in the right
 * half: straight up for the strongest rise, pointing right for a small change and straight down
 * for the strongest fall. The colour encodes the rate as well.
 */
@Composable
private fun TrendArrow(rate: Float, modifier: Modifier = Modifier) {
    if (abs(rate) < TrendMinChange) {
        // No change in the next days: draw nothing.
        Box(modifier)
        return
    }
    val color = trendColor(rate)
    // t = +1 → 0° (up, strongest rise), t = 0 → 90° (right), t = -1 → 180° (down, strongest fall)
    val angle = (90f * (1f - (rate / TrendFullScale).coerceIn(-1f, 1f))) * PI.toFloat() / 180f
    val dx = sin(angle)
    val dy = -cos(angle)
    Canvas(modifier) {
        val side = min(size.width, size.height)
        val len = side * 0.70f
        val headW = side * 0.58f
        val headL = side * 0.40f
        val shaftW = side * 0.20f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val tip = Offset(cx + dx * len / 2f, cy + dy * len / 2f)
        val tail = Offset(cx - dx * len / 2f, cy - dy * len / 2f)
        val neck = Offset(tip.x - dx * headL, tip.y - dy * headL)
        val px = -dy // perpendicular, for the two base corners of the head
        val py = dx
        val head = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(neck.x + px * headW / 2f, neck.y + py * headW / 2f)
            lineTo(neck.x - px * headW / 2f, neck.y - py * headW / 2f)
            close()
        }
        // Dark contour first: a tall bar reaches into the arrow slot and would swallow it.
        drawLine(TrendArrowOutline, tail, neck, shaftW * 1.9f, StrokeCap.Round)
        drawPath(head, TrendArrowOutline, style = Stroke(width = shaftW * 0.9f))
        drawLine(color, tail, neck, shaftW, StrokeCap.Round)
        drawPath(head, color)
    }
}

@Composable
fun HormoneBarChart(
    levels: Map<Hormone, Float>,
    modifier: Modifier = Modifier,
    visible: List<Hormone> = Hormone.defaultVisible.toList(),
    trend: Map<Hormone, Float>? = null, // mean change per day over the next days; null = no arrows
    chartHeight: Dp = 80.dp,
    horizontalPadding: Dp = 12.dp,
    spacing: Dp = 6.dp,
    onHormoneClick: ((Hormone) -> Unit)? = null, // long press
    onChartTap: (() -> Unit)? = null, // short tap
) {
    val shown = visible.ifEmpty { Hormone.defaultVisible.toList() }
    val compact = shown.size > 6
    val gap = if (compact) 2.dp else spacing
    val labelStyle = if (compact) MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp)
    else MaterialTheme.typography.labelSmall
    fun press(h: Hormone): Modifier =
        if (onHormoneClick == null && onChartTap == null) Modifier
        else Modifier.pointerInput(h) {
            detectTapGestures(
                onTap = { onChartTap?.invoke() },
                onLongPress = { onHormoneClick?.invoke(h) },
            )
        }
    Column(modifier.fillMaxWidth().padding(horizontal = horizontalPadding)) {
        Row(
            Modifier.fillMaxWidth().height(chartHeight),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.Bottom,
        ) {
            shown.forEach { h ->
                val v = (levels[h] ?: 0f).coerceIn(0.03f, 1f)
                Box(
                    Modifier.weight(1f).fillMaxHeight().then(press(h)),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        Modifier.fillMaxWidth().fillMaxHeight(v)
                            .background(hormoneColor(h), RoundedCornerShape(4.dp)),
                    )
                    if (trend != null) {
                        // Sits directly on top of this bar, not in a row of its own.
                        TrendArrow(
                            rate = trend[h] ?: 0f,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (chartHeight * (1f - v) - trendArrowSize).coerceAtLeast(0.dp))
                                .size(trendArrowSize),
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
            shown.forEach { h ->
                Text(
                    h.short,
                    modifier = Modifier.weight(1f).then(press(h)),
                    style = labelStyle,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    color = hormoneColor(h),
                )
            }
        }
    }
}
