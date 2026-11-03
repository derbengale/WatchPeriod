package com.watchyourself.watchperiod.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * In-process notification that the watch database changed. The UI collects this flow;
 * it is raised both for local edits and for applied snapshots from the phone.
 */
object SyncBus {
    private val state = MutableStateFlow(0)
    val changes: StateFlow<Int> = state

    fun changed() {
        state.update { it + 1 }
    }
}
