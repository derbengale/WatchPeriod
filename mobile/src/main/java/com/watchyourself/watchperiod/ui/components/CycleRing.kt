package com.watchyourself.watchperiod.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.watchyourself.watchperiod.domain.CycleState
import com.watchyourself.watchperiod.domain.DotKind
import com.watchyourself.watchperiod.ui.theme.dotColor
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The cycle as a ring of dots – the same visual language as the Wear app, but with more
 * room for the centre summary. Tapping a dot opens that day.
 */
@Composable
fun CycleRing(
    state: CycleState,
    modifier: Modifier = Modifier,
    onDayClick: (Int) -> Unit = {},
    center: @Composable () -> Unit = {},
) {
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val todayColor = MaterialTheme.colorScheme.primary
    val n = state.dots.size.coerceAtLeast(1)
    Box(
        modifier
            .aspectRatio(1f)
            .pointerInput(n, state.todayIndex) {
                detectTapGestures { tap ->
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val dx = tap.x - cx
                    val dy = tap.y - cy
                    val distance = sqrt(dx * dx + dy * dy)
                    val outer = min(size.width, size.height) / 2f
                    if (distance in outer * 0.42f..outer * 0.98f) {
                        var angle = atan2(dy, dx) + (PI / 2).toFloat()
                        if (angle < 0f) angle += (2 * PI).toFloat()
                        val index = ((angle / (2 * PI).toFloat() * n).roundToInt() % n + n) % n
                        onDayClick(index)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val side = size.minDimension
            val radius = side / 2f * 0.80f
            val dotRadius = side / (if (n > 35) 30f else 24f)
            drawCircle(
                color = trackColor.copy(alpha = 0.55f),
                radius = radius,
                style = Stroke(width = 1.dp.toPx()),
            )
            for (i in 0 until n) {
                val angle = -PI / 2 + 2 * PI * i / n
                val centre = Offset(
                    x = size.width / 2f + cos(angle).toFloat() * radius,
                    y = size.height / 2f + sin(angle).toFloat() * radius,
                )
                val kind = state.dots.getOrElse(i) { DotKind.NORMAL }
                val isToday = i == state.todayIndex
                val base = dotColor(kind)
                if (isToday) {
                    drawCircle(base.copy(alpha = 0.22f), dotRadius * 2.35f, centre)
                    drawCircle(base, dotRadius * 1.25f, centre)
                    drawCircle(Color.White, dotRadius * 0.55f, centre)
                    drawCircle(todayColor.copy(alpha = 0.55f), dotRadius * 1.9f, centre, style = Stroke(1.5.dp.toPx()))
                } else {
                    drawCircle(base, if (kind == DotKind.PERIOD) dotRadius * 1.1f else dotRadius, centre)
                }
            }
        }
        center()
    }
}
