package com.watchyourself.watchperiod.complication

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.watchyourself.watchperiod.data.PeriodRepository

/** Short text: days until the next period (or "Day N" while it is ongoing). */
class MainComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        return create("12 T", "Nächste Periode in 12 Tagen")
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val state = PeriodRepository(this).state()
        val days = state.daysUntilNext
        return when {
            state.periodOngoing -> create("Tag ${state.periodDay}", "Periodentag ${state.periodDay}")
            days == null -> create("--", "Keine Daten")
            days < 0 -> create("+${-days} T", "Periode ${-days} Tage überfällig")
            else -> create("$days T", "Nächste Periode in $days Tagen")
        }
    }

    private fun create(text: String, contentDescription: String) =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder(contentDescription).build()
        ).build()
}
