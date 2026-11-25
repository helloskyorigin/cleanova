package com.cleanova.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanova.app.HomeState
import java.text.NumberFormat

internal fun gb(bytes: Long): String = NumberFormat.getNumberInstance().apply {
    maximumFractionDigits = 1
}.format(bytes.coerceAtLeast(0) / 1_000_000_000.0) + " GB"

@Composable internal fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(CleanovaRadius.card), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(CleanovaSpacing.panel), verticalArrangement = Arrangement.spacedBy(CleanovaSpacing.medium), content = content)
    }
}
@Composable internal fun Detail(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CleanovaSpacing.small)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}
@Composable internal fun PrimaryAction(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(CleanovaRadius.button)) {
        Text(label)
    }
}
@Composable internal fun CapacityCard(state: HomeState, onRefresh: () -> Unit) = Panel {
    Text("Device storage", color = MaterialTheme.colorScheme.onSurfaceVariant)
    val cap = state.capacity
    if (cap == null) {
        Text(if (state.capacityError) "Storage unavailable" else "Reading device storage…")
        if (state.capacityError) TextButton(onClick = onRefresh) { Text("Retry") }
    } else {
        Text(gb(cap.available), fontSize = 42.sp, fontWeight = FontWeight.Bold)
        Text("Available", color = MaterialTheme.colorScheme.onSurfaceVariant)
        val progress by animateFloatAsState(cap.fraction, tween(CleanovaMotion.storageProgressMs), label = "storage")
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(10.dp))
        Detail("Used", gb(cap.used))
        Detail("Capacity", gb(cap.total))
        if (state.capacityError) Text("Last successful reading. Refresh to retry.", style = MaterialTheme.typography.bodySmall)
    }
}
