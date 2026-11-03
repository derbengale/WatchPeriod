package com.watchyourself.watchperiod.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.watchyourself.watchperiod.domain.CycleCalculator
import com.watchyourself.watchperiod.domain.CycleEvent
import com.watchyourself.watchperiod.domain.CycleState
import com.watchyourself.watchperiod.domain.EventType
import com.watchyourself.watchperiod.domain.Hormone
import com.watchyourself.watchperiod.domain.Period
import com.watchyourself.watchperiod.domain.Settings
import com.watchyourself.watchperiod.sync.PhoneSync
import com.watchyourself.watchperiod.sync.SyncBus
import com.watchyourself.watchperiod.sync.SyncSnapshot
import com.watchyourself.watchperiod.widget.CycleWidgetProvider
import java.time.LocalDate

/** Longest selectable period duration – caps a forgotten open period when a new one starts. */
private const val MAX_OPEN_PERIOD_DAYS = 10L

/**
 * Phone-side storage. Every mutation updates the tile-independent UI flow and pushes a
 * compressed snapshot to the watch through the Wear Data Layer.
 */
class PeriodRepository(context: Context) {
    private val appContext = context.applicationContext
    private val helper = PeriodDb(appContext)

    /** All periods, newest first. */
    fun all(): List<Period> {
        val result = mutableListOf<Period>()
        helper.readableDatabase.query(
            "periods", arrayOf("id", "start_date", "end_date"),
            null, null, null, null, "start_date DESC"
        ).use { c ->
            while (c.moveToNext()) {
                result += Period(
                    id = c.getLong(0),
                    start = LocalDate.parse(c.getString(1)),
                    end = if (c.isNull(2)) null else LocalDate.parse(c.getString(2)),
                )
            }
        }
        return result
    }

    fun openPeriod(): Period? = all().firstOrNull { it.end == null }

    fun startToday(today: LocalDate = LocalDate.now()) = startAt(today)

    /** Logs a period start and closes a forgotten open period the day before. */
    fun startAt(date: LocalDate) {
        val open = openPeriod()
        if (open != null && open.start == date) return
        if (open != null && open.start.isBefore(date)) {
            val cap = open.start.plusDays(MAX_OPEN_PERIOD_DAYS - 1L)
            val yesterday = date.minusDays(1)
            update(open.id, open.start, if (yesterday.isBefore(cap)) yesterday else cap)
        }
        insert(date, null)
    }

    fun endToday(today: LocalDate = LocalDate.now()) {
        val open = openPeriod() ?: return
        val end = if (today.isBefore(open.start)) open.start else today
        helper.writableDatabase.update(
            "periods", ContentValues().apply { put("end_date", end.toString()) },
            "id = ?", arrayOf(open.id.toString())
        )
        notifyChanged()
    }

    fun add(start: LocalDate, end: LocalDate?) {
        if (end == null) startAt(start) else insert(start, end)
    }

    private fun insert(start: LocalDate, end: LocalDate?) {
        helper.writableDatabase.insert("periods", null, ContentValues().apply {
            put("start_date", start.toString())
            if (end != null) put("end_date", end.toString())
        })
        notifyChanged()
    }

    fun update(id: Long, start: LocalDate, end: LocalDate?) {
        helper.writableDatabase.update(
            "periods",
            ContentValues().apply {
                put("start_date", start.toString())
                if (end != null) put("end_date", end.toString()) else putNull("end_date")
            },
            "id = ?", arrayOf(id.toString())
        )
        notifyChanged()
    }

    fun delete(id: Long) {
        helper.writableDatabase.delete("periods", "id = ?", arrayOf(id.toString()))
        notifyChanged()
    }

    // ---- logged events -------------------------------------------------------------------

    fun allEvents(): List<CycleEvent> {
        val result = mutableListOf<CycleEvent>()
        helper.readableDatabase.query(
            "events", arrayOf("id", "day", "type", "value"), null, null, null, null, "day ASC, id ASC"
        ).use { c ->
            while (c.moveToNext()) {
                val type = EventType.entries.firstOrNull { it.name == c.getString(2) } ?: continue
                result += CycleEvent(c.getLong(0), LocalDate.parse(c.getString(1)), type, c.getInt(3))
            }
        }
        return result
    }

    fun eventCounts(day: LocalDate): Map<EventType, Int> {
        val counts = EventType.entries.associateWith { 0 }.toMutableMap()
        helper.readableDatabase.rawQuery(
            "SELECT type, COUNT(*) FROM events WHERE day = ? AND type != ? GROUP BY type",
            arrayOf(day.toString(), EventType.MOOD.name),
        ).use { c ->
            while (c.moveToNext()) {
                val type = EventType.entries.firstOrNull { it.name == c.getString(0) } ?: continue
                counts[type] = c.getInt(1)
            }
        }
        return counts
    }

    fun addEvent(day: LocalDate, type: EventType) {
        helper.writableDatabase.insert("events", null, ContentValues().apply {
            put("day", day.toString())
            put("type", type.name)
            put("value", 0)
        })
        notifyChanged()
    }

    fun removeEvent(day: LocalDate, type: EventType) {
        helper.writableDatabase.execSQL(
            "DELETE FROM events WHERE id = (" +
                "SELECT id FROM events WHERE day = ? AND type = ? ORDER BY id DESC LIMIT 1)",
            arrayOf(day.toString(), type.name),
        )
        notifyChanged()
    }

    fun moodFor(day: LocalDate): Int? = helper.readableDatabase.query(
        "events", arrayOf("value"), "day = ? AND type = ?",
        arrayOf(day.toString(), EventType.MOOD.name), null, null, "id DESC", "1",
    ).use { if (it.moveToFirst()) it.getInt(0) else null }

    fun saveMood(day: LocalDate, value: Int) {
        val clamped = value.coerceIn(EventType.MIN_VALUE, EventType.MAX_VALUE)
        val values = ContentValues().apply { put("value", clamped) }
        val updated = helper.writableDatabase.update(
            "events", values, "day = ? AND type = ?",
            arrayOf(day.toString(), EventType.MOOD.name),
        )
        if (updated == 0) {
            helper.writableDatabase.insert("events", null, ContentValues().apply {
                put("day", day.toString())
                put("type", EventType.MOOD.name)
                put("value", clamped)
            })
        }
        notifyChanged()
    }

    fun deleteMood(day: LocalDate) {
        helper.writableDatabase.delete(
            "events", "day = ? AND type = ?",
            arrayOf(day.toString(), EventType.MOOD.name),
        )
        notifyChanged()
    }

    // ---- settings ------------------------------------------------------------------------

    fun settings(): Settings = Settings(
        autoCycle = getSetting("auto_cycle")?.toBoolean() ?: true,
        cycleLength = getSetting("cycle_length")?.toIntOrNull() ?: 28,
        periodLength = getSetting("period_length")?.toIntOrNull() ?: 5,
        visibleHormones = visibleHormones(),
    )

    private fun visibleHormones(): Set<Hormone> =
        Hormone.parseCsv(getSetting("visible_hormones")).ifEmpty { Hormone.defaultVisible }

    fun saveSettings(s: Settings) {
        setSetting("auto_cycle", s.autoCycle.toString())
        setSetting("cycle_length", s.cycleLength.toString())
        setSetting("period_length", s.periodLength.toString())
        setSetting("visible_hormones", Hormone.toCsv(s.visibleHormones.ifEmpty { Hormone.defaultVisible }))
        notifyChanged()
    }

    fun state(today: LocalDate = LocalDate.now()): CycleState =
        CycleCalculator.compute(all(), settings(), today)

    private fun getSetting(key: String): String? =
        helper.readableDatabase.query(
            "settings", arrayOf("value"), "key = ?", arrayOf(key), null, null, null
        ).use { if (it.moveToFirst()) it.getString(0) else null }

    private fun setSetting(key: String, value: String) {
        helper.writableDatabase.insertWithOnConflict(
            "settings", null,
            ContentValues().apply { put("key", key); put("value", value) },
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    // ---- sync ----------------------------------------------------------------------------

    fun snapshot(revision: Long, origin: String): SyncSnapshot = SyncSnapshot(
        revision = revision,
        writtenAt = System.currentTimeMillis(),
        origin = origin,
        periods = all(),
        events = allEvents(),
        settings = settings(),
    )

    /**
     * Replaces the local database with a remote snapshot in one transaction. Deliberately
     * does not push anything back – applying a snapshot must never start a sync loop.
     */
    fun applySnapshot(snapshot: SyncSnapshot) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            db.delete("periods", null, null)
            snapshot.periods.forEach { period ->
                db.insertWithOnConflict(
                    "periods", null,
                    ContentValues().apply {
                        put("id", period.id)
                        put("start_date", period.start.toString())
                        if (period.end != null) put("end_date", period.end.toString())
                    },
                    SQLiteDatabase.CONFLICT_REPLACE,
                )
            }
            db.delete("events", null, null)
            snapshot.events.forEach { event ->
                db.insertWithOnConflict(
                    "events", null,
                    ContentValues().apply {
                        put("id", event.id)
                        put("day", event.date.toString())
                        put("type", event.type.name)
                        put("value", event.value)
                    },
                    SQLiteDatabase.CONFLICT_REPLACE,
                )
            }
            db.delete("settings", null, null)
            putSetting(db, "auto_cycle", snapshot.settings.autoCycle.toString())
            putSetting(db, "cycle_length", snapshot.settings.cycleLength.toString())
            putSetting(db, "period_length", snapshot.settings.periodLength.toString())
            putSetting(
                db, "visible_hormones",
                Hormone.toCsv(snapshot.settings.visibleHormones.ifEmpty { Hormone.defaultVisible }),
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        SyncBus.changed()
        CycleWidgetProvider.refresh(appContext)
    }

    private fun putSetting(db: SQLiteDatabase, key: String, value: String) {
        db.insertWithOnConflict(
            "settings", null,
            ContentValues().apply { put("key", key); put("value", value) },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    /** Refresh the UI and let the watch know that something changed. */
    private fun notifyChanged() {
        SyncBus.changed()
        CycleWidgetProvider.refresh(appContext)
        PhoneSync.pushLocalAsync(appContext)
    }
}
