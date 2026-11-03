package com.watchyourself.watchperiod.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.watchyourself.watchperiod.domain.CycleEvent
import com.watchyourself.watchperiod.domain.CycleState
import com.watchyourself.watchperiod.domain.EventAnalytics
import com.watchyourself.watchperiod.domain.EventStats
import com.watchyourself.watchperiod.domain.EventType
import com.watchyourself.watchperiod.domain.Hormone
import com.watchyourself.watchperiod.domain.HormoneCorrelation
import com.watchyourself.watchperiod.domain.HormoneEstimator
import com.watchyourself.watchperiod.domain.PartnerInfo
import com.watchyourself.watchperiod.domain.Period
import com.watchyourself.watchperiod.domain.Settings
import com.watchyourself.watchperiod.domain.TemperatureEstimator
import com.watchyourself.watchperiod.domain.formatCelsius
import com.watchyourself.watchperiod.domain.formatShift
import com.watchyourself.watchperiod.ui.InfoPage
import com.watchyourself.watchperiod.ui.components.ChartCaption
import com.watchyourself.watchperiod.ui.components.HormoneBars
import com.watchyourself.watchperiod.ui.components.MetricRow
import com.watchyourself.watchperiod.ui.components.MoodTrend
import com.watchyourself.watchperiod.ui.components.PatternChart
import com.watchyourself.watchperiod.ui.components.PhaseLegend
import com.watchyourself.watchperiod.ui.components.SectionCard
import com.watchyourself.watchperiod.ui.components.StatChip
import com.watchyourself.watchperiod.ui.components.TemperatureChart
import com.watchyourself.watchperiod.ui.theme.eventColor
import com.watchyourself.watchperiod.ui.theme.phaseName
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

@Composable
fun InsightsScreen(
    state: CycleState,
    settings: Settings,
    periods: List<Period>,
    events: List<CycleEvent>,
    onOpenInfo: (InfoPage) -> Unit,
) {
    val today = remember { LocalDate.now() }
    val stats = remember(periods, events, state.cycleLength, today) {
        EventAnalytics.analyse(events, periods, state.cycleLength, today)
    }
    val visible = Hormone.ordered(settings.visibleHormones)
    val hasCycle = state.todayIndex in state.dots.indices
    val phase = state.dots.getOrNull(state.todayIndex)
    val levels = remember(state.todayIndex, state.cycleLength) {
        if (hasCycle) HormoneEstimator.levels(state.todayIndex, state.cycleLength) else emptyMap()
    }
    val trends = remember(state.todayIndex, state.cycleLength) {
        if (hasCycle) HormoneEstimator.trend(state.todayIndex, state.cycleLength) else emptyMap()
    }
    val estimate = remember(state.todayIndex, state.cycleLength, phase) {
        if (hasCycle && phase != null) TemperatureEstimator.estimate(state.todayIndex, state.cycleLength, phase) else null
    }
    val curve = remember(state.cycleLength) { TemperatureEstimator.curve(state.cycleLength) }
    val recentMoods = remember(events) {
        events.filter { it.type == EventType.MOOD }.sortedBy { it.date }.takeLast(14).map { it.value }
    }
    val moodByPhase = remember(stats) { EventAnalytics.moodByPhase(stats) }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Text("Auswertung", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Muster aus deinen eigenen Einträgen – kein Urteil und keine Vorhersage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionCard(title = "Dein Zyklus in Zahlen") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip("${state.cycleLength}", "Tage Ø Zyklus", Modifier.weight(1f))
                    StatChip("${state.periodLength}", "Tage Ø Periode", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip("${stats.cycles}", "erfasste Zyklen", Modifier.weight(1f))
                    StatChip("${events.size}", "Einträge", Modifier.weight(1f))
                }
                if (stats.cycles == 0) {
                    Text(
                        "Trage zuerst eine Periode ein. Danach füllt sich die Auswertung mit deinen " +
                            "Einträgen – ganz ohne Vergleich mit anderen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        "Die Auswertung verbindet deine Einträge mit Zyklusphasen. Je mehr Tage du " +
                            "loggst, desto belastbarer wird sie.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PhaseLegend()
            }
        }

        if (hasCycle && visible.isNotEmpty()) {
            item {
                SectionCard(
                    title = "Hormone heute",
                    subtitle = "Schematischer Verlauf · Tag ${state.todayIndex + 1} von ${state.cycleLength}",
                ) {
                    HormoneBars(levels, trends, visible)
                    Text(
                        "Pfeile zeigen die Richtung der nächsten drei Tage. Die Kurven sind ein " +
                            "Modell aus der Literatur, keine Messung.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (hasCycle && estimate != null && phase != null) {
            item {
                SectionCard(
                    title = "Körpertemperatur",
                    subtitle = "Geschätzt für heute: ${estimate.valueText}",
                ) {
                    MetricRow("Verschiebung zur Follikelphase", estimate.shiftText)
                    MetricRow(
                        "Im Vergleich zum Mann (${formatCelsius(TemperatureEstimator.MALE_REFERENCE)})",
                        formatShift(TemperatureEstimator.differenceToMale(estimate.celsius)),
                    )
                    TemperatureChart(curve, state.dots, state.todayIndex)
                    Text(estimate.note, style = MaterialTheme.typography.bodySmall)
                    Text(
                        TemperatureEstimator.DISCLAIMER,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            SectionCard(title = "Stimmung", subtitle = moodSummaryHeadline(stats, moodByPhase)) {
                if (recentMoods.isNotEmpty()) {
                    MoodTrend(recentMoods)
                    ChartCaption("Deine letzten ${recentMoods.size} Stimmungseinträge (alt → neu).")
                }
                Text(
                    moodSummary(stats, EventType.MOOD),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    correlationSummary(stats, EventType.MOOD),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        EventType.entries.filter { !it.scaled }.forEach { type ->
            item {
                SectionCard(title = "${type.emoji} ${type.label}") {
                    PatternChart(
                        values = EventAnalytics.observed(stats, type),
                        phases = state.dots,
                        accent = eventColor(type),
                        logged = EventAnalytics.logged(stats, type),
                        markerIndex = state.todayIndex,
                    )
                    Text(
                        patternSummary(stats, type),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        correlationSummary(stats, type),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            SectionCard(title = "Für Partner") {
                Text(PartnerInfo.headline, style = MaterialTheme.typography.bodyMedium)
                val range = EventAnalytics.sensitiveDays(stats)
                Text(
                    if (range != null) {
                        "Rücksichtsvolle Tage laut deinen Einträgen: Tag ${range.first}–${range.last}."
                    } else {
                        "Sobald du Angst und Streit loggst, zeigt sich, an welchen Tagen mehr " +
                            "Rücksicht guttut."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Button(onClick = { onOpenInfo(InfoPage.PARTNER) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Hinweise für Partner öffnen")
                }
            }
        }

        item {
            SectionCard(title = "Wissen") {
                OutlinedButton(
                    onClick = { onOpenInfo(InfoPage.PMDD) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("PMDD/PMDS verstehen") }
                OutlinedButton(
                    onClick = { onOpenInfo(InfoPage.SOURCES) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Quellen & Hinweise") }
                Text(
                    "Diese App ist kein Medizinprodukt. Bei starker Beeinträchtigung oder Gedanken " +
                        "an Selbstverletzung suche bitte professionelle Hilfe. In akuten Krisen: " +
                        "Notruf 112 oder Telefonseelsorge 0800 111 0 111.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Text(
                "r = Pearson-Korrelation (−1 … +1), r² = erklärter Anteil, n = Tage mit Daten. " +
                    "Ein Zusammenhang ist keine Ursache; die Hormonwerte sind ein Schema, keine Messung.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun moodSummaryHeadline(
    stats: EventStats,
    phases: List<com.watchyourself.watchperiod.domain.PhaseMood>,
): String {
    val average = stats.moodAverage ?: return "Noch keine Einträge"
    val worst = phases.firstOrNull()
    val best = phases.lastOrNull()
    return if (worst != null && best != null && worst.phase != best.phase) {
        "Ø ${signedDecimal(average)} · am besten in der ${phaseName(best.phase)}, am niedrigsten in der ${phaseName(worst.phase)}"
    } else {
        "Ø ${signedDecimal(average)}"
    }
}

/** Phases where this event type is more frequent than average, plus the literature note. */
private fun patternSummary(stats: EventStats, type: EventType): String {
    if (stats.cycles == 0) return "Noch keine Periode eingetragen – keine Auswertung möglich."
    if (type.scaled) return moodSummary(stats, type)

    val values = EventAnalytics.observed(stats, type)
    val peaks = EventAnalytics.phaseIntensities(values, stats.cycleLength)
        .filter { it.relative >= 1.15f }
        .take(2)
    val head = if (peaks.isEmpty()) {
        "Tendenz: über den Zyklus gleichmäßig – keine Phase ≥ 1,2×."
    } else {
        "Tendenz: gehäuft in " + peaks.joinToString(", ") {
            "${phaseName(it.phase)} ${multiplierText(it.relative)}"
        } + " (im Vergleich zum Zyklusmittel)."
    }
    val total = stats.total(type)
    val base = when {
        total == 0 -> "Noch keine ${type.label}-Einträge (${stats.cycles} Zyklen)."
        total < 5 -> "Datenbasis: $total Einträge in ${stats.cycles} Zyklen – die Tendenz ist noch unsicher."
        else -> "Datenbasis: $total Einträge in ${stats.cycles} Zyklen."
    }
    return "$head\n$base\nLiteratur: ${type.literature}"
}

private fun moodSummary(stats: EventStats, type: EventType): String {
    val average = stats.moodAverage
    if (average == null) {
        return "Noch keine Stimmungs-Einträge (${stats.cycles} Zyklen).\n" +
            "Trage täglich eine Stimmung ein, dann zeigt sich die Tendenz pro Phase."
    }
    val phases = EventAnalytics.moodByPhase(stats)
    val lowest = phases.firstOrNull()
    val highest = phases.lastOrNull()
    val phaseText = if (lowest != null && highest != null && lowest.phase != highest.phase) {
        "Am niedrigsten: ${phaseName(lowest.phase)} (Ø ${signedDecimal(lowest.mean)}); " +
            "am höchsten: ${phaseName(highest.phase)} (Ø ${signedDecimal(highest.mean)})."
    } else {
        "Noch zu wenige Phasen für einen Vergleich."
    }
    val days = stats.moodDays()
    val base = if (days < 5) "Datenbasis: $days Tage – die Tendenz ist noch unsicher."
    else "Datenbasis: $days Tage in ${stats.cycles} Zyklen."
    return "Tendenz: Stimmung im Mittel ${EventAnalytics.moodLabelFor(average)} " +
        "(Ø ${signedDecimal(average)}).\n$phaseText\n$base\nLiteratur: ${type.literature}"
}

private fun correlationSummary(stats: EventStats, type: EventType): String {
    val list: List<HormoneCorrelation> = EventAnalytics.correlations(stats, type)
    if (list.isEmpty()) {
        return "Zusammenhang mit Hormonen: noch zu wenige Daten (mind. 4 Einträge bzw. Tage)."
    }
    val lines = list.joinToString("\n") { c ->
        val sign = if (c.r >= 0f) "+" else "−"
        "${c.hormone.label}: r = $sign${decimalText(abs(c.r))} " +
            "(${EventAnalytics.correlationStrength(c.r)}, r² = ${decimalText(c.r2)}, n = ${c.n})"
    }
    val top = list.first()
    val tendency = when {
        type.scaled && top.r >= 0f -> "Je höher ${top.hormone.label}, desto besser im Mittel die Stimmung."
        type.scaled -> "Je höher ${top.hormone.label}, desto schlechter im Mittel die Stimmung."
        top.r >= 0f -> "Je höher ${top.hormone.label}, desto häufiger ${type.label}."
        else -> "Je höher ${top.hormone.label}, desto seltener ${type.label}."
    }
    return "Zusammenhang mit angenommenen Hormonen (stärkste 3 von ${Hormone.entries.size}):\n" +
        "$lines\nTendenz: $tendency"
}

private fun signedDecimal(value: Float): String {
    val text = decimalText(value)
    return if (value > 0f) "+$text" else text
}

private fun decimalText(value: Float): String = String.format(Locale.GERMANY, "%.1f", value)

private fun multiplierText(relative: Float): String {
    val rounded = round(relative * 10) / 10.0
    val text = if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString().replace('.', ',')
    return "$text×"
}
