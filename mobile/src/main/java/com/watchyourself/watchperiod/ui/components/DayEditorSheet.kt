package com.watchyourself.watchperiod.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watchyourself.watchperiod.data.PeriodRepository
import com.watchyourself.watchperiod.domain.CycleState
import com.watchyourself.watchperiod.domain.DotKind
import com.watchyourself.watchperiod.domain.EventType
import com.watchyourself.watchperiod.domain.HormoneEstimator
import com.watchyourself.watchperiod.domain.Period
import com.watchyourself.watchperiod.ui.theme.phaseName
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Bottom sheet for one day. Each tap writes immediately, so the watch always has the
 * same state without a separate "save" step.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayEditorSheet(
    day: LocalDate,
    state: CycleState,
    periods: List<Period>,
    repo: PeriodRepository,
    version: Int,
    onDismiss: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    val shownOffset = state.offsetFor(day)
    val historicalOffset = if (shownOffset == null) cycleIndexFor(day, periods, state.cycleLength) else null
    val offset = shownOffset ?: historicalOffset
    val phase = when {
        shownOffset != null -> state.dots.getOrNull(shownOffset)
        historicalOffset != null -> historicalPhase(day, historicalOffset, periods, state)
        else -> null
    }
    val counts = remember(version, day) { repo.eventCounts(day) }
    val mood = remember(version, day) { repo.moodFor(day) }
    val future = day.isAfter(today)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                formatLongDay(day),
                style = MaterialTheme.typography.headlineSmall,
            )
            if (offset == null || phase == null) {
                Text(
                    "Für diesen Tag gibt es noch keine Zyklusdaten. Trage zuerst eine Periode ein.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .width(10.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(com.watchyourself.watchperiod.ui.theme.dotColor(phase)),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Tag ${offset + 1} · ${phaseName(phase)}" +
                            when (phase) {
                                DotKind.PERIOD -> " · geloggt"
                                DotKind.PREDICTED -> " · errechnet"
                                else -> ""
                            },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (future) {
                Text(
                    "Dieser Tag liegt in der Zukunft. Ereignisse lassen sich erst am Tag selbst " +
                        "eintragen – so bleibt der Verlauf ehrlich.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (offset != null && phase != null) {
                Text("Stimmung", style = MaterialTheme.typography.titleMedium)
                MoodPicker(mood) { value ->
                    if (value == null) repo.deleteMood(day) else repo.saveMood(day, value)
                }
                Text(
                    "Ereignisse",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
                EventType.entries.filter { !it.scaled }.forEach { type ->
                    EventCounterRow(
                        type = type,
                        count = counts[type] ?: 0,
                        onPlus = { repo.addEvent(day, type) },
                        onMinus = { repo.removeEvent(day, type) },
                    )
                }
                if (phase == DotKind.PREDICTED) {
                    OutlinedButton(
                        onClick = {
                            repo.startAt(day)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Periode begann an diesem Tag") }
                }
                if (state.periodPredicted && offset in 0 until state.predictedPeriodDays) {
                    Text(
                        "Der Periodenstart ist gerade nur errechnet. Bestätige ihn hier, damit " +
                            "die Uhr ihn als geloggt anzeigt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "Alles wird sofort gespeichert und mit deiner Uhr abgeglichen.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Fertig", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun MoodPicker(mood: Int?, onMood: (Int?) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        (-2..2).forEach { value ->
            val selected = mood == value
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    )
                    .clickable { onMood(if (selected) null else value) }
                    .padding(horizontal = 5.dp, vertical = 8.dp)
                    .weight(1f),
            ) {
                Text(EventType.moodEmoji(value), fontSize = 25.sp)
                Text(
                    EventType.moodLabel(value),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (value < 2) Spacer(Modifier.width(5.dp))
        }
    }
}

@Composable
fun EventCounterRow(
    type: EventType,
    count: Int,
    onPlus: () -> Unit,
    onMinus: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(type.emoji, fontSize = 21.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(type.label, style = MaterialTheme.typography.titleMedium)
            Text(
                type.hint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        IconButton(onClick = onMinus, enabled = count > 0) { Text("−", fontSize = 21.sp) }
        Text(
            "$count",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(22.dp),
        )
        IconButton(onClick = onPlus) { Text("+", fontSize = 21.sp) }
    }
}

/** 0-based cycle day of [day] relative to the latest period that started on or before it. */
private fun cycleIndexFor(day: LocalDate, periods: List<Period>, cycleLength: Int): Int? {
    val start = periods.map { it.start }.filter { !it.isAfter(day) }.maxOrNull() ?: return null
    val diff = ChronoUnit.DAYS.between(start, day).toInt()
    return diff.mod(cycleLength.coerceAtLeast(1))
}

/** Phase marker for a day outside the currently shown cycle. */
private fun historicalPhase(
    day: LocalDate,
    index: Int,
    periods: List<Period>,
    state: CycleState,
): DotKind {
    val inLoggedPeriod = periods.any { period ->
        val end = period.end ?: period.start.plusDays(state.periodLength - 1L)
        !day.isBefore(period.start) && !day.isAfter(end)
    }
    return if (inLoggedPeriod) DotKind.PERIOD else HormoneEstimator.phaseAt(index, state.cycleLength)
}
