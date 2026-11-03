package com.watchyourself.watchperiod.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.watchyourself.watchperiod.domain.DotKind
import com.watchyourself.watchperiod.domain.EventAnalytics
import com.watchyourself.watchperiod.domain.EventType
import java.util.Locale

fun eventColor(type: EventType): Color = when (type) {
    EventType.ANXIETY -> Color(0xFF4DB8FF)
    EventType.CONFLICT -> Color(0xFFFFA040)
    EventType.SEX -> Color(0xFFFF5470)
    EventType.MOOD -> Color(0xFFFFD34D)
}

/**
 * Relative intensity per cycle day (0..1) as thin bars. Every day gets a visible base bar in
 * its cycle-phase colour, so the chart always spans the whole calculated cycle; the height and
 * the small cap show the logged intensity. [logged] marks days with a real entry (needed for
 * mood, where a low value looks like "nothing logged").
 */
@Composable
fun EventPatternChart(
    values: FloatArray,
    eventColor: Color,
    modifier: Modifier = Modifier,
    phases: List<DotKind> = emptyList(),
    logged: BooleanArray? = null,
    chartHeight: Dp = 44.dp,
    markerIndex: Int = -1,
    startLabel: String = "Tag 1",
    endLabel: String = "Tag ${values.size}",
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Canvas(Modifier.fillMaxWidth().height(chartHeight)) {
            val n = values.size
            if (n == 0) return@Canvas
            val gap = 1.5f
            val barWidth = (size.width - gap * (n - 1)) / n
            val capHeight = 2.5.dp.toPx()
            values.forEachIndexed { i, raw ->
                val x = i * (barWidth + gap)
                val intensity = raw.coerceIn(0f, 1f)
                val hasEntry = logged?.getOrNull(i) ?: (raw > 0.0001f)
                val phase = phases.getOrNull(i) ?: EventAnalytics.phaseOfDay(i, n)
                val base = 0.10f
                val h = size.height * (base + (1f - base) * intensity)
                val top = size.height - h
                val alpha = when {
                    phase == DotKind.PREDICTED -> if (hasEntry) 0.55f else 0.30f
                    hasEntry -> 0.95f
                    else -> 0.45f
                }
                drawRect(
                    color = dotColor(phase).copy(alpha = alpha),
                    topLeft = Offset(x, top),
                    size = Size(barWidth, h),
                )
                if (hasEntry) {
                    // The event colour only marks the top so the phase colour stays readable.
                    drawRect(eventColor, topLeft = Offset(x, top), size = Size(barWidth, capHeight))
                }
            }
            if (markerIndex in 0 until n) {
                val x = markerIndex * (barWidth + gap) + barWidth / 2f
                drawLine(
                    color = Color.White,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 2f,
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
            Text(
                startLabel,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                endLabel,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                maxLines = 1,
            )
        }
    }
}

/** Colour key for the bars: the same phase colours as the ring and the tile. */
@Composable
fun PhaseLegend(modifier: Modifier = Modifier) {
    val phases = listOf(
        DotKind.PERIOD to "Periode",
        DotKind.FOLLICULAR to "Follikel",
        DotKind.OVULATION to "Eisprung",
        DotKind.LUTEAL to "Luteal",
        DotKind.PREMENSTRUAL to "Spätluteal",
    )
    val text = buildAnnotatedString {
        phases.forEachIndexed { i, (kind, label) ->
            if (i == 3) append("\n") else if (i > 0) append("  ")
            withStyle(SpanStyle(color = dotColor(kind))) { append("\u25CF") }
            append(" $label")
        }
        append("\n")
        withStyle(SpanStyle(color = PredictedPink)) { append("\u25CB") }
        append(" errechnet · Höhe = Häufigkeit/Stimmung")
    }
    Text(
        text,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** "1,8" with a German decimal comma. */
fun decimalText(value: Float): String = String.format(Locale.GERMANY, "%.1f", value)

/** Compact "1,8×" style multiplier for a phase. */
fun multiplierText(relative: Float): String {
    val rounded = kotlin.math.round(relative * 10) / 10.0
    val text = if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString().replace('.', ',')
    return "$text×"
}
