package com.watchyourself.watchperiod.ui.components

import com.watchyourself.watchperiod.domain.CycleState

/** Countdown line under the ring – shown on the Heute screen and in the home-screen widget. */
fun nextPeriodLabel(state: CycleState): String {
    if (state.periodPredicted) return "Start nur errechnet – bitte bestätigen"
    if (state.periodOngoing) {
        return "Tag ${state.periodDay ?: (state.todayIndex + 1)} von ${state.periodLength}"
    }
    val days = state.daysUntilNext ?: return ""
    return when {
        days > 0 -> "Nächste Periode in $days Tagen"
        days == 0 -> "Periode heute erwartet"
        else -> "Periode seit ${-days} Tagen überfällig"
    }
}
