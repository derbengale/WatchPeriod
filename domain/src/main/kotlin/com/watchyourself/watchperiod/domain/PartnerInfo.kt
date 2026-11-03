package com.watchyourself.watchperiod.domain

/**
 * Practical, evidence-informed guidance for the partner of someone with PMDD/PMDS.
 *
 * The individual recommendations are derived from clinical reviews and the small number of
 * studies that looked at couples; they describe what tends to help on average, not what is
 * right for every couple. Not a treatment manual and not a substitute for care.
 */
object PartnerInfo {
    val disclaimer =
        "Diese Hinweise fassen zusammen, was in Studien und klinischen Leitlinien zum Umgang mit " +
            "PMDS beschrieben wird. Sie sind keine Therapie und ersetzen keine ärztliche oder " +
            "psychotherapeutische Behandlung. Jede Person und jede Beziehung ist anders – nimm die " +
            "Punkte als Vorschläge, nicht als Regeln."

    /** Label of the single most important idea, shown as the screen's headline. */
    val headline = "Nicht die Beziehung ist das Problem – der Zyklus verstärkt, was da ist."

    val sections = listOf(
        InfoSection(
            "Was im Körper passiert",
            "PMDS ist keine „schlechte Phase“ und kein Beziehungsproblem. Die Hormonspiegel sind meist " +
                "normal; das Gehirn reagiert empfindlicher auf die normalen Schwankungen von Progesteron " +
                "und seinem Abbauprodukt Allopregnanolon. In der späten Lutealphase steigt dadurch die " +
                "Reizbarkeit, die Ablehnungsempfindlichkeit und die emotionale Verletzlichkeit – und sinkt " +
                "innerhalb weniger Tage nach Beginn der Menstruation wieder. Deine Partnerin ist in dieser " +
                "Zeit nicht „sie selbst“ – und auch nicht sie selbst schuld.",
        ),
        InfoSection(
            "Was in Studien auffällt",
            "In einer Tagebuchstudie über einen ganzen Zyklus, in der beide Partner täglich befragt " +
                "wurden, sanken in der letzten Zykluswoche Zufriedenheit und gemeinsame schöne Erlebnisse, " +
                "während Streit zunahm (Fehlner et al. 2018). Weitere Studien zeigen: Je stärker die " +
                "Symptome, desto stärker die Belastung der Beziehung; Reizbarkeit in der Lutealphase hängt " +
                "besonders mit Beziehungsbeeinträchtigung zusammen. Gleichzeitig bleibt die Bindung selbst " +
                "meist stabil – die Beziehung ist nicht kaputt, sie wird zyklisch belastet.",
        ),
        InfoSection(
            "Was am meisten hilft",
            "Zuhören, ohne zu reparieren. Das Gefühl spiegeln statt das Problem lösen: „Das klingt gerade " +
                "sehr viel.“ Konkrete Entlastung anbieten statt zu fragen („Ich koche heute“ statt „Soll ich " +
                "was machen?“). Rückzug nicht als Liebesentzug deuten – sich zurückzuziehen ist oft " +
                "Reizschutz. Und: Ihre Wahrnehmung nicht in Frage stellen. „Das ist doch nur die Periode“ " +
                "sagt ihr, dass ihr Erleben nicht zählt, auch wenn es biologisch mit dem Zyklus zu tun hat.",
        ),
        InfoSection(
            "Was eher verletzt",
            "Sachdiskussionen mitten in der Emotion, Beweise und „aber letzte Woche hast du gesagt“, " +
                "Diagnosen im Streit („du bist irrational“), unangekündigter Rückzug oder Schweigen, " +
                "Vorwürfe und Nachkarten. Auch gut gemeinte Ratschläge kommen im Akutmoment selten an. " +
                "Verschieben ist keine Ausrede, sondern oft die einzige Möglichkeit, ein Thema fair zu klären.",
        ),
        InfoSection(
            "Wann Rücksicht zählt",
            "Am meisten in den letzten 7–10 Tagen vor der Periode und an den ersten 1–2 " +
                "Menstruationstagen. Genau dort liegen in der Literatur Angst, Reizbarkeit und Konflikte. " +
                    "Diese App zeigt dir unter „Auswertung“, wann bei ihr Angst und Streit in ihren eigenen " +
                "Aufzeichnungen gehäuft vorkommen. Plane schwierige Gespräche, Entscheidungen und große " +
                "Termine in die Follikelphase (etwa Woche 1–2 nach der Periode). In der Spätlutealphase " +
                "geht es um Schadensbegrenzung, nicht um Grundsatzfragen.",
        ),
        InfoSection(
            "Absprachen, die halten",
            "Trefft die Regeln in einer guten Phase, nicht im Streit: ein Signalwort, das „ich bin gerade " +
                "überlastet“ bedeutet; eine Pause von 20–30 Minuten bei Eskalation, mit klarer Rückkehr " +
                "(„ich komme um 20 Uhr wieder“); ein Zeitfenster pro Woche für schwierige Themen; und ein " +
                "Plan für die belasteten Tage (wer kocht, wer die Kinder übernimmt, was gestrichen wird). " +
                "Vorher festgelegte Absprachen funktionieren besser als Improvisation im Ausnahmezustand.",
        ),
        InfoSection(
            "Nähe und Sex",
            "Lust schwankt: In einer Metaanalyse war sexuelles Interesse im Mittel um den Eisprung am " +
                "höchsten und in der Lutealphase niedriger (Roney & Simmons 2018) – mit sehr großen " +
                "individuellen Unterschieden. In der späten Lutealphase kommen körperliche Beschwerden, " +
                "Müdigkeit und Stimmung dazu. Wichtig ist deshalb: kein Druck, kein Beziehungs-Test, keine " +
                "Bewertung von Ablehnung. Nähe anbieten, die nicht Sex bedeutet. Über Wünsche außerhalb " +
                "der angespannten Tage sprechen. Und: Ein geloggter „Sex“-Eintrag ist ein Muster-Datenpunkt, " +
                "kein Maßstab für die Beziehung.",
        ),
        InfoSection(
            "Gemeinsam tracken – ohne Kontrolle",
            "In einer kontrollierten Studie verbesserte ein gemeinsam geführtes Symptomtagebuch die " +
                "Beziehungszufriedenheit (Frank, Dixon & Grosz 1993). Der entscheidende Unterschied ist die " +
                "Rolle: unterstützender Beobachter statt Kontrolleur. Wenn du den Ring siehst, heißt das " +
                "nicht „jetzt ist sie schwierig“, sondern „jetzt braucht sie mehr Ruhe und weniger " +
                "Reibung“. Frag, ob sie deine Mithilfe beim Eintragen überhaupt möchte.",
        ),
        InfoSection(
            "Wenn es eskaliert",
            "Nicht entscheiden, wenn die Emotion am höchsten ist – weder Trennungsgedanken noch " +
                "Grundsatzurteile über die Beziehung. Nach der Akutphase gemeinsam sortieren: was war echt, " +
                "was war der Zyklus, was brauchen wir nächstes Mal. Bei wiederholten schweren Konflikten " +
                "kann eine Paarberatung helfen; sie ersetzt aber keine Behandlung der PMDS. Bei Gewalt, " +
                "Bedrohung oder Gedanken an Selbstverletzung gilt: sofort professionelle Hilfe.",
        ),
        InfoSection(
            "Deine eigene Belastung zählt",
            "Partnerinnen und Partner von Menschen mit PMDS berichten selbst über deutliche Belastung und " +
                "einen hohen Unterstützungsbedarf. Das ist kein Zeichen von Schwäche. Achte auf deine " +
                "Grenzen, plane eigene Auszeiten ein und hol dir Unterstützung, wenn du nur noch " +
                "funktionierst. Eine gute Begleitung hilft ihr mehr als ein erschöpfter Partner.",
        ),
        InfoSection(
            "Was diese App kann – und nicht kann",
                "Die Kurven unter „Auswertung“ verbinden deine Einträge mit einem schematischen Literaturmuster. " +
                "Sie beschreiben Wahrscheinlichkeiten, keine Vorhersagen: Ein ruhiger Tag in der " +
                "Spätlutealphase ist völlig normal, ein schwieriger Tag in der Follikelphase auch. " +
                "PMDS wird klinisch mit einem Symptomtagebuch über mindestens zwei Zyklen diagnostiziert – " +
                "diese App kann das vorbereiten, aber nicht ersetzen.",
        ),
    )
}
