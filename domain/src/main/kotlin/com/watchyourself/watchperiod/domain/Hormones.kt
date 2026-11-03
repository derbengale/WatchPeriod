package com.watchyourself.watchperiod.domain

/** How strongly the literature links a hormone to mood and well-being. */
enum class MoodImpact(val label: String, val relevant: Boolean) {
    HIGH("deutlich", true),
    SOME("möglich", true),
    LOW("gering", false),
}

enum class Hormone(
    val label: String,
    val short: String,
    val moodImpact: MoodImpact,
    val info: String,
    val mood: String,
) {
    TESTOSTERONE(
        "Testosteron", "T", MoodImpact.SOME,
        "Wird in Eierstöcken und Nebennieren gebildet. Bei vielen Frauen steigt Testosteron in der späten Follikelphase an, erreicht um den Eisprung einen kleinen Gipfel und sinkt in der Lutealphase wieder ab. Der individuelle Verlauf ist sehr unterschiedlich.",
        "Kann sexuelles Verlangen beeinflussen. Antrieb, Selbstvertrauen und Stimmung werden von vielen Faktoren mitbestimmt; ein niedriger Wert allein erklärt Müdigkeit oder Lustlosigkeit nicht zuverlässig.",
    ),
    FSH(
        "FSH", "FSH", MoodImpact.LOW,
        "Follikelstimulierendes Hormon aus der Hirnanhangsdrüse. Fördert das Heranreifen der Follikel. In der frühen Follikelphase ist es erhöht, fällt dann ab und zeigt um den Eisprung einen kleinen zusätzlichen Gipfel zusammen mit LH.",
        "Keine bekannte direkte Stimmungswirkung. Wirkt indirekt über die von ihm angeregte Östrogenbildung.",
    ),
    LH(
        "LH", "LH", MoodImpact.LOW,
        "Luteinisierendes Hormon aus der Hirnanhangsdrüse. Ein starker LH-Anstieg löst den Eisprung aus, meist etwa 1–2 Tage später.",
        "Rund um den Eisprung berichten manche Frauen von mehr sexuellem Verlangen und Energie. Stimmung und Energie sind aber individuell und lassen sich nicht zuverlässig aus dem LH-Wert ableiten. Manche spüren einen Mittelschmerz.",
    ),
    PROGESTERONE(
        "Progesteron", "P", MoodImpact.HIGH,
        "Wird nach dem Eisprung vom Gelbkörper gebildet, bereitet die Gebärmutter auf eine mögliche Schwangerschaft vor und fällt kurz vor der Periode ab. Sein Abbauprodukt Allopregnanolon wirkt auf GABA-Rezeptoren im Gehirn.",
        "Kann beruhigen und müde machen. Bei manchen Frauen löst Allopregnanolon jedoch Reizbarkeit oder Angst aus, besonders bei PMDD/PMDS. Entscheidend scheinen die zyklischen Schwankungen und die individuelle Empfindlichkeit zu sein, nicht ein einfacher Hormonmangel.",
    ),
    ESTROGEN(
        "Östrogen", "E", MoodImpact.HIGH,
        "Wird in den reifenden Follikeln und im Gelbkörper gebildet. Steigt in der Follikelphase bis kurz vor dem Eisprung, fällt um den Eisprung kurz ab und erreicht in der Mitte der Lutealphase einen zweiten, niedrigeren Gipfel, bevor es prämenstruell absinkt.",
        "Moduliert Serotonin und Dopamin und kann Stimmung, Energie und Konzentration beeinflussen. Die Wirkung ist individuell und weniger geradlinig, als „Östrogen = gute Laune“ suggeriert. Niedrige Werte in der Periode gehen nicht automatisch mit schlechter Stimmung einher; bei PMDD/PMDS sind die Hormonspiegel meist normal.",
    ),
    PROLACTIN(
        "Prolaktin", "PRL", MoodImpact.SOME,
        "Wird in der Hirnanhangsdrüse gebildet, durch Östrogen gefördert und durch Dopamin gebremst. Im Zyklus schwankt es nur mäßig: gemessene Mittelwerte liegen bei etwa 12 µg/L in der Follikelphase, 17 µg/L um den Eisprung und 14 µg/L in der Lutealphase. Sex, Sport, Stress, Schlaf und Mahlzeiten heben die Werte kurzfristig an; dauerhaft hohe Werte können den Zyklus stören.",
        "Nach dem Orgasmus steigt Prolaktin an und wird mit Sättigung und Entspannung verbunden; deutlich erhöhte Werte können Libido und Zyklus dämpfen. Ein einzelner Wert erklärt keine Lustlosigkeit.",
    ),
    OXYTOCIN(
        "Oxytocin", "OT", MoodImpact.SOME,
        "Entsteht im Hypothalamus und wird über die Hirnanhangsdrüse freigesetzt. Bei Frauen vor den Wechseljahren liegen die Werte um den Eisprung und in der Lutealphase etwa doppelt so hoch wie in der Follikelphase; Östrogen fördert die Bildung. Bei sexueller Erregung, Orgasmus, Berührung und beim Stillen steigt Oxytocin stark an.",
        "Wird mit Bindung, Vertrauen und Beruhigung verbunden. Die Studienlage zu Stimmung und Angst ist uneinheitlich – die Wirkung hängt stark von Situation und Person ab.",
    ),
    INHIBIN_A(
        "Inhibin A", "INH", MoodImpact.LOW,
        "Wird von den Granulosazellen der Follikel und vom Gelbkörper gebildet und bremst die FSH-Ausschüttung. Inhibin B gipfelt früh- bis mittelfollikulär und um den Eisprung, Inhibin A erst Mitte der Lutealphase. Beide sind vor allem Laborwerte der Eierstockfunktion.",
        "Kein bekannter direkter Einfluss auf die Stimmung. Die Werte zeigen indirekt, wie aktiv Follikel und Gelbkörper sind.",
    ),
    RELAXIN(
        "Relaxin", "RLX", MoodImpact.LOW,
        "Wird vor allem vom Gelbkörper gebildet. Nach dem Eisprung steigt es an, gipfelt etwa zwei Wochen später und fällt ohne Schwangerschaft wieder ab; dieser Abfall geht der Menstruation voraus. Es lockert glatte Muskulatur, Bänder und Gefäße.",
        "Kein gesicherter direkter Stimmungseinfluss. Manche Frauen bemerken in der späten Lutealphase mehr Gelenk- oder Beckenbeschwerden, was indirekt auf das Wohlbefinden wirken kann.",
    ),
    KISSPEPTIN(
        "Kisspeptin", "KISS", MoodImpact.SOME,
        "Botenstoff des Hypothalamus, der die GnRH-Ausschüttung und damit FSH und LH steuert. Es gilt als Auslöser der Pubertät und des ovulatorischen LH-Gipfels; in der Lutealphase ist es niedriger. Kisspeptin-Gaben wurden bei niedriger Libido untersucht.",
        "Wird mit Libido und sexuellem Verlangen in Verbindung gebracht (Studien mit Einzelinfusionen). Die Datenlage ist vorläufig; Stimmung und Verlangen lassen sich nicht aus einem Wert ableiten.",
    ),
    CORTISOL(
        "Cortisol", "CORT", MoodImpact.HIGH,
        "Zentrales Stresshormon der Nebennierenrinde. Der Verlauf über den Zyklus ist individuell und in Studien uneinheitlich; beschrieben werden leicht höhere Werte in der späten Lutealphase und um die Menstruation. Tagesrhythmus, Schlaf und Belastung überlagern den Zykluseffekt deutlich.",
        "Dauerhafter Stress kann Stimmung, Schlaf und Reizbarkeit beeinflussen. Der Zusammenhang mit der Zyklusphase ist schwach und bei jeder Frau anders – ein grober Hinweis, kein Messwert.",
    ),
    PROSTAGLANDIN(
        "Prostaglandin", "PG", MoodImpact.SOME,
        "Prostaglandine entstehen im Endometrium. F2α verengt die Spiralarterien und zieht die Gebärmutter zusammen – so beginnt die Menstruation, und so entstehen die typischen Regelschmerzen der ersten Tage. Auch Entzündungsbotenstoffe gehören zu dieser Gruppe.",
        "Schmerzen, Krämpfe und Erschöpfung an den ersten Menstruationstagen können die Stimmung drücken – der Effekt läuft über Schmerz und Schlaf, nicht über eine direkte Stimmungswirkung.",
    );

    /** ★ marker in charts, lists and menus. */
    val moodRelevant: Boolean get() = moodImpact.relevant

    companion object {
        /** Shown until the user changes it: everything that affects well-being. */
        val defaultVisible: Set<Hormone> get() = entries.filter { it.moodRelevant }.toSet()

        /** Hormones of [set] in enum (chart) order. */
        fun ordered(set: Collection<Hormone>): List<Hormone> = entries.filter { it in set }

        /** Parses the stored comma separated list; unknown names are ignored. */
        fun parseCsv(raw: String?): Set<Hormone> {
            val names = raw?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: return emptySet()
            return entries.filter { it.name in names }.toSet()
        }

        fun toCsv(hormones: Collection<Hormone>): String =
            entries.filter { it in hormones }.joinToString(",") { it.name }
    }
}

/**
 * Schematic 28-day reference cycle with ovulation on day 14.
 * The luteal phase is kept at 14 days where possible; only the follicular phase
 * is stretched/compressed for other cycle lengths. Very short cycles are clamped.
 */
object HormoneEstimator {
    private const val L = 0.12f // low
    private const val H = 0.9f // high

    const val LUTEAL_LENGTH = 14
    const val PREMENSTRUAL_DAYS = 6
    const val REFERENCE_CYCLE = 28
    const val REFERENCE_OVULATION_DAY = 14 // 1-based

    // 28 entries, day 1..28. Ovulation on day 14.
    // Estrogen: pre-ovulatory peak (13), small dip (14), mid-luteal peak (21).
    private val estrogen = floatArrayOf(
        L, L, L, L, .3f, .4f, .5f, .6f, .7f, .8f, .85f, H, .95f, .6f,
        .5f, .55f, .6f, .65f, .7f, .75f, .8f, .7f, .5f, .35f, .25f, L, L, L,
    )
    // Progesterone: low until ovulation, peak on day 21.
    private val progesterone = floatArrayOf(
        L, L, L, L, L, L, L, L, L, L, L, L, L, .2f,
        .3f, .4f, .5f, .6f, .7f, .8f, H, .8f, .65f, .5f, .35f, .2f, L, L,
    )
    // LH: sharp midcycle surge.
    private val lh = floatArrayOf(
        L, L, L, L, L, L, L, L, L, L, .3f, .6f, H, .5f,
        L, L, L, L, L, L, L, L, L, L, L, L, L, L,
    )
    // FSH: elevated early follicular, mid-follicular decline, ovulatory surge, late-luteal rise.
    private val fsh = floatArrayOf(
        .7f, .7f, .7f, .7f, .65f, .6f, .55f, .5f, .45f, .4f, .45f, .6f, .8f, .6f,
        L, L, L, L, L, L, L, L, L, L, .3f, .5f, .7f, .8f,
    )
    // Testosterone: midcycle peak, gradual luteal decline.
    private val testosterone = floatArrayOf(
        L, L, L, L, .25f, .35f, .45f, .55f, .65f, .7f, .75f, .85f, H, H,
        .85f, .75f, .65f, .55f, .45f, .35f, .25f, L, L, L, L, L, L, L,
    )
    // Prolactin: only mildly cyclical, highest around ovulation (about 17 vs. 12-14 µg/L).
    private val prolactin = floatArrayOf(
        .35f, .35f, .35f, .35f, .34f, .34f, .34f, .34f, .36f, .42f, .5f, .62f, .78f, H,
        .6f, .55f, .52f, .52f, .54f, .56f, .56f, .55f, .52f, .48f, .44f, .4f, .36f, .34f,
    )
    // Oxytocin: about twice as high around ovulation and in the luteal phase.
    private val oxytocin = floatArrayOf(
        .3f, .3f, .3f, .3f, .3f, .32f, .34f, .36f, .4f, .45f, .52f, .6f, .75f, .65f,
        .55f, .55f, .56f, .58f, .6f, .6f, .58f, .55f, .5f, .45f, .4f, .36f, .33f, .3f,
    )
    // Inhibin A: rises after ovulation, peak in the mid-luteal phase.
    private val inhibinA = floatArrayOf(
        .15f, .15f, .15f, .15f, .15f, .16f, .17f, .18f, .2f, .22f, .25f, .3f, .35f, .3f,
        .4f, .5f, .62f, .72f, .8f, .86f, H, .85f, .7f, .55f, .4f, .28f, .2f, .16f,
    )
    // Relaxin: rises after ovulation, peaks about two weeks later and falls without pregnancy.
    private val relaxin = floatArrayOf(
        .1f, .1f, .1f, .1f, .1f, .1f, .1f, .1f, .1f, .1f, .1f, .1f, .12f, .2f,
        .28f, .36f, .44f, .52f, .6f, .66f, .72f, .78f, .84f, .88f, .92f, .95f, .9f, .6f,
    )
    // Kisspeptin: low in the follicular phase, sharp peak with the LH surge.
    private val kisspeptin = floatArrayOf(
        .2f, .2f, .2f, .2f, .2f, .22f, .24f, .26f, .3f, .4f, .55f, .8f, H, .55f,
        .35f, .3f, .3f, .3f, .32f, .34f, .36f, .36f, .34f, .32f, .3f, .28f, .26f, .22f,
    )
    // Cortisol: flat and individual; only a slight late-luteal rise is described.
    private val cortisol = floatArrayOf(
        .5f, .48f, .46f, .44f, .44f, .44f, .45f, .45f, .46f, .46f, .47f, .47f, .48f, .48f,
        .47f, .47f, .48f, .48f, .49f, .5f, .5f, .51f, .52f, .53f, .54f, .55f, .54f, .52f,
    )
    // Prostaglandins: high during menstruation, rising again towards the next period.
    private val prostaglandin = floatArrayOf(
        .95f, .85f, .7f, .5f, .35f, .25f, .2f, .18f, .16f, .15f, .15f, .15f, .16f, .18f,
        .2f, .2f, .2f, .2f, .2f, .2f, .22f, .24f, .26f, .3f, .34f, .4f, .5f, .7f,
    )

    /** 1-based ovulation day for a cycle length. */
    fun ovulationDay(cycleLength: Int): Int {
        val maxDay = (cycleLength - 1).coerceAtLeast(1)
        val minDay = if (maxDay >= 2) 2 else 1
        return (cycleLength - LUTEAL_LENGTH).coerceIn(minDay, maxDay)
    }

    /** 0-based ovulation index. */
    fun ovulationIndex(cycleLength: Int): Int = ovulationDay(cycleLength) - 1

    internal fun sampleReference(table: FloatArray, referenceDay: Float): Float {
        val n = table.size
        val x = (referenceDay - 1f).coerceIn(0f, (n - 1).toFloat())
        val i0 = x.toInt().coerceIn(0, n - 1)
        val i1 = (i0 + 1).coerceAtMost(n - 1)
        val t = x - i0
        return table[i0] * (1 - t) + table[i1] * t
    }

    /**
     * Map a 0-based cycle day to the 28-day reference. The follicular phase
     * (cycle start to ovulation) is stretched/compressed; the luteal phase
     * always spans 14 reference days (15..28).
     */
    internal fun referenceDay(dayIndex: Int, cycleLength: Int): Float {
        val ov = ovulationIndex(cycleLength).coerceAtLeast(1)
        val d = dayIndex.coerceIn(0, cycleLength - 1)
        return if (d <= ov) {
            // 0..ov -> reference day 1..14
            1f + (d.toFloat() / ov) * (REFERENCE_OVULATION_DAY - 1)
        } else {
            val lutealSpan = (cycleLength - ov - 2).coerceAtLeast(1)
            val p = (d - ov - 1).toFloat() / lutealSpan
            15f + p * 13f
        }
    }

    /** Relative level (0..1) per hormone for the given cycle day (0-based). */
    fun levels(dayIndex: Int, cycleLength: Int): Map<Hormone, Float> {
        val ref = referenceDay(dayIndex, cycleLength)
        return mapOf(
            Hormone.TESTOSTERONE to sampleReference(testosterone, ref),
            Hormone.FSH to sampleReference(fsh, ref),
            Hormone.LH to sampleReference(lh, ref),
            Hormone.PROGESTERONE to sampleReference(progesterone, ref),
            Hormone.ESTROGEN to sampleReference(estrogen, ref),
            Hormone.PROLACTIN to sampleReference(prolactin, ref),
            Hormone.OXYTOCIN to sampleReference(oxytocin, ref),
            Hormone.INHIBIN_A to sampleReference(inhibinA, ref),
            Hormone.RELAXIN to sampleReference(relaxin, ref),
            Hormone.KISSPEPTIN to sampleReference(kisspeptin, ref),
            Hormone.CORTISOL to sampleReference(cortisol, ref),
            Hormone.PROSTAGLANDIN to sampleReference(prostaglandin, ref),
        )
    }

    /** Days the trend arrows look ahead. */
    const val TREND_DAYS = 3

    /**
     * Mean daily change of every level over the next [days] days (0-based cycle days wrap
     * around into the next cycle). Positive = rising, negative = falling, 0 = unchanged.
     */
    fun trend(dayIndex: Int, cycleLength: Int, days: Int = TREND_DAYS): Map<Hormone, Float> {
        val n = cycleLength.coerceAtLeast(1)
        val step = days.coerceIn(1, n)
        val now = levels(dayIndex.mod(n), n)
        val later = levels((dayIndex + step).mod(n), n)
        return now.mapValues { (h, v) -> ((later[h] ?: v) - v) / step }
    }

    /** Phase of a 0-based cycle day. Menstruation is overlaid from real data. */
    fun phaseAt(dayIndex: Int, cycleLength: Int): DotKind {
        val ov = ovulationIndex(cycleLength)
        val premenstrualStart = maxOf(ov + 1, cycleLength - PREMENSTRUAL_DAYS)
        return when {
            dayIndex < ov -> DotKind.FOLLICULAR
            dayIndex == ov -> DotKind.OVULATION
            dayIndex < premenstrualStart -> DotKind.LUTEAL
            else -> DotKind.PREMENSTRUAL
        }
    }
}
