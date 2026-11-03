package com.watchyourself.watchperiod

import com.watchyourself.watchperiod.domain.DotKind
import com.watchyourself.watchperiod.domain.MoodEstimator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodEstimatorTest {
    @Test
    fun libidoHintIsEitherInformativeOrAbsent() {
        DotKind.values().forEach { kind ->
            val libido = MoodEstimator.forPhase(kind).libido
            // Nichts ist besser als ein Platzhalter wie "Individuell" oder "Unbekannt".
            assertTrue(libido == null || (libido.isNotBlank() && libido !in PLACEHOLDER_HINTS))
        }
    }

    private companion object {
        val PLACEHOLDER_HINTS = setOf("Individuell", "Unbekannt")
    }

    @Test
    fun noAbsoluteMoodClaims() {
        assertFalse(MoodEstimator.forPhase(DotKind.OVULATION).mood == "Sehr gut")
        assertFalse(MoodEstimator.forPhase(DotKind.PREMENSTRUAL).irritability == "Hoch")
    }
}
