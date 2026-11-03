package com.watchyourself.watchperiod.sync

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/** Receives snapshots and pull requests from the paired phone. */
class WatchSyncService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            if (event.dataItem.uri.path == WatchSync.PATH_SNAPSHOT) {
                WatchSync.onSnapshot(this, event.dataItem.data)
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == WatchSync.PATH_REQUEST) {
            WatchSync.respondAsync(this)
        }
    }
}
