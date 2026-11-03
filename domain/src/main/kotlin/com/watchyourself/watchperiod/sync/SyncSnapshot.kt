package com.watchyourself.watchperiod.sync

import com.watchyourself.watchperiod.domain.CycleEvent
import com.watchyourself.watchperiod.domain.EventType
import com.watchyourself.watchperiod.domain.Hormone
import com.watchyourself.watchperiod.domain.Period
import com.watchyourself.watchperiod.domain.Settings
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import org.json.JSONArray
import org.json.JSONObject

/**
 * Complete state of one device. The phone and the watch exchange this as a single,
 * gzip-compressed data item – small enough for the Wear Data Layer and robust enough
 * to carry a few years of entries.
 *
 * Conflict rule: the snapshot with the higher [revision] wins as a whole. Each local
 * change bumps the revision, so a forgotten edit on one device cannot silently
 * overwrite newer data from the other one.
 */
data class SyncSnapshot(
    val revision: Long,
    val writtenAt: Long,
    val origin: String,
    val periods: List<Period>,
    val events: List<CycleEvent>,
    val settings: Settings,
)

object SyncCodec {
    const val FORMAT_VERSION = 1

    fun toJson(snapshot: SyncSnapshot): String = JSONObject().apply {
        put("version", FORMAT_VERSION)
        put("revision", snapshot.revision)
        put("writtenAt", snapshot.writtenAt)
        put("origin", snapshot.origin)
        put("periods", JSONArray().apply {
            snapshot.periods.forEach { period ->
                put(JSONObject().apply {
                    put("id", period.id)
                    put("start", period.start.toString())
                    period.end?.let { put("end", it.toString()) }
                })
            }
        })
        put("events", JSONArray().apply {
            snapshot.events.forEach { event ->
                put(JSONObject().apply {
                    put("id", event.id)
                    put("date", event.date.toString())
                    put("type", event.type.name)
                    put("value", event.value)
                })
            }
        })
        put("settings", JSONObject().apply {
            put("autoCycle", snapshot.settings.autoCycle)
            put("cycleLength", snapshot.settings.cycleLength)
            put("periodLength", snapshot.settings.periodLength)
            put("visibleHormones", Hormone.toCsv(snapshot.settings.visibleHormones))
        })
    }.toString()

    fun fromJson(raw: String?): SyncSnapshot? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val root = JSONObject(raw)
            if (root.optInt("version", 0) > FORMAT_VERSION) return null

            val periods = buildList {
                val array = root.optJSONArray("periods") ?: JSONArray()
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val start = item.optString("start").toLocalDateOrNull() ?: continue
                    val end = item.optString("end").toLocalDateOrNull()
                    add(Period(item.optLong("id"), start, end))
                }
            }
            val events = buildList {
                val array = root.optJSONArray("events") ?: JSONArray()
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val date = item.optString("date").toLocalDateOrNull() ?: continue
                    val type = EventType.entries.firstOrNull { it.name == item.optString("type") } ?: continue
                    add(CycleEvent(item.optLong("id"), date, type, item.optInt("value", 0)))
                }
            }
            val settingsJson = root.optJSONObject("settings")
            val visible = Hormone.parseCsv(settingsJson?.optString("visibleHormones"))
                .ifEmpty { Hormone.defaultVisible }
            val settings = Settings(
                autoCycle = settingsJson?.optBoolean("autoCycle", true) ?: true,
                cycleLength = (settingsJson?.optInt("cycleLength", 28) ?: 28).coerceIn(15, 90),
                periodLength = (settingsJson?.optInt("periodLength", 5) ?: 5).coerceIn(1, 20),
                visibleHormones = visible,
            )

            SyncSnapshot(
                revision = root.optLong("revision", 0L).coerceAtLeast(0L),
                writtenAt = root.optLong("writtenAt", 0L),
                origin = root.optString("origin", "unknown"),
                periods = periods,
                events = events,
                settings = settings,
            )
        }.getOrNull()
    }

    /** Gzip-compressed JSON – typically a few kilobytes even with years of entries. */
    fun encode(snapshot: SyncSnapshot): ByteArray {
        val bytes = toJson(snapshot).toByteArray(Charsets.UTF_8)
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(bytes) }
        return out.toByteArray()
    }

    fun decode(bytes: ByteArray?): SyncSnapshot? {
        if (bytes == null || bytes.isEmpty()) return null
        val json = runCatching {
            GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes().toString(Charsets.UTF_8) }
        }.getOrNull() ?: return null
        return fromJson(json)
    }

    private fun String.toLocalDateOrNull(): LocalDate? =
        if (isBlank()) null else runCatching { LocalDate.parse(this) }.getOrNull()
}
