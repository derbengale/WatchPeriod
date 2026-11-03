package com.watchyourself.watchperiod

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.watchyourself.watchperiod.data.PeriodRepository
import com.watchyourself.watchperiod.sync.PhoneSync
import com.watchyourself.watchperiod.ui.WatchPeriodApp

class MainActivity : ComponentActivity() {
    private lateinit var repository: PeriodRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository = PeriodRepository(this)
        setContent {
            WatchPeriodApp(repository)
        }
    }

    override fun onResume() {
        super.onResume()
        // Pull the watch state whenever the app comes to the front.
        PhoneSync.syncNow(this)
    }
}