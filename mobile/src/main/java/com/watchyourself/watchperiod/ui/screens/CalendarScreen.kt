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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.watchyourself.watchperiod.data.PeriodRepository
import com.watchyourself.watchperiod.domain.CycleState
import com.watchyourself.watchperiod.domain.DotKind
import com.watchyourself.watchperiod.domain.HormoneEstimator
import com.watchyourself.watchperiod.domain.Period
import com.watchyourself.watchperiod.ui.components.PhaseLegend
import com.watchyourself.watchperiod.ui.components.SectionCard
import com.watchyourself.watchperiod.ui.components.formatDay
import com.watchyourself.watchperiod.ui.theme.dotColor
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun CalendarScreen(
    repo: PeriodRepository,
    state: CycleState,
    periods: List<Period>,
    onOpenDay: (LocalDate) -> Unit,
    onAddPeriod: () -> Unit,
    onEditPeriod: (Period) -> Unit,
) {
    val today = remember { LocalDate.now() }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    val monthFormat = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN) }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Text("Kalender", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Tippe einen Tag an, um Stimmung und Ereignisse zu erfassen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionCard {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { month = month.minusMonths(1) }) {
                        Text("‹", style = MaterialTheme.typography.headlineSmall)
                    }
                    Text(
                        monthFormat.format(month).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { month = month.plusMonths(1) }) {
                        Text("›", style = MaterialTheme.typography.headlineSmall)
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So").forEach { label ->
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                MonthGrid(
                    month = month,
                    today = today,
                    state = state,
                    periods = periods,
                    onOpenDay = onOpenDay,
                )
                PhaseLegend()
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { month = YearMonth.from(today) }) { Text("Heute") }
                    Text(
                        "Punktfarbe = Zyklusphase",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            SectionCard(title = "Perioden") {
                Button(onClick = onAddPeriod, modifier = Modifier.fillMaxWidth()) {
                    Text("Periode nachtragen")
                }
                val predicted = state.nextPeriod
                if (predicted != null) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Nächste Periode (errechnet)", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${formatDay(predicted)} – ${formatDay(predicted.plusDays(state.periodLength - 1L))}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (!predicted.isAfter(today)) {
                                TextButton(onClick = { repo.startAt(predicted) }) {
                                    Text("Als Periodenstart bestätigen")
                                }
                            }
                        }
                    }
                }
                if (periods.isEmpty()) {
                    Text(
                        "Noch keine Periode eingetragen. Beginne mit deiner letzten Periode – " +
                            "der Rest wird berechnet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    periods.sortedByDescending { it.start }.forEach { period ->
                        PeriodRow(period, state.periodLength, onClick = { onEditPeriod(period) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    state: CycleState,
    periods: List<Period>,
    onOpenDay: (LocalDate) -> Unit,
) {
    val leading = month.atDay(1).dayOfWeek.value - 1
    val days = month.lengthOfMonth()
    val cells = ((leading + days + 6) / 7) * 7
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (row in 0 until cells / 7) {
            Row(Modifier.fillMaxWidth()) {
                for (column in 0 until 7) {
                    val cell = row * 7 + column
                    val dayNumber = cell - leading + 1
                    val date = if (dayNumber in 1..days) month.atDay(dayNumber) else null
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(
                                if (date == today) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                            )
                            .clickable(enabled = date != null) { date?.let(onOpenDay) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (date != null) {
                            val kind = phaseFor(date, state, periods)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "$dayNumber",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal,
                                    color = if (date == today) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(2.dp))
                                if (kind != null) {
                                    Box(
                                        Modifier
                                            .size(if (kind == DotKind.PERIOD) 7.dp else 6.dp)
                                            .clip(CircleShape)
                                            .background(dotColor(kind)),
                                    )
                                } else {
                                    Spacer(Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodRow(period: Period, fallbackLength: Int, onClick: () -> Unit) {
    val duration = period.end?.let { ChronoUnit.DAYS.between(period.start, it).toInt() + 1 }
        ?: fallbackLength
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val end = period.end
                Text(
                    if (end != null) {
                        "${formatDay(period.start)} – ${formatDay(end)}"
                    } else {
                        "ab ${formatDay(period.start)}"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "$duration Tage" + if (end == null) " · läuft noch" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

/**
 * Best marker for a calendar cell: a logged period wins over a predicted one, then the
 * schematic phase of that day. Dates before the first logged period stay neutral.
 */
private fun phaseFor(date: LocalDate, state: CycleState, periods: List<Period>): DotKind? {
    val logged = periods.any { period ->
        val end = period.end ?: period.start.plusDays(state.periodLength - 1L)
        !date.isBefore(period.start) && !date.isAfter(end)
    }
    if (logged) return DotKind.PERIOD

    val next = state.nextPeriod
    if (next != null && !date.isBefore(next) && !date.isAfter(next.plusDays(state.periodLength - 1L))) {
        return DotKind.PREDICTED
    }
    val shownOffset = state.offsetFor(date)
    if (state.periodPredicted && shownOffset != null && shownOffset < state.predictedPeriodDays) {
        return DotKind.PREDICTED
    }

    val latestStart = periods.map { it.start }.filter { !it.isAfter(date) }.maxOrNull() ?: return null
    val diff = ChronoUnit.DAYS.between(latestStart, date).toInt()
    val index = diff.mod(state.cycleLength)
    return HormoneEstimator.phaseAt(index, state.cycleLength)
}
