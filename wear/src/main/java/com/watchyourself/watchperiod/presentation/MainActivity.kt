package com.watchyourself.watchperiod.presentation

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.watchyourself.watchperiod.domain.Period
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.DatePicker
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.watchyourself.watchperiod.data.PeriodRepository
import com.watchyourself.watchperiod.domain.CycleEvent
import com.watchyourself.watchperiod.domain.DotKind
import com.watchyourself.watchperiod.domain.EventAnalytics
import com.watchyourself.watchperiod.domain.EventStats
import com.watchyourself.watchperiod.domain.EventType
import com.watchyourself.watchperiod.domain.Hormone
import com.watchyourself.watchperiod.domain.HormoneEstimator
import com.watchyourself.watchperiod.domain.PartnerInfo
import com.watchyourself.watchperiod.domain.PmddInfo
import com.watchyourself.watchperiod.domain.Sources
import com.watchyourself.watchperiod.domain.TemperatureEstimator
import com.watchyourself.watchperiod.domain.formatCelsius
import com.watchyourself.watchperiod.domain.formatShift
import com.watchyourself.watchperiod.presentation.theme.WatchPeriodTheme
import com.watchyourself.watchperiod.sync.SyncBus
import com.watchyourself.watchperiod.sync.WatchSync
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var refresh by mutableIntStateOf(0)
    private var launchDate by mutableStateOf<LocalDate?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen() // the drop from splash_icon replaces the default system icon
        super.onCreate(savedInstanceState)
        // Opaque window: the watch face (wallpaper) must not show through anywhere.
        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.BLACK))
        // The decor view does not exist yet – the bar is hidden in onResume/onFocusChanged.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        launchDate = intent?.dayFromDeepLink()
        setContent { WearApp(refresh, launchDate) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchDate = intent.dayFromDeepLink()
        refresh++
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        WatchSync.syncNow(this)
        refresh++ // tile may have changed data while we were away
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    /** Wear OS 8 draws a system status bar with the time over the app – hide it too. */
    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = window.insetsController ?: return
        controller.hide(WindowInsets.Type.statusBars())
        controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

/** `watchperiod://day/2026-10-30` – opened by tapping a selected dot in the tile. */
private const val EXTRA_DAY = "watchperiod.day"

private fun Intent?.dayFromDeepLink(): LocalDate? {
    val raw = this?.getStringExtra(EXTRA_DAY)
        ?: this?.data?.let { uri ->
            if (uri.scheme == "watchperiod" && uri.host == "day") uri.pathSegments.firstOrNull() else null
        }
        ?: return null
    return runCatching { LocalDate.parse(raw) }.getOrNull()
}

private enum class Screen {
    HOME, HISTORY, ADD, SETTINGS, HORMONES, HORMONE_INFO, PMDD_INFO, SOURCES,
    PERIOD_MENU, EDIT_START, EDIT_END, DAY_MENU, ANALYSIS, PARTNER, TEMPERATURE,
}

private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

@Composable
fun WearApp(refreshKey: Int = 0, launchDate: LocalDate? = null) {
    WatchPeriodTheme {
        // No TimeText: the clock at 12 o'clock is hidden while the app is open.
        AppScaffold(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            timeText = {},
        ) {
            val context = LocalContext.current
            val repo = remember { PeriodRepository(context) }
            val syncStamp by SyncBus.changes.collectAsState()
            var version by remember { mutableIntStateOf(0) }
            var screen by remember { mutableStateOf(Screen.HOME) }
            fun changed() { version++ }

            val key = refreshKey + version + syncStamp // re-read DB whenever this changes
            val state = remember(key) { repo.state() }
            val periods = remember(key) { repo.all() }
            val settings = remember(key) { repo.settings() }
            val events = remember(key) { repo.allEvents() }

            var selectedHormone by remember { mutableStateOf(Hormone.ESTROGEN) }
            var menuPeriod by remember { mutableStateOf<Period?>(null) }
            var infoReturn by remember { mutableStateOf(Screen.HORMONES) }
            var sourcesReturn by remember { mutableStateOf(Screen.PMDD_INFO) }
            var temperatureReturn by remember { mutableStateOf(Screen.HOME) }
            var dayOffset by remember { mutableStateOf<Int?>(null) }
            var dayReturn by remember { mutableStateOf(Screen.HOME) }

            // Long press on a tile dot opens the app with "watchperiod://day/<date>".
            LaunchedEffect(launchDate) {
                val date = launchDate ?: return@LaunchedEffect
                val offset = repo.state().offsetFor(date) ?: return@LaunchedEffect
                dayOffset = offset
                dayReturn = Screen.HOME
                screen = Screen.DAY_MENU
            }

            fun openDayMenu(offset: Int, from: Screen) {
                dayOffset = offset
                dayReturn = from
                screen = Screen.DAY_MENU
            }

            BackHandler(enabled = screen != Screen.HOME) {
                screen = when (screen) {
                    Screen.HORMONE_INFO -> infoReturn
                    Screen.PMDD_INFO -> Screen.HORMONES
                    Screen.SOURCES -> sourcesReturn
                    Screen.TEMPERATURE -> temperatureReturn
                    Screen.PERIOD_MENU -> Screen.HISTORY
                    Screen.EDIT_START, Screen.EDIT_END -> Screen.PERIOD_MENU
                    Screen.DAY_MENU -> dayReturn
                    Screen.ANALYSIS, Screen.PARTNER -> Screen.HOME
                    else -> Screen.HOME
                }
            }

            when (screen) {
                Screen.HOME -> {
                    val pagerState = rememberPagerState(pageCount = { 2 })
                    val scope = rememberCoroutineScope()
                    BackHandler(enabled = pagerState.currentPage > 0) {
                        scope.launch { pagerState.animateScrollToPage(0) }
                    }
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                        if (page == 0) {
                            Column(
                                Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                CycleRing(
                                    state,
                                    modifier = Modifier.fillMaxWidth(),
                                    visibleHormones = Hormone.ordered(settings.visibleHormones),
                                    onHormoneClick = {
                                        selectedHormone = it; infoReturn = Screen.HOME; screen = Screen.HORMONE_INFO
                                    },
                                    onDayLongPress = { openDayMenu(it, Screen.HOME) },
                                    onSwipeRight = { scope.launch { pagerState.animateScrollToPage(1) } },
                                )
                            }
                        } else {
                            ListScreen {
                                item { Header("Menü") }
                                item {
                                    if (state.periodOngoing) {
                                        ActionButton("Periode endete heute") { repo.endToday(); changed() }
                                    } else {
                                        ActionButton("Periode begann heute") { repo.startToday(); changed() }
                                    }
                                }
                                if (state.periodPredicted) {
                                    item {
                                        Text(
                                            "Der Periodenstart ist gerade nur errechnet. Bestätige ihn im " +
                                                "Tagesmenü am passenden Tag – dann wird er geloggt.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                item { ActionButton("Heute erfassen") { openDayMenu(state.todayIndex, Screen.HOME) } }
                                item { ActionButton("Auswertung") { screen = Screen.ANALYSIS } }
                                item { ActionButton("Für Partner") { screen = Screen.PARTNER } }
                                item { ActionButton("Hormone") { screen = Screen.HORMONES } }
                                item {
                                    ActionButton("Körpertemperatur") {
                                        temperatureReturn = Screen.HOME
                                        screen = Screen.TEMPERATURE
                                    }
                                }
                                item { ActionButton("Verlauf") { screen = Screen.HISTORY } }
                                item { Header("Hormone anzeigen") }
                                item {
                                    Text(
                                        "★ = Einfluss auf das Wohlbefinden. Mindestens ein Hormon bleibt sichtbar.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Hormone.entries.sortedByDescending { it.moodRelevant }.forEach { h ->
                                    item {
                                        SwitchButton(
                                            checked = h in settings.visibleHormones,
                                            onCheckedChange = { on ->
                                                val keepLast = !on && settings.visibleHormones.size <= 1
                                                if (!keepLast) {
                                                    val next = if (on) settings.visibleHormones + h
                                                    else settings.visibleHormones - h
                                                    repo.saveSettings(settings.copy(visibleHormones = next))
                                                    changed()
                                                }
                                            },
                                            label = { Text(if (h.moodRelevant) "★ ${h.label}" else h.label) },
                                            secondaryLabel = { Text("Stimmung: ${h.moodImpact.label}") },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                                item { Header("Daten") }
                                item { ActionButton("Periode nachtragen") { screen = Screen.ADD } }
                                item { ActionButton("Einstellungen") { screen = Screen.SETTINGS } }
                                item { Spacer(Modifier.height(56.dp)) }
                            }
                        }
                    }
                }

                Screen.HORMONES -> ListScreen {
                    item { Header("Hormone") }
                    if (state.todayIndex < 0) {
                        item { Text("Zuerst eine Periode eintragen") }
                    } else {
                        val levels = HormoneEstimator.levels(state.todayIndex, state.cycleLength)
                        val trend = HormoneEstimator.trend(state.todayIndex, state.cycleLength)
                        item {
                            HormoneBarChart(
                                levels,
                                trend = trend,
                                visible = Hormone.ordered(settings.visibleHormones),
                                onHormoneClick = {
                                    selectedHormone = it; infoReturn = Screen.HORMONES; screen = Screen.HORMONE_INFO
                                },
                            )
                        }
                        item {
                            Text(
                                "Schematische Schätzung – keine Messung. Individuelle Werte können stark abweichen. Kein Diagnoseinstrument.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Hormone.entries.forEach { h ->
                        item {
                            ActionButton(if (h.moodRelevant) "★ ${h.label}" else h.label) {
                                selectedHormone = h; infoReturn = Screen.HORMONES; screen = Screen.HORMONE_INFO
                            }
                        }
                    }
                    item { ActionButton("PMDD/PMDS-Info") { screen = Screen.PMDD_INFO } }
                    item {
                        ActionButton("Körpertemperatur") {
                            temperatureReturn = Screen.HORMONES
                            screen = Screen.TEMPERATURE
                        }
                    }
                    item {
                        ActionButton("Quellen & Hinweise") {
                            sourcesReturn = Screen.HORMONES; screen = Screen.SOURCES
                        }
                    }
                }

                Screen.TEMPERATURE -> ListScreen {
                    item { Header("Körpertemperatur") }
                    if (state.todayIndex < 0) {
                        item { Text("Zuerst eine Periode eintragen") }
                    } else {
                        val phase = state.dots[state.todayIndex]
                        val estimate = TemperatureEstimator.estimate(state.todayIndex, state.cycleLength, phase)
                        item {
                            Text(
                                "Heute geschätzt: ${estimate.valueText}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        item {
                            Text(
                                "${phaseName(phase)} · ${estimate.shiftText} gegenüber der Follikelphase",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        item {
                            Text(
                                "Differenz zu Mann (${formatCelsius(TemperatureEstimator.MALE_REFERENCE)}): " +
                                    formatShift(TemperatureEstimator.differenceToMale(estimate.celsius)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        item {
                            TemperatureChart(
                                values = TemperatureEstimator.curve(state.cycleLength),
                                phases = state.dots,
                                markerIndex = state.todayIndex,
                            )
                        }
                        item { Text(estimate.note, style = MaterialTheme.typography.bodySmall) }
                        item { Text(TemperatureEstimator.EXPLANATION, style = MaterialTheme.typography.bodySmall) }
                        item {
                            Text(
                                TemperatureEstimator.DISCLAIMER,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        item {
                            ActionButton("Quellen & Hinweise") {
                                sourcesReturn = Screen.TEMPERATURE; screen = Screen.SOURCES
                            }
                        }
                        item { Spacer(Modifier.height(56.dp)) }
                    }
                }

                Screen.HORMONE_INFO -> ListScreen {
                    item { Header(selectedHormone.label) }
                    item { Text(selectedHormone.info, style = MaterialTheme.typography.bodySmall) }
                    item { Header("Einfluss auf die Stimmung (${selectedHormone.moodImpact.label})") }
                    item { Text(selectedHormone.mood, style = MaterialTheme.typography.bodySmall) }
                    item {
                        ActionButton("Quellen & Hinweise") {
                            sourcesReturn = Screen.HORMONE_INFO; screen = Screen.SOURCES
                        }
                    }
                    // Extra room so the last lines can scroll fully into view on a round screen
                    item { androidx.compose.foundation.layout.Spacer(Modifier.height(56.dp)) }
                }

                Screen.PMDD_INFO -> ListScreen {
                    item { Header("PMDD/PMDS") }
                    item {
                        Text(
                            PmddInfo.disclaimer,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    PmddInfo.sections.forEach { section ->
                        item { Header(section.title) }
                        item { Text(section.body, style = MaterialTheme.typography.bodySmall) }
                    }
                    item {
                        ActionButton("Quellen & Hinweise") {
                            sourcesReturn = Screen.PMDD_INFO; screen = Screen.SOURCES
                        }
                    }
                    item { androidx.compose.foundation.layout.Spacer(Modifier.height(56.dp)) }
                }

                Screen.SOURCES -> ListScreen {
                    item { Header("Quellen & Hinweise") }
                    item {
                        Text(
                            Sources.disclaimer,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Sources.items.forEach { source ->
                        item { Text(source, style = MaterialTheme.typography.bodySmall) }
                    }
                    item { androidx.compose.foundation.layout.Spacer(Modifier.height(56.dp)) }
                }

                Screen.DAY_MENU -> {
                    val today = LocalDate.now()
                    val offset = dayOffset ?: state.todayIndex
                    val hasCycle = offset >= 0 && state.todayIndex >= 0
                    val date = if (hasCycle) state.dateForOffset(offset) else null
                    val dayIndex = if (hasCycle) offset.mod(state.cycleLength) else -1
                    val counts = if (date != null) remember(key, date) { repo.eventCounts(date) } else emptyMap()
                    val mood = if (date != null) remember(key, date) { repo.moodFor(date) } else null
                    val stats = remember(key) {
                        EventAnalytics.analyse(events, periods, state.cycleLength, today)
                    }
                    ListScreen {
                        item { Header(date?.let { dateFormat.format(it) } ?: "Tag") }
                        if (!hasCycle || date == null) {
                            item { Text("Zuerst eine Periode eintragen") }
                        } else if (date.isAfter(today)) {
                            item { Text("Dieser Tag liegt in der Zukunft – Ereignisse lassen sich erst am Tag selbst eintragen.") }
                        } else {
                            val predicted = state.dots[dayIndex] == DotKind.PREDICTED
                            val suffix = when {
                                predicted -> " · errechnet"
                                state.dots[dayIndex] == DotKind.PERIOD -> " · geloggt"
                                else -> ""
                            }
                            item {
                                Text(
                                    "Tag ${dayIndex + 1} · ${phaseName(state.dots[dayIndex])}$suffix",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            item {
                                Text(
                                    "Ereignisse: Tippen +1 · Lang drücken −1",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            item {
                                MoodStepper(mood) { value ->
                                    if (value == null) repo.deleteMood(date) else repo.saveMood(date, value)
                                    changed()
                                }
                            }
                            EventType.entries.filter { !it.scaled }.forEach { type ->
                                item {
                                    ActionButton(
                                        label = "${type.emoji} ${type.label} · ${counts[type] ?: 0}",
                                        onLongClick = { repo.removeEvent(date, type); changed() },
                                    ) { repo.addEvent(date, type); changed() }
                                }
                            }
                            if (mood != null) {
                                item { ActionButton("Stimmung löschen") { repo.deleteMood(date); changed() } }
                            }
                            item {
                                Text(
                                    dayPatternHint(stats, dayIndex),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (state.periodPredicted && dayIndex in 0 until state.predictedPeriodDays) {
                                item {
                                    ActionButton("Periodenstart hier eintragen") {
                                        repo.startAt(date)
                                        changed()
                                        screen = Screen.HOME
                                    }
                                }
                            }
                        }
                        item { ActionButton("Auswertung") { screen = Screen.ANALYSIS } }
                        item { ActionButton("Fertig") { screen = dayReturn } }
                    }
                }

                Screen.ANALYSIS -> {
                    val today = LocalDate.now()
                    val stats = remember(key) {
                        EventAnalytics.analyse(events, periods, state.cycleLength, today)
                    }
                    ListScreen {
                        item { Header("Auswertung") }
                        item {
                            Text(
                                when {
                                    stats.cycles == 0 ->
                                        "Noch keine Periode eingetragen – trage zuerst eine Periode ein."
                                    stats.isEmpty ->
                                        "Noch keine Einträge. Die Balken zeigen schon die Zyklusphasen; " +
                                            "die Tendenz füllt sich mit deinen Einträgen. " +
                                            "Grundlage: ${stats.cycles} Zyklen à ${stats.cycleLength} Tage."
                                    else ->
                                        "${stats.cycles} Zyklen à ${stats.cycleLength} Tage, " +
                                            "${events.size} Einträge. Tendenz aus deinen eigenen Daten – " +
                                            "je mehr Tage du loggst, desto belastbarer."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        item { PhaseLegend() }
                        EventType.entries.forEach { type ->
                            item { Header("${type.emoji} ${type.label}") }
                            item {
                                EventPatternChart(
                                    values = EventAnalytics.observed(stats, type),
                                    eventColor = eventColor(type),
                                    phases = state.dots,
                                    logged = EventAnalytics.logged(stats, type),
                                    markerIndex = state.todayIndex,
                                    endLabel = "Tag ${stats.cycleLength}",
                                )
                            }
                            if (type.scaled) {
                                item {
                                    Text(
                                        "Balkenhöhe = Stimmung (unten sehr schlecht, oben sehr gut). " +
                                            "Blasser Balken ohne farbige Spitze = kein Eintrag.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            item {
                                Text(
                                    patternSummary(stats, type),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            item {
                                Text(
                                    correlationSummary(stats, type),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        item {
                            Text(
                                "r = Pearson-Korrelation (−1 … +1), r² = erklärter Anteil, " +
                                    "n = Tage mit Daten. Zusammenhang ist keine Ursache; die " +
                                    "Hormonwerte sind ein Schema, keine Messung.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        item { ActionButton("Für Partner") { screen = Screen.PARTNER } }
                        item {
                            ActionButton("Quellen & Hinweise") {
                                sourcesReturn = Screen.ANALYSIS; screen = Screen.SOURCES
                            }
                        }
                        item { Spacer(Modifier.height(56.dp)) }
                    }
                }

                Screen.PARTNER -> {
                    val today = LocalDate.now()
                    val stats = remember(key) {
                        EventAnalytics.analyse(events, periods, state.cycleLength, today)
                    }
                    ListScreen {
                        item { Header("Für Partner") }
                        item { Text(PartnerInfo.headline, style = MaterialTheme.typography.bodySmall) }
                        item {
                            val range = EventAnalytics.sensitiveDays(stats)
                            if (range != null) {
                                Text(
                                    "Tendenz: rücksichtsvolle Tage Tag ${range.first}–${range.last} " +
                                        "(aus deinen Einträgen)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                Text(
                                    "Tendenz noch unklar – sobald du Ereignisse loggst, zeigt sich, " +
                                        "welche Tage bei dir häufiger betroffen sind.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        item {
                            Text(
                                PartnerInfo.disclaimer,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        PartnerInfo.sections.forEach { section ->
                            item { Header(section.title) }
                            item { Text(section.body, style = MaterialTheme.typography.bodySmall) }
                        }
                        item {
                            ActionButton("Quellen & Hinweise") {
                                sourcesReturn = Screen.PARTNER; screen = Screen.SOURCES
                            }
                        }
                        item { Spacer(Modifier.height(56.dp)) }
                    }
                }

                Screen.HISTORY -> {
                    // One chronological list: calculated and logged periods plus logged events.
                    val nextStart = state.nextPeriod
                    val timeline = remember(key) {
                        historyTimeline(periods, events, nextStart, state.periodLength)
                    }
                    ListScreen {
                        item { Header("Verlauf") }
                        item {
                            Text(
                                "Chronologisch \u00B7 Periode antippen f\u00FCr Men\u00FC",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (timeline.isEmpty()) item { Text("Noch keine Eintr\u00E4ge") }
                        timeline.forEach { entry ->
                            item {
                                val text = "${dateFormat.format(entry.date)} \u00B7 ${entry.label}"
                                val period = entry.period
                                if (period != null) {
                                    ActionButton(
                                        text,
                                        onLongClick = { menuPeriod = period; screen = Screen.PERIOD_MENU },
                                    ) { menuPeriod = period; screen = Screen.PERIOD_MENU }
                                } else {
                                    ActionButton(text) {}
                                }
                            }
                        }
                        item { Spacer(Modifier.height(56.dp)) }
                    }
                }

                Screen.PERIOD_MENU -> {
                    val p = menuPeriod
                    ListScreen {
                        item { Header(if (p != null) dateFormat.format(p.start) else "") }
                        item { ActionButton("Startdatum") { screen = Screen.EDIT_START } }
                        item {
                            ActionButton(if (p?.end == null) "Enddatum eintragen" else "Enddatum ändern") {
                                screen = Screen.EDIT_END
                            }
                        }
                        if (p != null && p.end != null) {
                            item {
                                ActionButton("Ende wieder offen") {
                                    repo.update(p.id, p.start, null)
                                    menuPeriod = p.copy(end = null)
                                    changed(); screen = Screen.PERIOD_MENU
                                }
                            }
                        }
                        item {
                            ActionButton("Löschen") {
                                p?.let { repo.delete(it.id) }
                                menuPeriod = null; changed(); screen = Screen.HISTORY
                            }
                        }
                        item { ActionButton("Abbrechen") { screen = Screen.HISTORY } }
                    }
                }

                Screen.EDIT_START -> {
                    val p = menuPeriod
                    if (p == null) {
                        screen = Screen.HISTORY
                    } else {
                        val today = remember { LocalDate.now() }
                        PickerWithLabel("Beginn der Periode") {
                            DatePicker(
                                initialDate = p.start,
                                maxValidDate = today,
                                onDatePicked = { start ->
                                    val end = p.end?.takeIf { !it.isBefore(start) }
                                    repo.update(p.id, start, end)
                                    menuPeriod = p.copy(start = start, end = end)
                                    changed()
                                    screen = Screen.PERIOD_MENU
                                },
                            )
                        }
                    }
                }

                Screen.EDIT_END -> {
                    val p = menuPeriod
                    if (p == null) {
                        screen = Screen.HISTORY
                    } else {
                        val today = remember { LocalDate.now() }
                        val initialEnd = (p.end ?: today).coerceAtMost(today)
                        PickerWithLabel("Ende der Periode") {
                            DatePicker(
                                initialDate = if (initialEnd.isBefore(p.start)) p.start else initialEnd,
                                minValidDate = p.start,
                                maxValidDate = today,
                                onDatePicked = { end ->
                                    repo.update(p.id, p.start, end)
                                    menuPeriod = p.copy(end = end)
                                    changed()
                                    screen = Screen.PERIOD_MENU
                                },
                            )
                        }
                    }
                }

                Screen.ADD -> AddPeriodScreen(
                    periodLength = state.periodLength,
                    onCancel = { screen = Screen.HOME },
                ) { start, end ->
                    repo.add(start, end); changed(); screen = Screen.HOME
                }

                Screen.SETTINGS -> ListScreen {
                    item { Header("Einstellungen") }
                    item {
                        SwitchButton(
                            checked = settings.autoCycle,
                            onCheckedChange = { repo.saveSettings(settings.copy(autoCycle = it)); changed() },
                            label = { Text("Zykluslänge automatisch") },
                            secondaryLabel = { Text("Aktuell ${state.cycleLength} Tage") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        Stepper("Zykluslänge", settings.cycleLength, 20..45, enabled = !settings.autoCycle) {
                            repo.saveSettings(settings.copy(cycleLength = it)); changed()
                        }
                    }
                    item {
                        Stepper("Periodendauer", settings.periodLength, 2..10) {
                            repo.saveSettings(settings.copy(periodLength = it)); changed()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddPeriodScreen(
    periodLength: Int,
    onCancel: () -> Unit,
    onDone: (LocalDate, LocalDate?) -> Unit,
) {
    var start by remember { mutableStateOf<LocalDate?>(null) }
    var choosingEnd by remember { mutableStateOf(false) }
    val today = remember { LocalDate.now() }
    BackHandler(enabled = start != null) {
        if (choosingEnd) choosingEnd = false else start = null
    }
    val s = start
    when {
        s == null -> PickerWithLabel("Beginn der Periode") {
            DatePicker(
                initialDate = today.minusDays(7),
                maxValidDate = today,
                onDatePicked = { start = it },
            )
        }
        !choosingEnd -> ListScreen {
            item { Header("Ende der Periode") }
            item {
                Text(
                    "Beginn: ${dateFormat.format(s)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item { ActionButton("Ende eintragen") { choosingEnd = true } }
            item { ActionButton("Ende noch unbekannt") { onDone(s, null) } }
            item { ActionButton("Abbrechen") { onCancel() } }
        }
        else -> {
            val guess = s.plusDays(periodLength.toLong() - 1L)
            PickerWithLabel("Ende der Periode") {
                DatePicker(
                    initialDate = if (guess.isAfter(today)) today else guess,
                    minValidDate = s,
                    maxValidDate = today,
                    onDatePicked = { onDone(s, it) },
                )
            }
        }
    }
}

/** One row of the history: a calculated or logged date with its label, e.g. `Periode berechnet · Start`. */
private data class HistoryEntry(val date: LocalDate, val label: String, val period: Period? = null)

/** Calculated and logged periods plus logged events, sorted by date. */
private fun historyTimeline(
    periods: List<Period>,
    events: List<CycleEvent>,
    nextStart: LocalDate?,
    periodLength: Int,
): List<HistoryEntry> {
    val entries = mutableListOf<HistoryEntry>()
    if (nextStart != null) {
        entries += HistoryEntry(nextStart, "Periode berechnet \u00B7 Start")
        entries += HistoryEntry(nextStart.plusDays(periodLength - 1L), "Periode berechnet \u00B7 Ende")
    }
    periods.forEach { p ->
        entries += HistoryEntry(p.start, "Periode geloggt \u00B7 Start", p)
        val end = p.end
        if (end != null) {
            entries += HistoryEntry(end, "Periode geloggt \u00B7 Ende", p)
        } else {
            entries += HistoryEntry(p.start.plusDays(periodLength - 1L), "Ende berechnet", p)
        }
    }
    events.groupBy { it.date to it.type }.forEach { (dayType, same) ->
        val type = dayType.second
        val label = if (type.scaled) {
            "${type.emoji} ${type.label}: ${EventType.moodLabel(same.last().value)}"
        } else {
            "${type.emoji} ${type.label}" + if (same.size > 1) " \u00D7 ${same.size}" else ""
        }
        entries += HistoryEntry(dayType.first, label)
    }
    return entries.sortedBy { it.date }
}

@Composable
private fun InfoRow(label: String, date: LocalDate) {
    Text(
        "$label · ${dateFormat.format(date)}",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PickerWithLabel(label: String, picker: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
        picker()
        Text(
            label,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ListScreen(content: TransformingLazyColumnScope.() -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = listState) { padding ->
        TransformingLazyColumn(contentPadding = padding, state = listState, content = content)
    }
}

@Composable
private fun TransformingLazyColumnItemScope.Header(text: String) {
    val spec = rememberTransformationSpec()
    ListHeader(
        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
        transformation = SurfaceTransformation(spec),
    ) { Text(text) }
}

@Composable
private fun TransformingLazyColumnItemScope.ActionButton(
    label: String,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val spec = rememberTransformationSpec()
    Button(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
        transformation = SurfaceTransformation(spec),
    ) { Text(label) }
}

@Composable
private fun TransformingLazyColumnItemScope.Stepper(
    label: String,
    value: Int,
    range: IntRange,
    enabled: Boolean = true,
    onChange: (Int) -> Unit,
) {
    val spec = rememberTransformationSpec()
    Row(
        Modifier.fillMaxWidth().transformedHeight(this, spec),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onChange(value - 1) }, enabled = enabled && value > range.first) { Text("−") }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$value", style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
        IconButton(onClick = { onChange(value + 1) }, enabled = enabled && value < range.last) { Text("+") }
    }
}

/**
 * Mood is a value from −2 to +2, not a count. The two buttons create the first entry when
 * nothing is logged yet (+ → Gut, − → Schlecht); Neutral is reachable from either side.
 */
@Composable
private fun TransformingLazyColumnItemScope.MoodStepper(
    value: Int?,
    onChange: (Int?) -> Unit,
) {
    val spec = rememberTransformationSpec()
    Row(
        Modifier.fillMaxWidth().transformedHeight(this, spec),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { onChange((value ?: 0) - 1) },
            enabled = value == null || value > EventType.MIN_VALUE,
        ) { Text("−") }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (value == null) "Stimmung: –" else "Stimmung: ${EventType.moodLabel(value)}",
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                if (value == null) "− schlechter · + besser"
                else "${EventType.moodEmoji(value)} ${value} · ± ändern",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        IconButton(
            onClick = { onChange((value ?: 0) + 1) },
            enabled = value == null || value < EventType.MAX_VALUE,
        ) { Text("+") }
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

/** Mood has values instead of a count – summarise mean and the best/worst phase. */
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

/**
 * The statistical parameters the user asked for: Pearson r, r² and the number of days for
 * the strongest assumed-hormone curves, followed by a plain-language tendency.
 */
private fun correlationSummary(stats: EventStats, type: EventType): String {
    val list = EventAnalytics.correlations(stats, type)
    if (list.isEmpty()) {
        return "Zusammenhang mit Hormonen: noch zu wenige Daten " +
            "(mind. 4 Einträge bzw. Tage)."
    }
    val lines = list.joinToString("\n") { c ->
        val sign = if (c.r >= 0f) "+" else "−"
        "${c.hormone.label}: r = $sign${decimalText(kotlin.math.abs(c.r))} " +
            "(${EventAnalytics.correlationStrength(c.r)}, r² = ${decimalText(c.r2)}, n = ${c.n})"
    }
    val top = list.first()
    val tendency = when {
        type.scaled && top.r >= 0f -> "Je höher ${top.hormone.label}, desto besser im Mittel die Stimmung."
        type.scaled -> "Je höher ${top.hormone.label}, desto schlechter im Mittel die Stimmung."
        top.r >= 0f -> "Je höher ${top.hormone.label}, desto häufiger ${type.label}."
        else -> "Je höher ${top.hormone.label}, desto seltener ${type.label}."
    }
    return "Zusammenhang mit angenommenen Hormonen " +
        "(stärkste 3 von ${Hormone.entries.size}):\n$lines\nTendenz: $tendency"
}

/** "+0,7" / "−0,7" with a German decimal comma. */
private fun signedDecimal(value: Float): String {
    val text = decimalText(value)
    return if (value > 0f) "+$text" else text
}

/** How likely this single cycle day is, relative to the cycle average. */
private fun dayPatternHint(stats: EventStats, dayIndex: Int): String {
    if (stats.isEmpty) return "Noch keine eigenen Einträge – keine Tendenz für diesen Tag."
    val parts = EventType.entries
        .filter { !it.scaled }
        .mapNotNull { type ->
            val values = EventAnalytics.observed(stats, type)
            val overall = values.average().toFloat()
            if (overall <= 0f || dayIndex !in values.indices) null
            else values[dayIndex].div(overall).takeIf { it >= 1.15f }
                ?.let { "${type.label} ${multiplierText(it)} häufiger" }
        }
        .toMutableList()
    val mood = stats.mood(dayIndex)
    val averageMood = stats.moodAverage
    if (mood != null && averageMood != null && kotlin.math.abs(mood - averageMood) >= 0.5f) {
        parts += "Stimmung ${signedDecimal(mood)} (Ø ${signedDecimal(averageMood)})"
    }
    return if (parts.isEmpty()) {
        "Tendenz: unauffällig – nichts ≥ 1,2× im Zyklusmittel."
    } else {
        "Tendenz an diesem Tag: " + parts.joinToString(", ") + "."
    }
}
