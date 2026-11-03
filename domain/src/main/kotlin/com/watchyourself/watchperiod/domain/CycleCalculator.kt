package com.watchyourself.watchperiod.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class Period(val id: Long, val start: LocalDate, val end: LocalDate?)

data class Settings(
    val autoCycle: Boolean = true,
    val cycleLength: Int = 28,
    val periodLength: Int = 5,
    /** Hormones shown in the ring, the hormone screen and the tile. */
    val visibleHormones: Set<Hormone> = Hormone.defaultVisible,
)

/** PERIOD = Menstruation, PREDICTED = expected rest of an open menstruation. */
enum class DotKind { NORMAL, PERIOD, PREDICTED, FOLLICULAR, OVULATION, LUTEAL, PREMENSTRUAL }

data class CycleState(
    val cycleLength: Int,
    /** Expected length of a period in days (logged average or the setting). */
    val periodLength: Int = 5,
    /** Estimated ovulation day (1-based). */
    val ovulationDay: Int,
    /** One entry per cycle day; index 0 = first day of the latest logged period. */
    val dots: List<DotKind>,
    /** Index of today's dot, or -1 if nothing logged yet. */
    val todayIndex: Int,
    val periodOngoing: Boolean,
    /** Day number within the current period (1-based) while ongoing. */
    val periodDay: Int?,
    val nextPeriod: LocalDate?,
    /** Negative when the period is late. */
    val daysUntilNext: Int?,
    /** Calendar date of day 1 in the cycle currently shown (null without data). */
    val cycleStart: LocalDate? = null,
    /** 0 = the shown cycle is the last logged one; > 0 = its period start is only calculated. */
    val cyclesSinceLogged: Int = 0,
    /** Number of period days at the start of this cycle that are calculated, not logged. */
    val predictedPeriodDays: Int = 0,
) {
    /** True while the shown period start is calculated rather than logged. */
    val periodPredicted: Boolean get() = cyclesSinceLogged > 0

    /** Last day of the next calculated period. */
    val nextPeriodEnd: LocalDate? get() = nextPeriod?.plusDays(periodLength - 1L)

    /** Calendar date of the 0-based cycle [offset] in the shown cycle (negative = earlier cycles). */
    fun dateForOffset(offset: Int): LocalDate? = cycleStart?.plusDays(offset.toLong())

    /** 0-based cycle offset of [date] inside the shown cycle, or null if it lies before it. */
    fun offsetFor(date: LocalDate): Int? {
        val start = cycleStart ?: return null
        val diff = ChronoUnit.DAYS.between(start, date).toInt()
        return if (diff >= 0) diff % cycleLength else null
    }
}

object CycleCalculator {
    private const val MIN_GAP = 15
    private const val MAX_GAP = 60
    private const val GAPS_USED = 6

    fun compute(periodsNewestFirst: List<Period>, settings: Settings, today: LocalDate): CycleState {
        val periods = periodsNewestFirst.sortedByDescending { it.start }
        val length = cycleLength(periods, settings).coerceAtLeast(1)
        val periodLen = periodLength(periods, settings).coerceAtLeast(1)

        val last = periods.firstOrNull()
        if (last == null) {
            return CycleState(
                cycleLength = length,
                periodLength = periodLen,
                ovulationDay = HormoneEstimator.ovulationDay(length),
                dots = List(length) { DotKind.NORMAL },
                todayIndex = -1,
                periodOngoing = false,
                periodDay = null,
                nextPeriod = null,
                daysUntilNext = null,
            )
        }

        val daysSince = ChronoUnit.DAYS.between(last.start, today).toInt().coerceAtLeast(0)
        // Once a cycle has run past its calculated length, the app shows the next cycle. Its
        // period days are only calculated – never log them silently.
        val cycleNo = daysSince / length
        val cycleStart = last.start.plusDays(cycleNo.toLong() * length)
        val todayIndex = daysSince % length

        // A forgotten open period (end never logged) stops counting as ongoing after the
        // expected period duration instead of growing forever.
        val loggedDays = if (last.end != null) {
            ChronoUnit.DAYS.between(last.start, last.end).toInt() + 1
        } else {
            minOf(daysSince + 1, periodLen)
        }
        val ongoing = last.end == null && daysSince + 1 <= periodLen && daysSince < length

        val dots = MutableList(length) { HormoneEstimator.phaseAt(it, length) }
        var predictedPeriodDays = 0
        if (cycleNo == 0) {
            // Logged period days are solid; the expected rest of an open period is dimmed.
            for (i in 0 until minOf(loggedDays, length)) dots[i] = DotKind.PERIOD
            if (ongoing) {
                for (i in loggedDays until minOf(periodLen, length)) dots[i] = DotKind.PREDICTED
            }
        } else {
            // The calculated period start was never confirmed: mark the whole period as
            // predicted so it can be told apart from a logged period at a glance.
            predictedPeriodDays = minOf(periodLen, length)
            for (i in 0 until predictedPeriodDays) dots[i] = DotKind.PREDICTED
        }

        // While the period is only calculated, the relevant reference is the start of the
        // shown cycle – otherwise the "overdue" count would grow across projected cycles.
        val next = if (cycleNo == 0) last.start.plusDays(length.toLong()) else cycleStart
        return CycleState(
            cycleLength = length,
            periodLength = periodLen,
            ovulationDay = HormoneEstimator.ovulationDay(length),
            dots = dots,
            todayIndex = todayIndex,
            periodOngoing = ongoing,
            periodDay = if (ongoing) daysSince + 1 else null,
            nextPeriod = next,
            daysUntilNext = ChronoUnit.DAYS.between(today, next).toInt(),
            cycleStart = cycleStart,
            cyclesSinceLogged = cycleNo,
            predictedPeriodDays = predictedPeriodDays,
        )
    }

    /** Average of the most recent start-to-start gaps, or the manual setting. */
    fun cycleLength(periodsNewestFirst: List<Period>, settings: Settings): Int {
        if (!settings.autoCycle) return settings.cycleLength
        val gaps = periodsNewestFirst.sortedByDescending { it.start }
            .zipWithNext { newer, older -> ChronoUnit.DAYS.between(older.start, newer.start).toInt() }
            .filter { it in MIN_GAP..MAX_GAP }
            .take(GAPS_USED)
        return if (gaps.isEmpty()) settings.cycleLength else Math.round(gaps.average()).toInt()
    }

    private fun periodLength(periods: List<Period>, settings: Settings): Int {
        val done = periods.filter { it.end != null }.take(GAPS_USED)
            .map { ChronoUnit.DAYS.between(it.start, it.end).toInt() + 1 }
        return if (!settings.autoCycle || done.isEmpty()) settings.periodLength
        else Math.round(done.average()).toInt()
    }
}
