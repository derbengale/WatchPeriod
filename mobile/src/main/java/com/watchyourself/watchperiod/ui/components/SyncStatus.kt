package com.watchyourself.watchperiod.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.watchyourself.watchperiod.sync.PhoneSync
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class WatchSyncInfo(
    val nodes: List<Node>,
    val lastSyncAt: Long,
) {
    val connected: Boolean get() = nodes.isNotEmpty()
}

@Composable
fun rememberWatchSyncInfo(refreshKey: Int): WatchSyncInfo {
    val context = LocalContext.current
    var nodes by remember { mutableStateOf<List<Node>>(emptyList()) }
    val lastSyncAt = remember(refreshKey) { PhoneSync.lastSyncAt(context) }
    LaunchedEffect(refreshKey) {
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes = it }
    }
    return WatchSyncInfo(nodes, lastSyncAt)
}

/** Small pill that tells at a glance whether the watch is reachable. */
@Composable
fun SyncBadge(
    info: WatchSyncInfo,
    modifier: Modifier = Modifier,
) {
    val dot = if (info.connected) MaterialTheme.colorScheme.tertiary
    else MaterialTheme.colorScheme.outline
    Row(
        modifier
            .clip(RoundedCornerShape(99.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dot),
        )
        Text(
            if (info.connected) "Uhr verbunden" else "Uhr nicht verbunden",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

fun formatSyncTime(millis: Long): String {
    if (millis <= 0L) return "noch nicht"
    val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN)
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(formatter)
}

@Composable
fun SyncLine(info: WatchSyncInfo, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Zuletzt abgeglichen: ${formatSyncTime(info.lastSyncAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (info.nodes.isNotEmpty()) {
            Spacer(Modifier.width(6.dp))
            Text(
                "· ${info.nodes.firstOrNull()?.displayName ?: "Uhr"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}
