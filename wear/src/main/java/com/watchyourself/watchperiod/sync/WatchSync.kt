package com.watchyourself.watchperiod.sync

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.watchyourself.watchperiod.data.PeriodRepository

/**
 * Watch end of the Data Layer sync – the mirror image of [PhoneSync]. A local edit on the
 * watch is pushed immediately; the app also asks the phone for its newer state on resume.
 */
object WatchSync {
    const val PATH_SNAPSHOT = "/watchperiod/snapshot"
    const val PATH_REQUEST = "/watchperiod/request"

    private const val PREFS = "watchperiod_sync"
    private const val KEY_REVISION = "revision"
    private const val KEY_LAST_SYNC = "last_sync"
    private const val KEY_LAST_ORIGIN = "last_origin"
    private const val TAG = "WatchPeriodSync"

    private val revisionLock = Any()
    private var repositoryInstance: PeriodRepository? = null

    private fun repository(context: Context): PeriodRepository = synchronized(this) {
        repositoryInstance ?: PeriodRepository(context.applicationContext).also { repositoryInstance = it }
    }

    fun origin(context: Context): String = "watch:" + Build.MODEL

    fun lastSyncAt(context: Context): Long =
        prefs(context).getLong(KEY_LAST_SYNC, 0L)

    fun currentRevision(context: Context): Long =
        prefs(context).getLong(KEY_REVISION, 0L)

    private fun nextLocalRevision(context: Context): Long = synchronized(revisionLock) {
        val prefs = prefs(context)
        val next = maxOf(System.currentTimeMillis(), prefs.getLong(KEY_REVISION, 0L) + 1L)
        prefs.edit().putLong(KEY_REVISION, next).apply()
        next
    }

    /** Called after a local database mutation on the watch. */
    fun pushLocalAsync(context: Context) {
        val appContext = context.applicationContext
        Thread({
            runCatching {
                val revision = nextLocalRevision(appContext)
                Log.d(TAG, "watch local push rev=$revision")
                pushSnapshot(appContext, revision, markSynced = true)
            }.onFailure { Log.e(TAG, "watch local push failed", it) }
        }, "watchperiod-watch-push").start()
    }

    /** Sends the current state without bumping the revision. Never sends an empty first state. */
    fun pushCurrentAsync(context: Context) {
        val appContext = context.applicationContext
        Thread({
            runCatching {
                if (currentRevision(appContext) > 0L) {
                    pushSnapshot(appContext, currentRevision(appContext), markSynced = false)
                }
            }
        }, "watchperiod-watch-push-current").start()
    }

    /**
     * Answers a pull request. An empty snapshot with revision 0 is not sent: on a fresh
     * install it would otherwise overwrite the data that already exists on the other device.
     */
    fun respondAsync(context: Context) {
        val appContext = context.applicationContext
        Thread({
            runCatching {
                val revision = currentRevision(appContext)
                val snapshot = repository(appContext).snapshot(revision, origin(appContext))
                if (revision > 0L || snapshot.periods.isNotEmpty() || snapshot.events.isNotEmpty()) {
                    pushSnapshot(appContext, revision, markSynced = false)
                }
            }
        }, "watchperiod-watch-respond").start()
    }

    fun syncNow(context: Context) {
        val appContext = context.applicationContext
        Thread({
            runCatching {
                val revision = currentRevision(appContext)
                if (revision <= 0L) {
                    // Data created before the sync feature: give it a revision and push it once.
                    val snapshot = repository(appContext).snapshot(0L, origin(appContext))
                    if (snapshot.periods.isNotEmpty() || snapshot.events.isNotEmpty()) {
                        pushSnapshot(appContext, nextLocalRevision(appContext), markSynced = false)
                    }
                } else {
                    pushSnapshot(appContext, revision, markSynced = false)
                }
                requestAsync(appContext)
            }
        }, "watchperiod-watch-sync-now").start()
    }

    fun requestAsync(context: Context) {
        val appContext = context.applicationContext
        Wearable.getNodeClient(appContext).connectedNodes
            .addOnSuccessListener { nodes ->
                Log.d(TAG, "watch requests snapshot from ${nodes.size} node(s): ${nodes.map { it.displayName }}")
                val messages = Wearable.getMessageClient(appContext)
                nodes.forEach { node ->
                    messages.sendMessage(node.id, PATH_REQUEST, ByteArray(0))
                        .addOnSuccessListener { Log.d(TAG, "watch request sent to ${node.displayName}") }
                        .addOnFailureListener { Log.e(TAG, "watch request to ${node.displayName} failed", it) }
                }
            }
            .addOnFailureListener { Log.e(TAG, "watch node query failed", it) }
    }

    /** True when the snapshot was newer than everything this device had seen. */
    fun onSnapshot(context: Context, bytes: ByteArray?): Boolean {
        val snapshot = SyncCodec.decode(bytes) ?: run {
            Log.w(TAG, "watch received undecodable snapshot (${bytes?.size ?: 0} bytes)")
            return false
        }
        val appContext = context.applicationContext
        Log.d(
            TAG,
            "watch received snapshot rev=${snapshot.revision} from=${snapshot.origin} " +
                "periods=${snapshot.periods.size} events=${snapshot.events.size}",
        )
        return synchronized(revisionLock) {
            val prefs = prefs(appContext)
            val lastRevision = prefs.getLong(KEY_REVISION, -1L)
            if (snapshot.origin != origin(appContext)) {
                prefs.edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply()
            }
            if (lastRevision >= 0 && snapshot.revision <= lastRevision) {
                Log.d(TAG, "watch ignores snapshot (last=$lastRevision)")
                return@synchronized false
            }

            repository(appContext).applySnapshot(snapshot)
            prefs.edit()
                .putLong(KEY_REVISION, maxOf(lastRevision, snapshot.revision))
                .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                .putString(KEY_LAST_ORIGIN, snapshot.origin)
                .apply()
            Log.d(TAG, "watch applied snapshot rev=${snapshot.revision}")
            true
        }
    }

    private fun pushSnapshot(context: Context, revision: Long, markSynced: Boolean) {
        val snapshot = repository(context).snapshot(revision, origin(context))
        val payload = SyncCodec.encode(snapshot)
        Log.d(
            TAG,
            "watch puts rev=$revision periods=${snapshot.periods.size} " +
                "events=${snapshot.events.size} bytes=${payload.size}",
        )
        val request = PutDataRequest.create(PATH_SNAPSHOT).setData(payload)
        Wearable.getDataClient(context).putDataItem(request)
            .addOnSuccessListener {
                Log.d(TAG, "watch putDataItem ok rev=$revision")
                if (markSynced) {
                    prefs(context).edit()
                        .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                        .apply()
                }
            }
            .addOnFailureListener { Log.e(TAG, "watch putDataItem failed rev=$revision", it) }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
