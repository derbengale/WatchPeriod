package com.watchyourself.watchperiod.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import com.watchyourself.watchperiod.MainActivity
import com.watchyourself.watchperiod.R
import com.watchyourself.watchperiod.data.PeriodRepository
import com.watchyourself.watchperiod.domain.CycleState
import com.watchyourself.watchperiod.domain.DotKind
import com.watchyourself.watchperiod.ui.components.nextPeriodLabel
import com.watchyourself.watchperiod.ui.theme.dotColor
import com.watchyourself.watchperiod.ui.theme.phaseName
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Home-screen widget: the same dot ring as the "Heute" screen, with the active cycle
 * zone (day and phase) in the centre. Tapping the widget opens the app.
 */
class CycleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { id -> update(context, appWidgetManager, id) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        update(context, appWidgetManager, appWidgetId)
    }

    companion object {
        /** Re-renders every placed widget after the cycle data changed. */
        fun refresh(context: Context) {
            val app = context.applicationContext
            val manager = AppWidgetManager.getInstance(app)
            val ids = manager.getAppWidgetIds(ComponentName(app, CycleWidgetProvider::class.java))
            ids.forEach { id -> update(app, manager, id) }
        }

        private fun update(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val state = PeriodRepository(context).state()
            val sizeDp = widgetSizeDp(manager.getAppWidgetOptions(widgetId))
            val density = context.resources.displayMetrics.density
            // RemoteViews are passed through a binder: keep the bitmap well below 1 MB.
            val ringPx = (sizeDp * density).toInt().coerceIn(96, 384)

            val kind = state.dots.getOrNull(state.todayIndex)
            val views = RemoteViews(context.packageName, R.layout.widget_cycle)
            views.setImageViewBitmap(R.id.widget_ring, renderRing(state, ringPx))
            views.setTextViewText(R.id.widget_day, dayTitle(state))
            views.setTextViewText(R.id.widget_phase, phaseTitle(kind))
            views.setTextViewText(R.id.widget_next, nextTitle(state))
            if (kind != null) {
                views.setTextColor(R.id.widget_phase, dotColor(kind).toArgb())
            }
            val launch = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, launch)
            manager.updateAppWidget(widgetId, views)
        }

        private fun widgetSizeDp(options: Bundle): Int {
            val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 150)
            val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 150)
            return min(width, height).coerceIn(96, 320)
        }

        private fun dayTitle(state: CycleState): String = when {
            state.todayIndex < 0 -> "Keine Daten"
            state.periodOngoing && state.periodDay != null -> "TAG ${state.periodDay}"
            else -> "TAG ${state.todayIndex + 1}"
        }

        private fun phaseTitle(kind: DotKind?): String =
            if (kind == null) "Zyklus eintragen" else phaseName(kind)

        private fun nextTitle(state: CycleState): String =
            if (state.todayIndex < 0) "Tippen zum Starten" else nextPeriodLabel(state)

        /**
         * Dot geometry and colours mirror [com.watchyourself.watchperiod.ui.components.CycleRing]:
         * one dot per cycle day on a circle, today highlighted with a halo and a white core.
         */
        internal fun renderRing(state: CycleState, size: Int): Bitmap {
            val bitmap = createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val n = state.dots.size.coerceAtLeast(1)
            val side = size.toFloat()
            val centreX = side / 2f
            val centreY = side / 2f
            val radius = side / 2f * 0.80f
            val dotRadius = side / (if (n > 35) 30f else 24f)

            val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = side / 240f
                color = 0x59FFFFFF
            }
            canvas.drawCircle(centreX, centreY, radius, track)

            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
            val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
            for (i in 0 until n) {
                val angle = -PI / 2 + 2 * PI * i / n
                val x = centreX + cos(angle).toFloat() * radius
                val y = centreY + sin(angle).toFloat() * radius
                val kind = state.dots.getOrElse(i) { DotKind.NORMAL }
                val base = dotColor(kind).toArgb()
                if (i == state.todayIndex) {
                    fill.color = base
                    fill.alpha = 56
                    canvas.drawCircle(x, y, dotRadius * 2.35f, fill)
                    fill.alpha = 255
                    canvas.drawCircle(x, y, dotRadius * 1.25f, fill)
                    fill.color = 0xFFFFFFFF.toInt()
                    canvas.drawCircle(x, y, dotRadius * 0.55f, fill)
                    outline.color = 0x8CFFB1C3.toInt()
                    outline.strokeWidth = side / 220f
                    canvas.drawCircle(x, y, dotRadius * 1.9f, outline)
                } else {
                    fill.alpha = 255
                    fill.color = base
                    canvas.drawCircle(
                        x,
                        y,
                        if (kind == DotKind.PERIOD) dotRadius * 1.1f else dotRadius,
                        fill,
                    )
                }
            }
            return bitmap
        }
    }
}
