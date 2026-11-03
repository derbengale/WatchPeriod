package com.watchyourself.watchperiod.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watchyourself.watchperiod.data.PeriodRepository
import com.watchyourself.watchperiod.domain.CycleState
import com.watchyourself.watchperiod.domain.EventType
import com.watchyourself.watchperiod.domain.Hormone
import com.watchyourself.watchperiod.domain.HormoneEstimator
import com.watchyourself.watchperiod.domain.MoodEstimator
import com.watchyourself.watchperiod.domain.Settings
import com.watchyourself.watchperiod.domain.TemperatureEstimator
import com.watchyourself.watchperiod.ui.components.CycleRing
import com.watchyourself.watchperiod.ui.components.EventCounterRow
import com.watchyourself.watchperiod.ui.components.HormoneBars
import com.watchyourself.watchperiod.ui.components.MetricRow
import com.watchyourself.watchperiod.ui.components.nextPeriodLabel
import com.watchyourself.watchperiod.ui.components.SectionCard
import com.watchyourself.watchperiod.ui.components.SyncBadge
import com.watchyourself.watchperiod.ui.components.SyncLine
import com.watchyourself.watchperiod.ui.components.formatDay
import com.watchyourself.watchperiod.ui.components.rememberWatchSyncInfo
import com.watchyourself.watchperiod.ui.theme.phaseName
import java.time.LocalDate

@Composable
fun HomeScreen(
    repo: PeriodRepository,
    state: CycleState,
    settings: Settings,
    version: Int,
    onOpenDay: (LocalDate) -> Unit,
    onAddPeriod: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    val counts = remember(version, today) { repo.eventCounts(today) }
    val mood = remember(version, today) { repo.moodFor(today) }
    val syncInfo = rememberWatchSyncInfo(version)
    val todayKind = state.dots.getOrNull(state.todayIndex)
    val hasCycle = state.todayIndex >= 0 && todayKind != null
    val visible = Hormone.ordered(settings.visibleHormones)
    val levels = remember(state.todayIndex, state.cycleLength) {
        if (state.todayIndex >= 0) HormoneEstimator.levels(state.todayIndex, state.cycleLength) else emptyMap()
    }
    val trend = remember(state.todayIndex, state.cycleLength) {
        if (state.todayIndex >= 0) HormoneEstimator.trend(state.todayIndex, state.cycleLength) else emptyMap()
    }
    val estimate = remember(state.todayIndex, state.cycleLength, todayKind) {
        val kind = todayKind
        if (state.todayIndex >= 0 && kind != null) {
            TemperatureEstimator.estimate(state.todayIndex, state.cycleLength, kind)
        } else {
            null
        }
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("WatchPeriod", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Dein Zyklus, nah bei dir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SyncBadge(syncInfo)
            }
        }

        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer,
                            ),
                            start = Offset.Zero,
                            end = Offset(1100f, 1400f),
                        ),
                    )
                    .padding(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CycleRing(
                        state = state,
                        modifier = Modifier.fillMaxWidth(),
                        onDayClick = { index -> state.dateForOffset(index)?.let(onOpenDay) },
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (hasCycle) {
                                Text(
                                    if (state.periodOngoing) "TAG ${state.periodDay}" else "TAG ${state.todayIndex + 1}",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    phaseName(todayKind!!),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    nextPeriodLabel(state),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center,
                                )
                            } else {
                                Text(
                                    "Willkommen",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Trage deine letzte Periode ein,\num den Ring zu starten.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        com.watchyourself.watchperiod.ui.components.StatChip(
                            value = "${state.cycleLength}",
                            label = "Tage Zyklus",
                            modifier = Modifier.weight(1f),
                        )
                        com.watchyourself.watchperiod.ui.components.StatChip(
                            value = "${state.periodLength}",
                            label = "Tage Periode",
                            modifier = Modifier.weight(1f),
                        )
                        com.watchyourself.watchperiod.ui.components.StatChip(
                            value = "${state.ovulationDay}.",
                            label = "Eisprung",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        item {
            SectionCard(title = "Schnell erfassen") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            if (state.periodOngoing) repo.endToday() else repo.startToday()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            if (state.periodOngoing) "Periode endete" else "Periode begann",
                            textAlign = TextAlign.Center,
                        )
                    }
                    OutlinedButton(
                        onClick = onAddPeriod,
                        modifier = Modifier.weight(1f),
                    ) { Text("Nachtragen") }
                }
                Text(
                    if (state.periodPredicted) {
                        "Der Start ist gerade nur errechnet. Mit „Periode begann“ bestätigst du ihn für heute."
                    } else {
                        "Ein Tipp auf den Ring öffnet den Tag – dort lassen sich auch Stimmung und Ereignisse nachtragen."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionCard(
                title = "Stimmung heute",
                subtitle = mood?.let { EventType.moodLabel(it) } ?: "Noch kein Eintrag – ein Tipp genügt.",
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    (-2..2).forEach { value ->
                        val selected = mood == value
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                )
                                .clickable {
                                    if (selected) repo.deleteMood(today) else repo.saveMood(today, value)
                                }
                                .padding(vertical = 8.dp, horizontal = 3.dp),
                        ) {
                            Text(EventType.moodEmoji(value), fontSize = 24.sp)
                            Text(
                                EventType.moodLabel(value).lowercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionCard(
                title = "Heute erfassen",
                subtitle = formatDay(today),
            ) {
                EventType.entries.filter { !it.scaled }.forEach { type ->
                    EventCounterRow(
                        type = type,
                        count = counts[type] ?: 0,
                        onPlus = { repo.addEvent(today, type) },
                        onMinus = { repo.removeEvent(today, type) },
                    )
                }
                TextButton(onClick = { onOpenDay(today) }) {
                    Text("Tagesdetails und Nachtragen")
                }
            }
        }

        if (hasCycle && estimate != null) {
            val forecast = MoodEstimator.forPhase(todayKind!!)
            item {
                SectionCard(title = "Ausblick für heute") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(forecast.emoji, fontSize = 32.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Stimmung: ${forecast.mood}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "Reizbarkeit: ${forecast.irritability}" +
                                    (forecast.libido?.let { " · Lust: $it" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        forecast.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    MetricRow(
                        "Körpertemperatur (geschätzt)",
                        "${estimate.valueText} · ${estimate.shiftText}",
                    )
                    Text(
                        estimate.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (visible.isNotEmpty()) {
                item {
                    SectionCard(
                        title = "Hormone",
                        subtitle = "Schematische Schätzung für heute – keine Messung.",
                    ) {
                        HormoneBars(levels, trend, visible, showArrows = false)
                        Text(
                            "★ markiert Hormone mit möglichem Einfluss auf das Wohlbefinden. " +
                                "Individuelle Werte können stark abweichen.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            Column {
                SyncLine(syncInfo)
                Text(
                    "Änderungen werden automatisch mit deiner Uhr abgeglichen. Beide Geräte " +
                        "behalten alle Daten lokal – es gibt kein Konto und keine Cloud.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}


