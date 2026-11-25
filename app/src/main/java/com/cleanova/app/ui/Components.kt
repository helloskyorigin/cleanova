package com.cleanova.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cleanova.app.R
import com.cleanova.app.HomeViewModel
import com.cleanova.app.HomeState
import com.cleanova.app.core.permissions.MediaPermissions
import com.cleanova.app.domain.Grant
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import java.text.NumberFormat

internal fun gb(bytes: Long): String = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }.format(bytes / 1_000_000_000.0) + " GB"
@Composable internal fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
@Composable internal fun Detail(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
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
        val progress by animateFloatAsState(cap.fraction, tween(450), label = "storage")
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(10.dp))
        Detail("Used", gb(cap.used))
        Detail("Capacity", gb(cap.total))
        if (state.capacityError) Text("Last successful reading. Refresh to retry.", style = MaterialTheme.typography.bodySmall)
    }
}
