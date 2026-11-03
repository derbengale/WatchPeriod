package com.watchyourself.watchperiod.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.watchyourself.watchperiod.domain.DotKind
import com.watchyourself.watchperiod.domain.Hormone
import com.watchyourself.watchperiod.ui.theme.OvulationBlue
import com.watchyourself.watchperiod.ui.theme.PeriodRed
import com.watchyourself.watchperiod.ui.theme.PredictedPink
import com.watchyourself.watchperiod.ui.theme.dotColor
import kotlin.math.max
import kotlin.math.min

@Composable
fun PhaseLegend(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendItem(DotKind.PERIOD to "Menstruation")
            LegendItem(DotKind.FOLLICULAR to "Follikel")
            LegendItem(DotKind.OVULATION to "Eisprung")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendItem(DotKind.LUTEAL to "Luteal")
            LegendItem(DotKind.PREMENSTRUAL to "Spätluteal")
            LegendItem(DotKind.PREDICTED to "Errechnet")
        }
    }
}

@Composable
private fun LegendItem(pair: Pair<DotKind, String>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(dotColor(pair.first)),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            pair.second,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Horizontal bars for the schematic hormone levels with a three-day trend arrow. */
@Composable
fun HormoneBars(
    levels: Map<Hormone, Float>,
    trends: Map<Hormone, Float>,
    visible: List<Hormone>,
    modifier: Modifier = Modifier,
    showArrows: Boolean = true,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        visible.forEach { hormone ->
            val level = (levels[hormone] ?: 0f).coerceIn(0f, 1f)
            val trend = trends[hormone] ?: 0f
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (hormone.moodRelevant) "★ ${hormone.label}" else hormone.label,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(118.dp),
                    maxLines = 1,
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(9.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(level)
                            .clip(RoundedCornerShape(99.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.secondary,
                                        MaterialTheme.colorScheme.primary,
                                    ),
                                ),
                            ),
                    )
                }
                if (showArrows) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when {
                            trend > 0.004f -> "↑"
                            trend < -0.004f -> "↓"
                            else -> "→"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = when {
                            trend > 0.004f -> MaterialTheme.colorScheme.tertiary
                            trend < -0.004f -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(18.dp),
                    )
                }
            }
        }
    }
}

/**
 * One bar per cycle day, coloured by phase. Faded bars for scaled types mean "no entry";
 * for counted types a zero simply means nothing was logged.
 */
@Composable
fun PatternChart(
    values: FloatArray,
    phases: List<DotKind>,
    accent: Color,
    modifier: Modifier = Modifier,
    logged: BooleanArray? = null,
    markerIndex: Int = -1,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val markerColor = MaterialTheme.colorScheme.primary
    Canvas(modifier.fillMaxWidth().height(132.dp)) {
        val n = values.size.coerceAtLeast(1)
        val width = size.width
        val height = size.height - 14.dp.toPx()
        val gap = if (n > 32) 1.2.dp.toPx() else 2.4.dp.toPx()
        val barWidth = max(1f, (width - gap * (n - 1)) / n)
        for (line in 1..3) {
            val y = height * (1f - line / 4f)
            drawLine(gridColor, Offset(0f, y), Offset(width, y), strokeWidth = 1f)
        }
        drawLine(gridColor, Offset(0f, height), Offset(width, height), strokeWidth = 1f)
        values.forEachIndexed { index, raw ->
            val value = raw.coerceIn(0f, 1f)
            val visibleBar = logged?.getOrNull(index) ?: true
            val barHeight = max(2.dp.toPx(), height * value)
            val x = index * (barWidth + gap)
            val phase = phases.getOrElse(index) { DotKind.NORMAL }
            val color = if (phase == DotKind.NORMAL) accent else dotColor(phase)
            drawRoundRect(
                color = color.copy(alpha = if (visibleBar) 0.9f else 0.22f),
                topLeft = Offset(x, height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
        if (markerIndex in 0 until n) {
            val x = markerIndex * (barWidth + gap) + barWidth / 2f
            drawLine(
                markerColor.copy(alpha = 0.55f),
                Offset(x, 0f),
                Offset(x, height),
                strokeWidth = 1.5.dp.toPx(),
            )
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Tag 1", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "Tag ${values.size}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Schematic basal-temperature curve with phase-coloured dots and today's marker. */
@Composable
fun TemperatureChart(
    values: List<Float>,
    phases: List<DotKind>,
    markerIndex: Int,
    modifier: Modifier = Modifier,
) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(modifier.fillMaxWidth().height(150.dp)) {
        if (values.size < 2) return@Canvas
        val pad = 10.dp.toPx()
        val minValue = (values.minOrNull() ?: 36f) - 0.08f
        val maxValue = (values.maxOrNull() ?: 37f) + 0.08f
        val span = max(0.1f, maxValue - minValue)
        val stepX = (size.width - pad * 2) / (values.size - 1)
        fun point(index: Int): Offset {
            val x = pad + index * stepX
            val y = pad + (1f - (values[index] - minValue) / span) * (size.height - pad * 2)
            return Offset(x, y)
        }
        val line = Path()
        values.indices.forEach { index ->
            val p = point(index)
            if (index == 0) line.moveTo(p.x, p.y) else line.lineTo(p.x, p.y)
        }
        val fill = Path().apply {
            addPath(line)
            val last = point(values.lastIndex)
            lineTo(last.x, size.height)
            lineTo(point(0).x, size.height)
            close()
        }
        drawPath(
            fill,
            brush = Brush.verticalGradient(
                listOf(primaryColor.copy(alpha = 0.20f), Color.Transparent),
            ),
        )
        drawPath(line, color = primaryColor, style = Stroke(width = 2.5.dp.toPx()))
        values.indices.forEach { index ->
            val phase = phases.getOrElse(index) { DotKind.NORMAL }
            drawCircle(dotColor(phase), radius = 2.8.dp.toPx(), center = point(index))
        }
        if (markerIndex in values.indices) {
            val p = point(markerIndex)
            drawLine(
                outline,
                Offset(p.x, 0f),
                Offset(p.x, size.height),
                strokeWidth = 1.5.dp.toPx(),
            )
            drawCircle(primaryColor, radius = 4.5.dp.toPx(), center = p)
            drawCircle(Color.White, radius = 2.dp.toPx(), center = p)
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Tag 1", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "Tag ${values.size}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A soft, compact preview of the mood values of the last days. */
@Composable
fun MoodTrend(values: List<Int>, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
        values.forEach { value ->
            val fraction = (value - (-2)) / 4f
            val color = when {
                value >= 1 -> MaterialTheme.colorScheme.tertiary
                value <= -1 -> PeriodRed
                else -> OvulationBlue
            }
            Box(
                Modifier
                    .width(9.dp)
                    .height((14 + fraction * 34).dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(color.copy(alpha = 0.85f)),
            )
        }
    }
}

@Composable
fun MoodScaleDots(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        listOf(-2, -1, 0, 1, 2).forEach { value ->
            val color = when {
                value >= 1 -> MaterialTheme.colorScheme.tertiary
                value <= -1 -> PeriodRed
                else -> OvulationBlue
            }
            Box(
                Modifier
                    .size(if (value == 0) 5.dp else 7.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.75f)),
            )
        }
    }
}

/** Faint background used behind the phase legend and cards. */
@Composable
fun SoftCardBackground(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        PredictedPink.copy(alpha = 0.16f),
                        Color.Transparent,
                    ),
                ),
            ),
    )
}

@Composable
fun ChartCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 4.dp),
    )
}

internal fun FloatArray.normalisedOrZero(): FloatArray {
    val maximum = maxOrNull() ?: 0f
    return if (maximum <= 0f) FloatArray(size) { 0f } else FloatArray(size) { min(1f, this[it] / maximum) }
}
