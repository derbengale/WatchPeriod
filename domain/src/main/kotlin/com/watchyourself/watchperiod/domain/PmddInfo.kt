package com.watchyourself.watchperiod.domain

data class InfoSection(val title: String, val body: String)

/** Educational PMDD/PMDS content. Not a diagnostic or treatment tool. */
object PmddInfo {
    val disclaimer =
        "Diese App ist kein Medizinprodukt und ersetzt keine ärztliche Beratung. " +
            "Die Hormonkurven sind ein schematisches Modell – keine Messung Ihrer individuellen Werte; " +
            "bei unregelmäßigen Zyklen ist das Schema besonders unsicher. " +
            "PMDD/PMDS wird klinisch mit einem Symptomtagebuch diagnostiziert; ein Hormontest reicht dafür nicht. " +
            "Bei starker Beeinträchtigung oder Gedanken an Selbstverletzung suchen Sie bitte professionelle Hilfe."

    val sections = listOf(
        InfoSection(
            "Was ist PMDD/PMDS?",
            "PMDD (deutsch: PMDS, prämenstruelle dysphorische Störung) ist eine schwere, zyklisch auftretende " +
                "Störung von Stimmung und Befinden. Sie betrifft etwa 3–8 % der menstruierenden Frauen und ist " +
                "mehr als „starkes PMS“.",
        ),
        InfoSection(
            "Symptome",
            "Mindestens fünf Symptome, darunter mindestens eines der vier Kernsymptome: Stimmungsschwankungen, " +
                "Reizbarkeit/Wut, depressive Verstimmung/Hoffnungslosigkeit, Angst/Anspannung. Dazu können " +
                "Interessenverlust, Konzentrationsprobleme, Müdigkeit, Appetit- oder Schlafveränderungen, " +
                "Überforderung sowie körperliche Beschwerden wie Blähungen, Brustspannen oder Gelenk- und " +
                "Muskelschmerzen kommen.",
        ),
        InfoSection(
            "Zeitmuster",
            "Die Beschwerden treten in der Lutealphase auf, verstärken sich oft in den letzten 7–10 Tagen vor der " +
                "Periode und bessern sich innerhalb weniger Tage nach Beginn der Menstruation. Sie beeinträchtigen " +
                "Alltag, Arbeit oder Beziehungen.",
        ),
        InfoSection(
            "Ursache",
            "Die Hormonspiegel sind bei PMDD meist normal. Nicht ein Hormonmangel, sondern eine erhöhte " +
                "individuelle Empfindlichkeit gegenüber normalen zyklischen Schwankungen – besonders von " +
                "Progesteron und seinem Metaboliten Allopregnanolon – wird als zentrale Ursache diskutiert. " +
                "GABA-A-Rezeptoren im Gehirn scheinen eine Rolle zu spielen.",
        ),
        InfoSection(
            "Diagnose",
            "PMDD wird klinisch diagnostiziert, nicht durch einen Hormontest. Wichtig sind ein Symptomtagebuch " +
                "über mindestens zwei Zyklen und der Ausschluss anderer Erkrankungen. Die App kann dabei " +
                "unterstützen, ersetzt aber keine ärztliche Diagnose.",
        ),
        InfoSection(
            "Behandlung",
            "Evidenzbasierte Optionen sind unter anderem SSRI (durchgehend oder nur in der Lutealphase), " +
                "kognitive Verhaltenstherapie, bestimmte hormonelle Verhütungsmittel und bei schweren Verläufen " +
                "GnRH-Analoga mit Add-back-Therapie. Die Wahl sollte ärztlich erfolgen.",
        ),
        InfoSection(
            "Wann Hilfe suchen?",
            "Wenn die Beschwerden Leidensdruck erzeugen, den Alltag beeinträchtigen oder Gedanken an " +
                "Selbstverletzung/Suizid auftreten, suchen Sie bitte zeitnah professionelle Hilfe. " +
                "In akuten Krisen: Notruf 112 oder Telefonseelsorge 0800 111 0 111.",
        ),
        InfoSection(
            "Partnerschaft",
            "PMDS belastet Beziehungen messbar: In der letzten Zykluswoche nehmen Konflikte zu, " +
                "Zufriedenheit und gemeinsame schöne Erlebnisse nehmen ab – bei beiden Partnern. " +
                "Die Bindung selbst bleibt dabei meist stabil. Weil das Muster zyklisch ist, lässt sich " +
                "vieles planen statt nur aushalten. Konkrete Hinweise für Partner stehen unter " +
                "„Für Partner“.",
        ),
    )
}
