package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CleanovaUiState
import com.example.data.repository.StorageScannerRepository.Companion.formatBytes
import com.example.ui.components.AiBadgePill
import com.example.ui.components.CleanovaStatChip
import com.example.ui.components.CleanovaStorageRing
import com.example.ui.components.FirebaseNoticeBanner
import com.example.ui.components.QuickActionModuleCard
import com.example.ui.theme.CleanovaAmber
import com.example.ui.theme.CleanovaBlue
import com.example.ui.theme.CleanovaCoral
import com.example.ui.theme.CleanovaMint

@Composable
fun OverviewScreen(
    uiState: CleanovaUiState,
    onRunSmartScan: () -> Unit,
    onSweepCache: () -> Unit,
    onNavigateToMedia: () -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToAdvisor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val stats = uiState.storageStats

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Editorial Cleanova Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${formatBytes(stats.freeBytes)} free",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Glassmorphic Hero Storage Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                tonalElevation = 2.dp
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_cleanova_hero),
                        contentDescription = stringResource(R.string.hero_banner_desc),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .alpha(0.28f)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CleanovaStorageRing(
                            usedFraction = stats.usedFraction,
                            healthScore = stats.healthScore,
                            usedLabel = formatBytes(stats.usedBytes),
                            totalLabel = formatBytes(stats.totalBytes),
                            isScanning = uiState.isScanning,
                            scanProgress = uiState.scanProgress
                        )

                        AnimatedVisibility(visible = uiState.isScanning) {
                            LinearProgressIndicator(
                                progress = { uiState.scanProgress },
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .height(6.dp)
                                    .clip(CircleShape)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CleanovaStatChip(
                                label = "App Cache",
                                value = formatBytes(stats.appCacheBytes),
                                accent = CleanovaBlue,
                                modifier = Modifier.weight(1f)
                            )
                            CleanovaStatChip(
                                label = "Media Flagged",
                                value = formatBytes(uiState.selectedMediaReclaimBytes),
                                accent = CleanovaAmber,
                                modifier = Modifier.weight(1f)
                            )
                            CleanovaStatChip(
                                label = "Session Freed",
                                value = formatBytes(uiState.lastFreedBytes),
                                accent = CleanovaMint,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onRunSmartScan,
                                enabled = !uiState.isScanning,
                                shape = CircleShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .height(50.dp)
                                    .testTag("smart_scan_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CleaningServices,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (uiState.isScanning) {
                                        stringResource(R.string.scanning_label)
                                    } else {
                                        stringResource(R.string.smart_scan_cta)
                                    }
                                )
                            }

                            OutlinedButton(
                                onClick = onSweepCache,
                                shape = CircleShape,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .height(50.dp)
                                    .testTag("sweep_cache_button")
                            ) {
                                Text("Sweep Cache")
                            }
                        }
                    }
                }
            }
        }

        // Quick Clean Modules 2x2 Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Smart Cleanup Modules",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionModuleCard(
                        title = "Media Audit",
                        subtitle = "Spot duplicates & heavy videos via Photo Picker",
                        metricLabel = "${uiState.auditedMedia.size} items",
                        icon = Icons.Rounded.PhotoLibrary,
                        accentColor = CleanovaBlue,
                        tag = "module_media_audit",
                        onClick = onNavigateToMedia,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionModuleCard(
                        title = "Deep Scan",
                        subtitle = "Inspect Downloads, APKs & large files",
                        metricLabel = if (uiState.scannedFiles.isNotEmpty()) {
                            "${uiState.scannedFiles.size} files"
                        } else {
                            "SAF Safe"
                        },
                        icon = Icons.Rounded.FolderOpen,
                        accentColor = CleanovaAmber,
                        tag = "module_deep_scan",
                        onClick = onNavigateToFolders,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionModuleCard(
                        title = "App Cache",
                        subtitle = "Clear temporary Cleanova workspace cache",
                        metricLabel = formatBytes(stats.appCacheBytes),
                        icon = Icons.Rounded.CleaningServices,
                        accentColor = CleanovaMint,
                        tag = "module_app_cache",
                        onClick = onSweepCache,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionModuleCard(
                        title = "System Storage",
                        subtitle = "Manage all installed app caches in Settings",
                        metricLabel = "Android OS",
                        icon = Icons.Rounded.Settings,
                        accentColor = CleanovaCoral,
                        tag = "module_system_storage",
                        onClick = {
                            val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            runCatching { context.startActivity(intent) }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // AI Declutter Advisor Preview Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("overview_ai_card"),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "AI Advisor",
                                tint = CleanovaBlue
                            )
                            Text(
                                text = "Declutter Advisor",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        AiBadgePill(label = "Sample AI")
                    }

                    FirebaseNoticeBanner()

                    Text(
                        text = uiState.aiPlan?.summary
                            ?: "Run a Smart Scan to generate personalized storage recovery priorities.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onNavigateToAdvisor,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("open_advisor_button")
                        ) {
                            Text("Open Advisor")
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
