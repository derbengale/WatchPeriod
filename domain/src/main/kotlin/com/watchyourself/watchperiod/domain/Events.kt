package com.watchyourself.watchperiod.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * A logged event on a single day. Several events of the same type on the same day are
 * allowed — the count is what matters for the pattern statistics. [value] is only used for
 * scaled event types such as mood (−2 … +2).
 */
data class CycleEvent(val id: Long, val date: LocalDate, val type: EventType, val value: Int = 0)

/** Everything the user can log per day, in the order it is shown. */
enum class EventType(
    val label: String,
    val emoji: String,
    val hint: String,
    /** What the literature says about the cyclical distribution – shown as context. */
    val literature: String,
    /** True for a type that is logged on the −2 … +2 scale instead of being counted. */
    val scaled: Boolean = false,
) {
    ANXIETY(
        "Angst", "\uD83D\uDE30",
        "Anspannung, Grübeln, Unruhe",
        "Angst und Anspannung gehören zu den Kernsymptomen von PMDS. Sie treten gehäuft in der " +
            "mittleren bis späten Lutealphase auf und bessern sich meist kurz nach Beginn der Menstruation.",
    ),
    CONFLICT(
        "Streit", "\u26A1",
        "Konflikt, Reizbarkeit, Eskalation",
        "Reizbarkeit und Wut sind Kernsymptome von PMDS. In einer Tagebuchstudie mit beiden Partnern " +
            "nahmen Konflikte in der letzten Zykluswoche zu, während Zufriedenheit und gemeinsame " +
            "schöne Erlebnisse sanken.",
    ),
    SEX(
        "Sex", "\u2764\uFE0F",
        "Nähe, Lust, Intimität",
        "Lust schwankt zyklisch: im Mittel etwas höher um den Eisprung, niedriger in der späten " +
            "Lutealphase. Die individuellen Unterschiede sind aber groß.",
    ),
    MOOD(
        "Stimmung", "\uD83D\uDE42",
        "Skala: 2 sehr gut · 1 gut · 0 neutral · −1 schlecht · −2 sehr schlecht",
        "Tendenz: Die Stimmung ist im Mittel in der späten Follikelphase und um den Eisprung am " +
            "besten und in der späten Lutealphase sowie an den ersten Menstruationstagen am " +
            "niedrigsten. Bei PMDD/PMDS sind diese Schwankungen deutlich stärker ausgeprägt.",
        scaled = true,
    );

    companion object {
        const val MIN_VALUE = -2
        const val MAX_VALUE = 2

        /** 2 → "Sehr gut" … −2 → "Sehr schlecht". */
        fun moodLabel(value: Int): String = when (value.coerceIn(MIN_VALUE, MAX_VALUE)) {
            2 -> "Sehr gut"
            1 -> "Gut"
            0 -> "Neutral"
            -1 -> "Schlecht"
            else -> "Sehr schlecht"
        }

        fun moodEmoji(value: Int): String = when (value.coerceIn(MIN_VALUE, MAX_VALUE)) {
            2 -> "\uD83D\uDE04"
            1 -> "\uD83D\uDE42"
            0 -> "\uD83D\uDE10"
            -1 -> "\uD83D\uDE15"
            else -> "\uD83D\uDE23"
        }
    }
}

/** Counts per cycle day plus the number of cycles that actually covered each day. */
class EventStats(
    val cycleLength: Int,
    /** Number of logged cycles that contributed data (may be 0). */
    val cycles: Int,
    private val perDay: Map<EventType, IntArray>,
    private val observed: IntArray,
    private val moodSum: FloatArray,
    private val moodCount: IntArray,
) {
    fun counts(type: EventType): IntArray = perDay.getValue(type)

    fun total(type: EventType): Int = perDay.getValue(type).sum()

    /** Events per observed cycle for each cycle day (0-based index). */
    fun rate(type: EventType): FloatArray {
        val c = perDay.getValue(type)
        return FloatArray(cycleLength) { i -> if (observed[i] > 0) c[i] / observed[i].toFloat() else 0f }
    }

    /** Number of cycles that covered this cycle day (0-based). */
    fun observedDays(index: Int): Int = observed.getOrElse(index) { 0 }

    /** Mean logged mood (−2 … +2) on this cycle day, or null when nothing was logged. */
    fun mood(index: Int): Float? =
        if (index in 0 until cycleLength && moodCount[index] > 0) moodSum[index] / moodCount[index] else null

    /** Number of days with a mood entry. */
    fun moodDays(): Int = moodCount.sum()

    /** Mean of all logged mood values, or null without entries. */
    val moodAverage: Float?
        get() {
            val count = moodCount.sum()
            return if (count > 0) moodSum.sum() / count else null
        }

    val isEmpty: Boolean get() = EventType.entries.all { total(it) == 0 } && moodDays() == 0
}

/** A cycle phase with its average intensity relative to the cycle average. */
data class PhaseIntensity(val phase: DotKind, val relative: Float)

/** Mean mood per cycle phase. */
data class PhaseMood(val phase: DotKind, val mean: Float, val days: Int)

/** Pearson correlation between an event series and one assumed hormone curve. */
data class HormoneCorrelation(val hormone: Hormone, val r: Float, val r2: Float, val n: Int)

/**
 * Turns logged events into a per-cycle-day profile and blends it with the
 * literature-based expectation. Everything here is a rough, individual estimate —
 * not a prediction and not a diagnosis.
 */
object EventAnalytics {
    /** Days at the very start of a cycle that are treated as menstruation. */
    const val MENSTRUAL_DAYS = 5

    /** 0-based cycle day of [date], clamped into the ring. Null if before the first logged period. */
    fun dayIndex(periodsNewestFirst: List<Period>, date: LocalDate, cycleLength: Int): Int? =
        indexFor(periodsNewestFirst.map { it.start }, date, cycleLength)

    private fun indexFor(starts: List<LocalDate>, date: LocalDate, cycleLength: Int): Int? {
        val start = starts.filter { !it.isAfter(date) }.maxOrNull() ?: return null
        val diff = ChronoUnit.DAYS.between(start, date).toInt()
        if (diff < 0) return null
        return min(diff, cycleLength - 1)
    }

    fun analyse(
        events: List<CycleEvent>,
        periodsNewestFirst: List<Period>,
        cycleLength: Int,
        today: LocalDate,
    ): EventStats {
        val n = cycleLength.coerceAtLeast(1)
        val starts = periodsNewestFirst.map { it.start }.sorted()
        val counts = EventType.entries.associateWith { IntArray(n) }
        val observed = IntArray(n)
        val moodSum = FloatArray(n)
        val moodCount = IntArray(n)
        var cycles = 0

        starts.forEachIndexed { i, start ->
            val nextStart = starts.getOrNull(i + 1) ?: today.plusDays(1)
            val end = minOf(nextStart, today.plusDays(1))
            val span = ChronoUnit.DAYS.between(start, end).toInt()
            if (span <= 0) return@forEachIndexed
            cycles++
            for (day in 0 until min(span, n)) observed[day]++
        }

        events.forEach { event ->
            if (event.date.isAfter(today)) return@forEach
            val index = indexFor(starts, event.date, n) ?: return@forEach
            counts.getValue(event.type)[index]++
            if (event.type.scaled) {
                moodSum[index] += event.value
                moodCount[index]++
            }
            if (observed[index] == 0) observed[index] = 1
        }

        return EventStats(
            cycleLength = n,
            cycles = cycles,
            perDay = counts,
            observed = observed,
            moodSum = moodSum,
            moodCount = moodCount,
        )
    }

    /**
     * Curve of the user's own logged events: events per observed cycle for each cycle day,
     * normalised to 0..1. For mood the value is the logged mood mapped to 0..1 (−2 → 0,
     * +2 → 1), so taller bars mean better mood. Without logged events every value is 0 –
     * nothing is added from the literature, so the chart never shows a pattern the user did
     * not log.
     */
    fun observed(stats: EventStats, type: EventType): FloatArray {
        val n = stats.cycleLength
        if (type.scaled) {
            return FloatArray(n) { i ->
                stats.mood(i)?.let { (it - EventType.MIN_VALUE) / (EventType.MAX_VALUE - EventType.MIN_VALUE).toFloat() }
                    ?: 0f
            }
        }
        val rate = stats.rate(type)
        val maxRate = rate.maxOrNull() ?: 0f
        return if (maxRate <= 0f) FloatArray(n) { 0f } else FloatArray(n) { rate[it] / maxRate }
    }

    /**
     * Which cycle days actually carry a value of this type. Events use a zero count as
     * "nothing happened"; mood needs this array to tell a logged "sehr schlecht" (0) apart
     * from a day without an entry.
     */
    fun logged(stats: EventStats, type: EventType): BooleanArray? =
        if (!type.scaled) null else BooleanArray(stats.cycleLength) { stats.mood(it) != null }

    /** Phase of a 0-based cycle day, with the first days counted as menstruation. */
    fun phaseOfDay(index: Int, cycleLength: Int): DotKind =
        if (index < MENSTRUAL_DAYS) DotKind.PERIOD
        else HormoneEstimator.phaseAt(index, cycleLength)

    /** Average intensity per phase, relative to the cycle average (1.0 = average). */
    fun phaseIntensities(values: FloatArray, cycleLength: Int): List<PhaseIntensity> {
        if (values.isEmpty()) return emptyList()
        val overall = values.average().takeIf { it > 0.0 } ?: return emptyList()
        return values.indices
            .groupBy { phaseOfDay(it, cycleLength) }
            .map { (phase, days) ->
                PhaseIntensity(phase, (days.map { values[it] }.average() / overall).toFloat())
            }
            .sortedByDescending { it.relative }
    }

    /** Mean logged mood per phase, lowest mood first. Empty without mood entries. */
    fun moodByPhase(stats: EventStats): List<PhaseMood> {
        val groups = mutableMapOf<DotKind, MutableList<Float>>()
        for (i in 0 until stats.cycleLength) {
            val value = stats.mood(i) ?: continue
            groups.getOrPut(phaseOfDay(i, stats.cycleLength)) { mutableListOf() } += value
        }
        return groups
            .map { (phase, values) -> PhaseMood(phase, values.average().toFloat(), values.size) }
            .sortedBy { it.mean }
    }

    /**
     * Pearson correlation of this event type with every assumed hormone curve. For counted
     * events the daily rate (per observed cycle) is correlated with the schematic level; for
     * mood only days with an entry are used. Returns the strongest three, or an empty list
     * while the data basis is too thin.
     */
    fun correlations(stats: EventStats, type: EventType): List<HormoneCorrelation> {
        val n = stats.cycleLength
        val days = mutableListOf<Int>()
        val values = mutableListOf<Float>()
        for (i in 0 until n) {
            if (type.scaled) {
                val mood = stats.mood(i) ?: continue
                days += i
                values += mood
            } else {
                if (stats.observedDays(i) <= 0) continue
                days += i
                values += stats.rate(type)[i]
            }
        }
        // With fewer than four points, or only a handful of event days, r is dominated by
        // noise. Show an explicit "not enough data" instead of a random number.
        if (days.size < MIN_CORRELATION_DAYS) return emptyList()
        if (!type.scaled && stats.total(type) < MIN_CORRELATION_DAYS) return emptyList()
        if (values.distinct().size < 2) return emptyList()

        val levels = days.map { HormoneEstimator.levels(it, n) }
        return Hormone.entries
            .mapNotNull { hormone ->
                val curve = levels.mapNotNull { it[hormone] }
                if (curve.size != levels.size) return@mapNotNull null
                val r = pearson(values, curve) ?: return@mapNotNull null
                HormoneCorrelation(hormone, r, r * r, days.size)
            }
            .sortedByDescending { abs(it.r) }
            .take(3)
    }

    /** Pearson r, or null when one of the series has no variance. */
    private fun pearson(x: List<Float>, y: List<Float>): Float? {
        if (x.size < 3 || x.size != y.size) return null
        val mx = x.average()
        val my = y.average()
        var cov = 0.0
        var vx = 0.0
        var vy = 0.0
        for (i in x.indices) {
            val dx = x[i] - mx
            val dy = y[i] - my
            cov += dx * dy
            vx += dx * dx
            vy += dy * dy
        }
        if (vx <= 1e-9 || vy <= 1e-9) return null
        return (cov / sqrt(vx * vy)).toFloat().coerceIn(-1f, 1f)
    }

    /** Plain-language strength of |r|. */
    fun correlationStrength(r: Float): String = when {
        abs(r) < 0.2f -> "sehr schwach"
        abs(r) < 0.4f -> "schwach"
        abs(r) < 0.6f -> "mittel"
        abs(r) < 0.8f -> "stark"
        else -> "sehr stark"
    }

    /** Nearest mood label for a mean value, e.g. 1.4 → "Gut". */
    fun moodLabelFor(value: Float): String = EventType.moodLabel(value.roundToInt())

    private const val MIN_CORRELATION_DAYS = 4

    /**
     * Contiguous 1-based day range around the peak of anxiety + conflict in the user's own
     * logging – the days where extra care is most likely needed. Null without own data.
     */
    fun sensitiveDays(stats: EventStats): IntRange? {
        if (stats.isEmpty) return null
        val n = stats.cycleLength
        val anxiety = observed(stats, EventType.ANXIETY)
        val conflict = observed(stats, EventType.CONFLICT)
        val risk = FloatArray(n) { (anxiety[it] + conflict[it]) / 2f }
        val peak = risk.maxOrNull() ?: return null
        if (peak <= 0f) return null
        val maxIndex = risk.indices.maxByOrNull { risk[it] } ?: return null
        val threshold = peak * 0.8f
        var from = maxIndex
        var to = maxIndex
        while (from > 0 && risk[from - 1] >= threshold) from--
        while (to < n - 1 && risk[to + 1] >= threshold) to++
        if (to - from < 1) return null
        return (from + 1)..(to + 1)
    }
}
