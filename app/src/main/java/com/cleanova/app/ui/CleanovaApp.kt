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

@Composable
fun CleanovaApp(vm: HomeViewModel = viewModel()) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("appearance", Context.MODE_PRIVATE) }
    var appearance by remember { mutableStateOf(prefs.getString("theme", "System") ?: "System") }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var permission by remember { mutableStateOf(MediaPermissions.read(context)) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val now = MediaPermissions.read(context)
                if (now != permission || now.hasAny && !now.isFull) vm.clearInventory()
                permission = now
                vm.refresh()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permission = MediaPermissions.read(context)
        if (permission.hasAny) vm.scan() else vm.clearInventory()
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val scan: () -> Unit = {
        if (!permission.hasAny) launcher.launch(MediaPermissions.requested()) else vm.scan()
    }
    BackHandler(settings || tab != 0) { if (settings) settings = false else tab = 0 }
    CleanovaTheme(appearance) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!settings) Surface(
                    modifier = Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(36.dp), shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(Modifier.fillMaxWidth().padding(5.dp)) {
                        val icons = listOf(Icons.Outlined.Home, Icons.Outlined.PhotoLibrary, Icons.Outlined.Storage)
                        listOf("Home", "Media", "Storage").forEachIndexed { index, label ->
                            val selected = tab == index
                            Surface(onClick = { tab = index }, modifier = Modifier.weight(1f).semantics { this.selected = selected },
                                shape = RoundedCornerShape(32.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
                                Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(icons[index], null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }
            }
        ) { padding ->
            AnimatedContent(targetState = if (settings) 3 else tab, modifier = Modifier.padding(padding), label = "navigation") { page ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (page == 3) IconButton(onClick = { settings = false }) { Icon(Icons.Outlined.ArrowBack, "Back") }
                        if (page == 0) Image(painterResource(R.drawable.cleanova_logo), null, Modifier.size(40.dp).padding(end = 6.dp))
                        Text(listOf("Cleanova", "Media", "Storage", "Settings")[page], fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        if (page == 0) IconButton(onClick = { settings = true }) { Icon(Icons.Outlined.Settings, "Settings") }
                    }
                    if (page == 0 || page == 2) {
                        CapacityCard(state, onRefresh = { vm.refresh() })
                    }
                    if (page == 0 || page == 1) {
                        Panel {
                            Icon(Icons.Outlined.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                            val title = if (page == 1) "Your media" else if ((state.capacity?.fraction ?: 0f) >= 0.9f) "Make some room." else "Less clutter. More life."
                            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                            Text("Review the media you allow Cleanova to access. Nothing is deleted.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Media access: " + permission.label, style = MaterialTheme.typography.labelMedium)
                            if (state.scanning) {
                                LinearProgressIndicator(Modifier.fillMaxWidth())
                                OutlinedButton(onClick = { vm.cancelScan() }, modifier = Modifier.fillMaxWidth()) { Text("Cancel scan") }
                            } else Button(onClick = scan, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) { Text("Scan media") }
                            if (permission.hasAny && !permission.isFull && !state.scanning) TextButton(onClick = { launcher.launch(MediaPermissions.requested()) }) { Text("Change media access") }
                            state.scanError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        }
                        state.inventory?.let { result ->
                            Panel {
                                Text("Accessible media", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                Detail("Photos (" + result.photos + ")", if (result.access.images == Grant.NONE) "No access" else gb(result.photoBytes))
                                HorizontalDivider()
                                Detail("Videos (" + result.videos + ")", if (result.access.videos == Grant.NONE) "No access" else gb(result.videoBytes))
                                Text("Media size is not reclaimable storage. Duplicate detection is not included in this build.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    if (page == 2) Panel {
                        Text("Storage information", style = MaterialTheme.typography.titleLarge)
                        Text("Capacity refers to the Android internal data volume. System and app-private files cannot be inspected by this build.")
                        Button(onClick = { vm.refresh() }, modifier = Modifier.fillMaxWidth()) { Text("Refresh storage") }
                    }
                    if (page == 3) {
                        Panel {
                            Text("Appearance", style = MaterialTheme.typography.titleLarge)
                            listOf("System", "Light", "Dark").forEach { option ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = appearance == option, onClick = { appearance = option; prefs.edit().putString("theme", option).apply() })
                                    Text(option)
                                }
                            }
                        }
                        Panel {
                            Text("Privacy", style = MaterialTheme.typography.titleLarge)
                            Text("Media inventory stays on this device. No accounts, network access, ads or file deletion in Foundation v0.1.")
                            Text("Cleanova • 0.1.0", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
