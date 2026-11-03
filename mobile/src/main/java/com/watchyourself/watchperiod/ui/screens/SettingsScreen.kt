package com.watchyourself.watchperiod.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.watchyourself.watchperiod.domain.CycleState
import com.watchyourself.watchperiod.domain.Hormone
import com.watchyourself.watchperiod.domain.Settings
import com.watchyourself.watchperiod.data.PeriodRepository
import com.watchyourself.watchperiod.sync.PhoneSync
import com.watchyourself.watchperiod.ui.InfoPage
import com.watchyourself.watchperiod.ui.components.SectionCard
import com.watchyourself.watchperiod.ui.components.SyncBadge
import com.watchyourself.watchperiod.ui.components.SyncLine
import com.watchyourself.watchperiod.ui.components.rememberWatchSyncInfo

@Composable
fun SettingsScreen(
    repo: PeriodRepository,
    settings: Settings,
    state: CycleState,
    version: Int,
    onOpenInfo: (InfoPage) -> Unit,
) {
    val context = LocalContext.current
    val syncInfo = rememberWatchSyncInfo(version)

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Text("Mehr", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Einstellungen, Synchronisation und Hintergründe.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionCard(
                title = "Deine Uhr",
                subtitle = "Daten wandern direkt zwischen Handy und Uhr – ohne Konto und ohne Cloud.",
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    SyncBadge(syncInfo, Modifier.weight(1f))
                    if (syncInfo.nodes.isNotEmpty()) {
                        Text(
                            syncInfo.nodes.first().displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
                SyncLine(syncInfo)
                Button(
                    onClick = { PhoneSync.syncNow(context) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = syncInfo.connected,
                ) { Text("Jetzt synchronisieren") }
                Text(
                    if (syncInfo.connected) {
                        "Änderungen werden bei jedem Eintrag automatisch übertragen. Der Abgleich " +
                            "ergänzt die neuere Version – es gibt keine Cloud und kein Konto."
                    } else {
                        "Öffne WatchPeriod auf der Uhr und stelle sicher, dass beide Geräte " +
                            "verbunden sind. Sobald die Uhr erreichbar ist, gleicht die App automatisch ab."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionCard(title = "Zyklus") {
                SettingSwitch(
                    title = "Zykluslänge automatisch",
                    subtitle = "Aktuell ${state.cycleLength} Tage aus deinen Perioden",
                    checked = settings.autoCycle,
                ) { enabled ->
                    repo.saveSettings(settings.copy(autoCycle = enabled))
                }
                if (!settings.autoCycle) {
                    SettingStepper(
                        label = "Zykluslänge",
                        value = settings.cycleLength,
                        range = 20..45,
                    ) { repo.saveSettings(settings.copy(cycleLength = it)) }
                }
                SettingStepper(
                    label = "Periodendauer",
                    value = settings.periodLength,
                    range = 2..10,
                ) { repo.saveSettings(settings.copy(periodLength = it)) }
                Text(
                    "Die Uhr übernimmt diese Einstellungen automatisch. Bei automatischer " +
                        "Zykluslänge nutzt sie den Mittelwert deiner letzten Perioden.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionCard(
                title = "Hormone anzeigen",
                subtitle = "Mindestens ein Hormon bleibt sichtbar. ★ = Einfluss auf das Wohlbefinden.",
            ) {
                val ordered = remember(settings.visibleHormones) {
                    Hormone.entries.sortedByDescending { it.moodRelevant }
                }
                ordered.forEachIndexed { index, hormone ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (hormone.moodRelevant) "★ ${hormone.label}" else hormone.label,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                "Stimmung: ${hormone.moodImpact.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = hormone in settings.visibleHormones,
                            onCheckedChange = { on ->
                                val visible = settings.visibleHormones
                                if (on || visible.size > 1) {
                                    val next = if (on) visible + hormone else visible - hormone
                                    repo.saveSettings(settings.copy(visibleHormones = next))
                                }
                            },
                        )
                    }
                    if (index < ordered.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }

        item {
            SectionCard(title = "Wissen") {
                OutlinedButton(
                    onClick = { onOpenInfo(InfoPage.PARTNER) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Für Partner") }
                OutlinedButton(
                    onClick = { onOpenInfo(InfoPage.PMDD) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("PMDD/PMDS verstehen") }
                OutlinedButton(
                    onClick = { onOpenInfo(InfoPage.SOURCES) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Quellen & Hinweise") }
            }
        }

        item {
            SectionCard(title = "Über WatchPeriod") {
                Text(
                    "WatchPeriod begleitet deinen Zyklus auf der Uhr und auf dem Handy. " +
                        "Alle Daten bleiben lokal auf deinen Geräten. Es gibt kein Benutzerkonto, " +
                        "keine Cloud und keine Werbung.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Die App ist kein Medizinprodukt und ersetzt keine ärztliche Beratung. " +
                        "Hormon- und Temperaturkurven sind schematische Schätzungen.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingStepper(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { if (value > range.first) onChange(value - 1) },
            enabled = value > range.first,
        ) { Text("−", style = MaterialTheme.typography.titleLarge) }
        Column(
            Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("$value Tage", style = MaterialTheme.typography.titleMedium)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        IconButton(
            onClick = { if (value < range.last) onChange(value + 1) },
            enabled = value < range.last,
        ) { Text("+", style = MaterialTheme.typography.titleLarge) }
    }
}
