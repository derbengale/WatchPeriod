package com.watchyourself.watchperiod.sync

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.watchyourself.watchperiod.data.PeriodRepository

/**
 * Phone end of the Data Layer sync. A local edit pushes a full snapshot; opening the app
 * or pressing "Jetzt synchronisieren" asks the watch for its current state. A snapshot is
 * only applied when its revision is at least as new as the last one seen here.
 */
object PhoneSync {
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

    fun origin(context: Context): String = "phone:" + Build.MODEL

    fun lastSyncAt(context: Context): Long =
        prefs(context).getLong(KEY_LAST_SYNC, 0L)

    fun currentRevision(context: Context): Long =
        prefs(context).getLong(KEY_REVISION, 0L)

    /** Stores the next local revision and returns it; monotonic even with clock skew. */
    private fun nextLocalRevision(context: Context): Long = synchronized(revisionLock) {
        val prefs = prefs(context)
        val next = maxOf(System.currentTimeMillis(), prefs.getLong(KEY_REVISION, 0L) + 1L)
        prefs.edit().putLong(KEY_REVISION, next).apply()
        next
    }

    /** Called after a local database mutation. */
    fun pushLocalAsync(context: Context) {
        val appContext = context.applicationContext
        Thread({
            runCatching {
                val revision = nextLocalRevision(appContext)
                Log.d(TAG, "phone local push rev=$revision")
                pushSnapshot(appContext, revision, markSynced = true)
            }.onFailure { Log.e(TAG, "phone local push failed", it) }
        }, "watchperiod-phone-push").start()
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
        }, "watchperiod-phone-push-current").start()
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
        }, "watchperiod-phone-respond").start()
    }

    /** Pull + push in one go – used on app start and by the button in settings. */
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
        }, "watchperiod-phone-sync-now").start()
    }

    /** Asks every connected watch to send its snapshot. */
    fun requestAsync(context: Context) {
        val appContext = context.applicationContext
        Wearable.getNodeClient(appContext).connectedNodes
            .addOnSuccessListener { nodes ->
                Log.d(TAG, "phone requests snapshot from ${nodes.size} node(s): ${nodes.map { it.displayName }}")
                val messages = Wearable.getMessageClient(appContext)
                nodes.forEach { node ->
                    messages.sendMessage(node.id, PATH_REQUEST, ByteArray(0))
                        .addOnSuccessListener { Log.d(TAG, "request sent to ${node.displayName}") }
                        .addOnFailureListener { Log.e(TAG, "request to ${node.displayName} failed", it) }
                }
            }
            .addOnFailureListener { Log.e(TAG, "phone node query failed", it) }
    }

    /** True when the snapshot was newer than everything this device had seen. */
    fun onSnapshot(context: Context, bytes: ByteArray?): Boolean {
        val snapshot = SyncCodec.decode(bytes) ?: run {
            Log.w(TAG, "phone received undecodable snapshot (${bytes?.size ?: 0} bytes)")
            return false
        }
        val appContext = context.applicationContext
        Log.d(
            TAG,
            "phone received snapshot rev=${snapshot.revision} from=${snapshot.origin} " +
                "periods=${snapshot.periods.size} events=${snapshot.events.size}",
        )
        return synchronized(revisionLock) {
            val prefs = prefs(appContext)
            val lastRevision = prefs.getLong(KEY_REVISION, -1L)
            if (snapshot.origin != origin(appContext)) {
                // Any snapshot from the watch proves that the link works.
                prefs.edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply()
            }
            // Equal revisions carry equal data; only a strictly newer snapshot replaces
            // the local database. The first sync (lastRevision = -1) always applies.
            if (lastRevision >= 0 && snapshot.revision <= lastRevision) {
                Log.d(TAG, "phone ignores snapshot (last=$lastRevision)")
                return@synchronized false
            }

            repository(appContext).applySnapshot(snapshot)
            prefs.edit()
                .putLong(KEY_REVISION, maxOf(lastRevision, snapshot.revision))
                .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                .putString(KEY_LAST_ORIGIN, snapshot.origin)
                .apply()
            Log.d(TAG, "phone applied snapshot rev=${snapshot.revision}")
            true
        }
    }

    private fun pushSnapshot(context: Context, revision: Long, markSynced: Boolean) {
        val snapshot = repository(context).snapshot(revision, origin(context))
        val payload = SyncCodec.encode(snapshot)
        Log.d(
            TAG,
            "phone puts rev=$revision periods=${snapshot.periods.size} " +
                "events=${snapshot.events.size} bytes=${payload.size}",
        )
        val request = PutDataRequest.create(PATH_SNAPSHOT).setData(payload)
        Wearable.getDataClient(context).putDataItem(request)
            .addOnSuccessListener {
                Log.d(TAG, "phone putDataItem ok rev=$revision")
                if (markSynced) {
                    prefs(context).edit()
                        .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                        .apply()
                }
            }
            .addOnFailureListener { Log.e(TAG, "phone putDataItem failed rev=$revision", it) }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
