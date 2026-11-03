package com.watchyourself.watchperiod.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.watchyourself.watchperiod.domain.DotKind

private const val CHART_MIN = 36.0f
private const val CHART_MAX = 37.0f

/**
 * Estimated basal temperature over one cycle as a line with one dot per day, coloured like the
 * ring phase. The y-axis is fixed at 36–37 °C so the small biphasic shift stays visible.
 */
@Composable
fun TemperatureChart(
    values: List<Float>,
    phases: List<DotKind>,
    modifier: Modifier = Modifier,
    markerIndex: Int = -1,
    chartHeight: Dp = 64.dp,
    topLabel: String = "37°",
    bottomLabel: String = "36°",
) {
    if (values.isEmpty()) return
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = Color.White.copy(alpha = 0.14f)

    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier.width(30.dp).height(chartHeight),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    topLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Text(
                    bottomLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Canvas(Modifier.weight(1f).height(chartHeight).padding(start = 4.dp)) {
                val n = values.size
                val dx = if (n > 1) size.width / (n - 1) else 0f
                fun yOf(value: Float): Float {
                    val fraction = (value - CHART_MIN) / (CHART_MAX - CHART_MIN)
                    return size.height * (1f - fraction.coerceIn(0f, 1f))
                }

                listOf(CHART_MIN, 36.5f, CHART_MAX).forEach { level ->
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, yOf(level)),
                        end = Offset(size.width, yOf(level)),
                        strokeWidth = 1f,
                    )
                }

                val path = Path()
                values.forEachIndexed { i, value ->
                    val point = Offset(i * dx, yOf(value))
                    if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                }
                drawPath(path, color = lineColor, style = Stroke(width = 2.dp.toPx()))

                values.forEachIndexed { i, value ->
                    val phase = phases.getOrNull(i) ?: DotKind.NORMAL
                    drawCircle(
                        color = dotColor(phase),
                        radius = 2.6.dp.toPx(),
                        center = Offset(i * dx, yOf(value)),
                    )
                }
                if (markerIndex in 0 until n) {
                    val x = markerIndex * dx
                    drawLine(
                        color = Color.White,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.5f,
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 2.dp, start = 34.dp)) {
            Text(
                "Tag 1",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                "Tag ${values.size}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                maxLines = 1,
            )
        }
    }
}
