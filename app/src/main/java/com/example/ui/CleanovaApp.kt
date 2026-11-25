package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.ui.screens.AdvisorScreen
import com.example.ui.screens.DeepScanScreen
import com.example.ui.screens.MediaAuditScreen
import com.example.ui.screens.OverviewScreen

enum class CleanovaDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    OVERVIEW(
        route = "overview",
        labelRes = R.string.nav_overview,
        selectedIcon = Icons.Rounded.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_overview"
    ),
    MEDIA(
        route = "media",
        labelRes = R.string.nav_media,
        selectedIcon = Icons.Rounded.PhotoLibrary,
        unselectedIcon = Icons.Outlined.PhotoLibrary,
        testTag = "nav_media"
    ),
    FOLDERS(
        route = "folders",
        labelRes = R.string.nav_folders,
        selectedIcon = Icons.Rounded.FolderOpen,
        unselectedIcon = Icons.Outlined.FolderOpen,
        testTag = "nav_folders"
    ),
    ADVISOR(
        route = "advisor",
        labelRes = R.string.nav_advisor,
        selectedIcon = Icons.Rounded.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
        testTag = "nav_advisor"
    )
}

@Composable
fun CleanovaApp(
    viewModel: CleanovaViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: CleanovaDestination.OVERVIEW.route
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.bannerMessage) {
        val message = uiState.bannerMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.dismissBanner()
        }
    }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        CleanovaDestination.entries.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        CleanovaDestination.entries.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = CleanovaDestination.OVERVIEW.route,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp)
                    ) {
                        composable(CleanovaDestination.OVERVIEW.route) {
                            OverviewScreen(
                                uiState = uiState,
                                onRunSmartScan = viewModel::runSmartScan,
                                onSweepCache = viewModel::sweepAppCache,
                                onNavigateToMedia = { navigateTopLevel(CleanovaDestination.MEDIA.route) },
                                onNavigateToFolders = { navigateTopLevel(CleanovaDestination.FOLDERS.route) },
                                onNavigateToAdvisor = { navigateTopLevel(CleanovaDestination.ADVISOR.route) }
                            )
                        }
                        composable(CleanovaDestination.MEDIA.route) {
                            MediaAuditScreen(
                                uiState = uiState,
                                onMediaPicked = viewModel::auditPickedMedia,
                                onFilterSelected = viewModel::setMediaFilter,
                                onToggleSelection = viewModel::toggleMediaSelection,
                                onClearSelectedFromQueue = viewModel::removeSelectedMediaFromAuditList
                            )
                        }
                        composable(CleanovaDestination.FOLDERS.route) {
                            DeepScanScreen(
                                uiState = uiState,
                                onFolderPicked = viewModel::scanFolder,
                                onFilterSelected = viewModel::setFolderFilter,
                                onToggleFileSelection = viewModel::toggleFolderFileSelection,
                                onSelectAllVisible = viewModel::selectAllVisibleFolderFiles,
                                onDeleteSelected = viewModel::deleteSelectedFolderFiles
                            )
                        }
                        composable(CleanovaDestination.ADVISOR.route) {
                            AdvisorScreen(
                                uiState = uiState,
                                onRefreshPlan = viewModel::generateAiPlan,
                                onAskAdvisor = viewModel::askAdvisor
                            )
                        }
                    }
                }
            }
        }
    }
}
