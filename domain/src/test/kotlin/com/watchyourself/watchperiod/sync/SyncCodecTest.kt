package com.watchyourself.watchperiod.sync

import com.watchyourself.watchperiod.domain.CycleEvent
import com.watchyourself.watchperiod.domain.EventType
import com.watchyourself.watchperiod.domain.Hormone
import com.watchyourself.watchperiod.domain.Period
import com.watchyourself.watchperiod.domain.Settings
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncCodecTest {

    private fun snapshot() = SyncSnapshot(
        revision = 1_760_000_000_123L,
        writtenAt = 1_760_000_000_456L,
        origin = "phone:Test",
        periods = listOf(
            Period(1, LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 24)),
            Period(2, LocalDate.of(2026, 11, 1), null),
        ),
        events = listOf(
            CycleEvent(10, LocalDate.of(2026, 10, 22), EventType.ANXIETY),
            CycleEvent(11, LocalDate.of(2026, 10, 22), EventType.MOOD, 2),
        ),
        settings = Settings(
            autoCycle = false,
            cycleLength = 31,
            periodLength = 4,
            visibleHormones = setOf(Hormone.ESTROGEN, Hormone.PROGESTERONE, Hormone.LH),
        ),
    )

    @Test
    fun roundTripKeepsAllData() {
        val original = snapshot()
        val decoded = SyncCodec.decode(SyncCodec.encode(original))

        assertEquals(original, decoded)
    }

    @Test
    fun jsonRoundTripKeepsSettings() {
        val original = snapshot()
        val decoded = SyncCodec.fromJson(SyncCodec.toJson(original))

        assertEquals(original.settings, decoded?.settings)
        assertEquals(original.periods.size, decoded?.periods?.size)
        assertEquals(original.events.size, decoded?.events?.size)
    }

    @Test
    fun garbageIsRejectedInsteadOfCrashing() {
        assertNull(SyncCodec.decode(byteArrayOf(1, 2, 3, 4)))
        assertNull(SyncCodec.decode(ByteArray(0)))
        assertNull(SyncCodec.fromJson("{"))
        assertNull(SyncCodec.fromJson(""))
    }

    @Test
    fun unknownEventTypesAreIgnored() {
        val json = """
            {
              "version": 1,
              "revision": 5,
              "writtenAt": 6,
              "origin": "watch:Test",
              "periods": [],
              "events": [
                {"id": 1, "date": "2026-10-22", "type": "NOT_A_REAL_TYPE", "value": 0},
                {"id": 2, "date": "2026-10-22", "type": "SEX", "value": 0}
              ],
              "settings": {"autoCycle": true, "cycleLength": 28, "periodLength": 5, "visibleHormones": ""}
            }
        """.trimIndent()

        val decoded = SyncCodec.fromJson(json)

        assertEquals(5L, decoded?.revision)
        assertEquals(1, decoded?.events?.size)
        assertTrue(decoded?.settings?.visibleHormones?.isNotEmpty() == true)
    }
}
