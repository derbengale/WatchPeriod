package com.watchyourself.watchperiod.domain

/** Expected mood for a cycle day; a rough, individual estimate, not a prediction. */
data class MoodForecast(
    val emoji: String,
    val mood: String,
    val irritability: String,
    /** Libido hint, or null when the phase gives no statement worth showing ("Individuell" says nothing). */
    val libido: String? = null,
    val note: String = "",
)

object MoodEstimator {
    fun forPhase(kind: DotKind): MoodForecast = when (kind) {
        DotKind.OVULATION -> MoodForecast(
            "\uD83D\uDE04",
            "Eher gut",
            "Eher niedrig",
            "Oft erhöht",
            "Tendenz: Energie und Lust liegen im Mittel etwas höher als in der Lutealphase.",
        )
        DotKind.FOLLICULAR -> MoodForecast(
            "\uD83D\uDE42",
            "Eher gut",
            "Eher niedrig",
            note = "Tendenz: Stimmung und Energie steigen nach der Menstruation meist an.",
        )
        DotKind.LUTEAL -> MoodForecast(
            "\uD83D\uDE0C",
            "Eher ruhig",
            "Individuell",
            note = "Tendenz: Beschwerden nehmen zur späten Lutealphase hin zu.",
        )
        DotKind.PREMENSTRUAL -> MoodForecast(
            "\uD83D\uDE20",
            "Eher gereizt",
            "Eher erhöht",
            "Eher niedriger",
            "Tendenz: Reizbarkeit und Stimmungstiefs treten hier am häufigsten auf; bei PMDS deutlich.",
        )
        DotKind.PERIOD, DotKind.PREDICTED -> MoodForecast(
            "\uD83D\uDE15",
            "Häufig besser",
            "Eher mittel",
            note = "Tendenz: Nach den ersten Tagen geht es meist bergauf; Regelschmerzen können drücken.",
        )
        DotKind.NORMAL -> MoodForecast(
            "\uD83D\uDE10",
            "Unbekannt",
            "Unbekannt",
        )
    }
}
