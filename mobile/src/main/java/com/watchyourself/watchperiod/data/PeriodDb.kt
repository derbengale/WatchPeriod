package com.watchyourself.watchperiod.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Local SQLite storage on the phone – same schema as the watch. Dates are ISO strings. */
class PeriodDb(context: Context) : SQLiteOpenHelper(context.applicationContext, "period.db", null, 3) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE periods (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                start_date TEXT NOT NULL,
                end_date TEXT
            )"""
        )
        db.execSQL("CREATE TABLE settings (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
        createEvents(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createEvents(db)
        if (oldVersion >= 2 && oldVersion < 3) {
            db.execSQL("ALTER TABLE events ADD COLUMN value INTEGER NOT NULL DEFAULT 0")
        }
    }

    private fun createEvents(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                day TEXT NOT NULL,
                type TEXT NOT NULL,
                value INTEGER NOT NULL DEFAULT 0
            )"""
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_events_day ON events(day)")
    }
}
