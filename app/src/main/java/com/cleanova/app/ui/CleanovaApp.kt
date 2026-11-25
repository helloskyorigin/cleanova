package com.cleanova.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cleanova.app.features.home.HomeScreen
import com.cleanova.app.HomeViewModel
import com.cleanova.app.core.navigation.Destination
import com.cleanova.app.core.navigation.NavigationState
import com.cleanova.app.core.permissions.MediaPermissions

@Composable
fun CleanovaApp(vm: HomeViewModel = viewModel()) {
    val context = LocalContext.current
    var tabName by rememberSaveable { mutableStateOf(Destination.HOME.name) }
    var settings by rememberSaveable { mutableStateOf(false) }
    val navigation = NavigationState(Destination.valueOf(tabName), settings)
    fun navigate(next: NavigationState) {
        tabName = next.tab.name
        settings = next.settingsOpen
    }
    var permission by remember { mutableStateOf(MediaPermissions.read(context)) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, context, vm) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val now = MediaPermissions.read(context)
                permission = now
                vm.onResume()
            } else if (event == Lifecycle.Event.ON_STOP) {
                vm.cancelScan()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.clearInventory()
        permission = MediaPermissions.read(context)
        // Explicitly scan after grant, not during the permission/lifecycle transition.
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val requestAccess: () -> Unit = { launcher.launch(MediaPermissions.requested()) }
    val scan: () -> Unit = {
        permission = MediaPermissions.read(context)
        if (!permission.hasAny) requestAccess() else vm.scan()
    }
    BackHandler(navigation.canGoBack) { navigate(navigation.back()) }
    CleanovaTheme("Light") {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!navigation.settingsOpen) FoundationNavigation(navigation.tab) { navigate(navigation.select(it)) }
            }
        ) { padding ->
            AnimatedContent(
                targetState = navigation.current,
                modifier = Modifier.padding(padding),
                transitionSpec = { fadeIn(tween(CleanovaMotion.navigationMs)) togetherWith fadeOut(tween(CleanovaMotion.navigationMs)) },
                label = "navigation"
            ) { page ->
                if (page == Destination.HOME) {
                    HomeScreen(state, permission,
                        onSettings = { navigate(navigation.select(Destination.SETTINGS)) },
                        onRefresh = vm::refresh, onScan = scan, onCancel = vm::cancelScan,
                        onChangeAccess = requestAccess)
                } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = CleanovaSpacing.screen, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(CleanovaSpacing.large)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (page == Destination.SETTINGS) IconButton(onClick = { navigate(navigation.back()) }) { Icon(Icons.Outlined.ArrowBack, "Back") }
                        Text(when(page) {
                            Destination.HOME -> "Cleanova"
                            Destination.MEDIA -> "Media"
                            Destination.STORAGE -> "Storage"
                            Destination.SETTINGS -> "Settings"
                        }, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    }
                    when (page) {
                        Destination.HOME -> Unit
                        Destination.MEDIA -> InventoryPanel("Your media", state, permission, scan, vm::cancelScan, requestAccess)
                        Destination.STORAGE -> {
                            CapacityCard(state, vm::refresh)
                            StorageFoundation(vm::refresh)
                        }
                        Destination.SETTINGS -> SettingsFoundation()
                    }
                }
            }
        }
    }
}

@Composable private fun FoundationNavigation(selectedTab: Destination, onSelect: (Destination) -> Unit) {
    Surface(
        modifier = Modifier.navigationBarsPadding().padding(horizontal = CleanovaSpacing.screen, vertical = 10.dp),
        shape = RoundedCornerShape(CleanovaRadius.navigation), shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(Modifier.fillMaxWidth().padding(5.dp)) {
            val tabs = listOf(
                Triple(Destination.HOME, "Home", Icons.Outlined.Home),
                Triple(Destination.MEDIA, "Media", Icons.Outlined.PhotoLibrary),
                Triple(Destination.STORAGE, "Storage", Icons.Outlined.Storage)
            )
            tabs.forEach { (destination, label, icon) ->
                val active = selectedTab == destination
                Surface(onClick = { onSelect(destination) }, modifier = Modifier.weight(1f).semantics { selected = active },
                    shape = RoundedCornerShape(CleanovaRadius.tab),
                    color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
                    Column(Modifier.heightIn(min = CleanovaSpacing.touchTarget).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(icon, null, tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(label, fontSize = 12.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}
