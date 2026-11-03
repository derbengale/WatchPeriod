package com.watchyourself.watchperiod.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.watchyourself.watchperiod.R
import com.watchyourself.watchperiod.data.PeriodRepository
import com.watchyourself.watchperiod.domain.Period
import com.watchyourself.watchperiod.sync.SyncBus
import com.watchyourself.watchperiod.ui.components.DayEditorSheet
import com.watchyourself.watchperiod.ui.components.PeriodEditorDialog
import com.watchyourself.watchperiod.ui.screens.CalendarScreen
import com.watchyourself.watchperiod.ui.screens.HomeScreen
import com.watchyourself.watchperiod.ui.screens.InfoScreen
import com.watchyourself.watchperiod.ui.screens.InsightsScreen
import com.watchyourself.watchperiod.ui.screens.SettingsScreen
import com.watchyourself.watchperiod.ui.theme.WatchPeriodTheme
import java.time.LocalDate

private enum class Tab(val label: String, val icon: Int) {
    TODAY("Heute", R.drawable.ic_nav_today),
    CALENDAR("Kalender", R.drawable.ic_nav_calendar),
    INSIGHTS("Auswertung", R.drawable.ic_nav_insights),
    MORE("Mehr", R.drawable.ic_nav_settings),
}

private data class PeriodDialogState(val period: Period?)

@Composable
fun WatchPeriodApp(repo: PeriodRepository) {
    WatchPeriodTheme {
        val version by SyncBus.changes.collectAsState()
        val today = remember { LocalDate.now() }
        val state = remember(version, today) { repo.state(today) }
        val periods = remember(version) { repo.all() }
        val settings = remember(version) { repo.settings() }
        val events = remember(version) { repo.allEvents() }

        var tabName by rememberSaveable { mutableStateOf(Tab.TODAY.name) }
        var infoName by rememberSaveable { mutableStateOf<String?>(null) }
        var daySheet by rememberSaveable { mutableStateOf<String?>(null) }
        var periodDialog by remember { mutableStateOf<PeriodDialogState?>(null) }

        val tab = Tab.entries.firstOrNull { it.name == tabName } ?: Tab.TODAY
        val info = infoName?.let { name -> InfoPage.entries.firstOrNull { it.name == name } }

        if (info != null) {
            InfoScreen(page = info, onBack = { infoName = null })
        } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                    ) {
                        Tab.entries.forEach { entry ->
                            NavigationBarItem(
                                selected = tab == entry,
                                onClick = { tabName = entry.name },
                                icon = {
                                    Icon(
                                        painter = painterResource(entry.icon),
                                        contentDescription = entry.label,
                                    )
                                },
                                label = { Text(entry.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                            )
                        }
                    }
                },
            ) { padding ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    when (tab) {
                        Tab.TODAY -> HomeScreen(
                            repo = repo,
                            state = state,
                            settings = settings,
                            version = version,
                            onOpenDay = { daySheet = it.toString() },
                            onAddPeriod = { periodDialog = PeriodDialogState(null) },
                        )

                        Tab.CALENDAR -> CalendarScreen(
                            repo = repo,
                            state = state,
                            periods = periods,
                            onOpenDay = { daySheet = it.toString() },
                            onAddPeriod = { periodDialog = PeriodDialogState(null) },
                            onEditPeriod = { periodDialog = PeriodDialogState(it) },
                        )

                        Tab.INSIGHTS -> InsightsScreen(
                            state = state,
                            settings = settings,
                            periods = periods,
                            events = events,
                            onOpenInfo = { infoName = it.name },
                        )

                        Tab.MORE -> SettingsScreen(
                            repo = repo,
                            settings = settings,
                            state = state,
                            version = version,
                            onOpenInfo = { infoName = it.name },
                        )
                    }
                }
            }

            daySheet?.let { iso ->
                val day = runCatching { LocalDate.parse(iso) }.getOrNull()
                if (day != null) {
                    DayEditorSheet(
                        day = day,
                        state = state,
                        periods = periods,
                        repo = repo,
                        version = version,
                        onDismiss = { daySheet = null },
                    )
                }
            }

            periodDialog?.let { dialog ->
                val existing = dialog.period
                PeriodEditorDialog(
                    period = existing,
                    onDismiss = { periodDialog = null },
                    onSave = { start, end ->
                        if (existing == null) repo.add(start, end)
                        else repo.update(existing.id, start, end)
                        periodDialog = null
                    },
                    onDelete = if (existing != null) {
                        {
                            repo.delete(existing.id)
                            periodDialog = null
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}
