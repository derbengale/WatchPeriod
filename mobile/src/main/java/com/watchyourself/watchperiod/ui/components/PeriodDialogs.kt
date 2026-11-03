package com.watchyourself.watchperiod.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.watchyourself.watchperiod.domain.Period
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val dayFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
private val longDayFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodEditorDialog(
    period: Period?,
    onDismiss: () -> Unit,
    onSave: (LocalDate, LocalDate?) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val today = remember { LocalDate.now() }
    var start by remember(period) { mutableStateOf(period?.start ?: today) }
    var end by remember(period) { mutableStateOf(period?.end) }
    var picker by remember { mutableStateOf<PickerTarget?>(null) }

    val valid = end == null || !end!!.isBefore(start)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (period == null) "Periode nachtragen" else "Periode bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Der Beginn ist Pflicht. Das Ende kannst du offen lassen, solange die Periode läuft.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { picker = PickerTarget.START },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Text("Beginn", style = MaterialTheme.typography.labelSmall)
                        Text(dayFormat.format(start), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                OutlinedButton(
                    onClick = { picker = PickerTarget.END },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Text("Ende", style = MaterialTheme.typography.labelSmall)
                        Text(
                            end?.let { dayFormat.format(it) } ?: "Noch offen",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                if (end != null) {
                    TextButton(onClick = { end = null }) { Text("Ende entfernen") }
                }
                if (!valid) {
                    Text(
                        "Das Ende darf nicht vor dem Beginn liegen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Periode löschen", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onSave(start, end) },
            ) { Text("Speichern", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
    )

    when (picker) {
        PickerTarget.START -> DateFieldPicker(
            initial = start,
            maxDate = today,
            onDismiss = { picker = null },
            onPick = { picked ->
                start = picked
                if (end != null && end!!.isBefore(picked)) end = null
                picker = null
            },
        )
        PickerTarget.END -> DateFieldPicker(
            initial = (end ?: today).notBefore(start),
            minDate = start,
            maxDate = today,
            onDismiss = { picker = null },
            onPick = { picked ->
                end = picked
                picker = null
            },
        )
        null -> Unit
    }
}

private enum class PickerTarget { START, END }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateFieldPicker(
    initial: LocalDate,
    maxDate: LocalDate,
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit,
    minDate: LocalDate? = null,
) {
    val selectable = remember(minDate, maxDate) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = utcTimeMillis.toUtcLocalDate()
                return !date.isAfter(maxDate) && (minDate == null || !date.isBefore(minDate))
            }

            override fun isSelectableYear(year: Int): Boolean =
                year <= maxDate.year && (minDate == null || year >= minDate.year)
        }
    }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.toUtcMillis(),
        selectableDates = selectable,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { onPick(it.toUtcLocalDate()) } ?: onDismiss()
                },
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    ) {
        DatePicker(
            state = state,
            showModeToggle = false,
        )
    }
}

fun formatDay(date: LocalDate): String = dayFormat.format(date)

fun formatLongDay(date: LocalDate): String = longDayFormat.format(date)

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private fun LocalDate.notBefore(other: LocalDate): LocalDate =
    if (isBefore(other)) other else this
