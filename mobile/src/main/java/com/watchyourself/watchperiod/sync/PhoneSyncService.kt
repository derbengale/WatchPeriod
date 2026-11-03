package com.watchyourself.watchperiod.sync

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/** Receives snapshots and pull requests from the paired watch. */
class PhoneSyncService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            if (event.dataItem.uri.path == PhoneSync.PATH_SNAPSHOT) {
                PhoneSync.onSnapshot(this, event.dataItem.data)
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == PhoneSync.PATH_REQUEST) {
            PhoneSync.respondAsync(this)
        }
    }
}
