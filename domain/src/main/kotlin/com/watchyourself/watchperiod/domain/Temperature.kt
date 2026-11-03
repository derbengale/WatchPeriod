package com.watchyourself.watchperiod.domain

import java.util.Locale
import kotlin.math.abs

/** Estimated waking (basal) body temperature for one cycle day; a schematic model. */
data class TemperatureEstimate(
    /** Estimated basal temperature in °C. */
    val celsius: Float,
    /** Difference to the low follicular baseline in °C; positive after ovulation. */
    val shift: Float,
    val phase: DotKind,
    val note: String,
) {
    /** e.g. "36,5 °C". */
    val valueText: String get() = formatCelsius(celsius)

    /** e.g. "+0,4 °C", "−0,1 °C" or "±0,0 °C". */
    val shiftText: String get() = formatShift(shift)
}

fun formatCelsius(value: Float): String = String.format(Locale.GERMANY, "%.1f °C", value)

fun formatShift(value: Float): String {
    val rounded = String.format(Locale.GERMANY, "%.1f", abs(value))
    return when {
        value >= 0.05f -> "+$rounded °C"
        value <= -0.05f -> "−$rounded °C"
        else -> "±0,0 °C"
    }
}

/**
 * Schematic basal body temperature (BBT) profile of an ovulatory cycle.
 *
 * Literature: the follicular phase is low (~36.1–36.4 °C), the nadir often lies around
 * ovulation, progesterone raises the temperature by about 0.2–0.5 °C after ovulation
 * (core temperature 0.3–0.7 °C), and it falls again 1–2 days before menstruation.
 * The table below is a 28-day reference cycle with ovulation on day 14; other cycle
 * lengths reuse [HormoneEstimator.referenceDay], so the luteal phase stays 14 days.
 *
 * This is an estimate, not a measurement: sleep, alcohol, illness, shift work and the
 * measurement method shift real values.
 */
object TemperatureEstimator {
    /** Low follicular reference level and baseline for [TemperatureEstimate.shift]. */
    const val FOLLICULAR_BASELINE = 36.30f

    /** Lowest value around ovulation (°C). */
    const val PREOVULATORY_NADIR = 36.20f

    /**
     * Vergleichswert für Männer (°C): unteres Ende des Normalbereichs für
     * gesunde Erwachsene (36,5–37,4 °C). Keine geschlechtsspezifische Basaltemperatur.
     */
    const val MALE_REFERENCE = 36.50f

    /** Difference of [celsius] to the male reference in °C; positive if higher. */
    fun differenceToMale(celsius: Float): Float = celsius - MALE_REFERENCE

    const val DISCLAIMER =
        "Die Körpertemperatur wird aus dem Zyklus schematisch geschätzt. Sie ersetzt keine " +
            "Messung, keine ärztliche Abklärung und ist kein Verhütungsmittel."

    const val EXPLANATION =
        "Nach dem Eisprung steigt die Aufwachtemperatur durch Progesteron um etwa 0,2–0,5 °C " +
            "an und bleibt bis kurz vor der Periode erhöht. Typisch sind etwa 36,1–36,4 °C in " +
            "der Follikelphase und 36,5–36,9 °C in der Lutealphase; der tiefste Wert (Nadir) " +
            "liegt oft am Eisprung, ist aber nicht bei jeder Frau sichtbar. Gemessen wird " +
            "morgens direkt nach dem Aufwachen, vor dem Aufstehen und möglichst zur gleichen " +
            "Zeit. Schlaf, Alkohol, Krankheit, Schichtarbeit und die Messmethode verschieben " +
            "die Werte – Hauttemperaturen von Uhren sind mit der Aufwachtemperatur nicht " +
            "direkt vergleichbar."

    // 28 reference days (day 1..28), ovulation on day 14. Values in °C.
    private val reference = floatArrayOf(
        36.32f, 36.30f, 36.28f, 36.30f, 36.31f, 36.30f, 36.30f, 36.31f, 36.31f, 36.32f,
        36.31f, 36.28f, 36.24f, PREOVULATORY_NADIR,
        36.42f, 36.55f, 36.62f, 36.65f, 36.67f, 36.69f, 36.70f, 36.69f, 36.68f, 36.66f,
        36.62f, 36.55f, 36.45f, 36.35f,
    )

    /** Estimated basal temperature (°C) for a 0-based cycle day. */
    fun celsius(dayIndex: Int, cycleLength: Int): Float =
        HormoneEstimator.sampleReference(reference, HormoneEstimator.referenceDay(dayIndex, cycleLength))

    /** Estimated curve over one whole cycle, one value per day. */
    fun curve(cycleLength: Int): List<Float> = List(cycleLength.coerceAtLeast(1)) { celsius(it, cycleLength) }

    /** Estimate with phase note; [phase] defaults to the calculated phase of the day. */
    fun estimate(
        dayIndex: Int,
        cycleLength: Int,
        phase: DotKind = HormoneEstimator.phaseAt(dayIndex, cycleLength),
    ): TemperatureEstimate {
        val value = celsius(dayIndex, cycleLength)
        val shown = if (phase == DotKind.PREDICTED) DotKind.PERIOD else phase
        return TemperatureEstimate(value, value - FOLLICULAR_BASELINE, shown, noteFor(shown))
    }

    private fun noteFor(phase: DotKind): String = when (phase) {
        DotKind.PERIOD, DotKind.PREDICTED ->
            "Menstruation: niedrige Ausgangslage wie in der Follikelphase."
        DotKind.FOLLICULAR ->
            "Follikelphase: niedrig; kurz vor dem Eisprung oft der tiefste Wert."
        DotKind.OVULATION ->
            "Um den Eisprung herum liegt der tiefste Wert (Nadir) – nicht bei jeder Frau sichtbar."
        DotKind.LUTEAL ->
            "Nach dem Eisprung etwa 0,3–0,5 °C höher; Progesteron hält die Temperatur oben."
        DotKind.PREMENSTRUAL ->
            "Späte Lutealphase: fällt 1–2 Tage vor der Periode wieder auf das Ausgangsniveau."
        DotKind.NORMAL ->
            "Ohne Zyklusdaten keine Schätzung."
    }
}
