package com.watchyourself.watchperiod.ui.theme

import androidx.compose.ui.graphics.Color
import com.watchyourself.watchperiod.domain.DotKind
import com.watchyourself.watchperiod.domain.EventType

// Brand palette – warm, calm, and readable in both light and dark mode.
val Rose = Color(0xFFE0456B)
val RoseDeep = Color(0xFF8E1739)
val RoseSoft = Color(0xFFFFD9E2)
val Plum = Color(0xFF3A1E2B)
val Cream = Color(0xFFFFF8F7)
val Lavender = Color(0xFF7C63C8)
val LavenderSoft = Color(0xFFE7E0FF)
val Mint = Color(0xFF3E9C78)
val MintSoft = Color(0xFFD8F5E7)
val Peach = Color(0xFFF28A5B)

// Cycle phase colours – the same language as the watch ring.
val PeriodRed = Color(0xFFFF5470)
val PredictedPink = Color(0xFFFF9DB0)
val DotGrey = Color(0xFF9A8B93)
val FollicularGreen = Color(0xFF4CAF7D)
val OvulationBlue = Color(0xFF3E9FE0)
val LutealPurple = Color(0xFF9A7BE0)
val PremenstrualOrange = Color(0xFFF09A3E)

fun dotColor(kind: DotKind): Color = when (kind) {
    DotKind.PERIOD -> PeriodRed
    DotKind.PREDICTED -> PredictedPink
    DotKind.FOLLICULAR -> FollicularGreen
    DotKind.OVULATION -> OvulationBlue
    DotKind.LUTEAL -> LutealPurple
    DotKind.PREMENSTRUAL -> PremenstrualOrange
    DotKind.NORMAL -> DotGrey
}

fun phaseName(kind: DotKind): String = when (kind) {
    DotKind.PERIOD, DotKind.PREDICTED -> "Menstruation"
    DotKind.FOLLICULAR -> "Follikelphase"
    DotKind.OVULATION -> "Eisprung"
    DotKind.LUTEAL -> "Lutealphase"
    DotKind.PREMENSTRUAL -> "Späte Lutealphase"
    DotKind.NORMAL -> "Noch kein Zyklus"
}

fun eventColor(type: EventType): Color = when (type) {
    EventType.ANXIETY -> PremenstrualOrange
    EventType.CONFLICT -> PeriodRed
    EventType.SEX -> LutealPurple
    EventType.MOOD -> OvulationBlue
}