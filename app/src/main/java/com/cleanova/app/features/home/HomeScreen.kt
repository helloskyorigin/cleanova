package com.cleanova.app.features.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanova.app.HomeState
import com.cleanova.app.R
import com.cleanova.app.domain.Grant
import com.cleanova.app.domain.MediaAccess
import com.cleanova.app.ui.*
import java.text.NumberFormat

@Composable
internal fun HomeScreen(
    state: HomeState,
    permission: MediaAccess,
    onSettings: () -> Unit,
    onRefresh: () -> Unit,
    onScan: () -> Unit,
    onCancel: () -> Unit,
    onChangeAccess: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = CleanovaSpacing.screen, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(CleanovaSpacing.large)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.app_name), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.home_tagline), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                IconButton(onClick = onSettings) {
                    Icon(Icons.Outlined.Settings, stringResource(R.string.home_settings), tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        HomeStorageCard(state, onRefresh)
        SmartCleanCard(state, permission, onScan = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onScan()
        }, onCancel = onCancel, onChangeAccess = onChangeAccess)
        Text(stringResource(R.string.home_for_you), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        BoxWithConstraints {
            if (maxWidth < 300.dp || androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.5f) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    UpcomingCard(Modifier.fillMaxWidth(), Icons.Outlined.Shield, stringResource(R.string.home_memory_guard), stringResource(R.string.home_memory_guard_desc))
                    UpcomingCard(Modifier.fillMaxWidth(), Icons.Outlined.Bolt, stringResource(R.string.home_space_rescue), stringResource(R.string.home_space_rescue_desc))
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    UpcomingCard(Modifier.weight(1f), Icons.Outlined.Shield, stringResource(R.string.home_memory_guard), stringResource(R.string.home_memory_guard_desc))
                    UpcomingCard(Modifier.weight(1f), Icons.Outlined.Bolt, stringResource(R.string.home_space_rescue), stringResource(R.string.home_space_rescue_desc))
                }
            }
        }
    }
}

@Composable
private fun HomeStorageCard(state: HomeState, onRefresh: () -> Unit) = Panel {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.home_device_storage), Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
        Icon(Icons.Outlined.Storage, null, tint = MaterialTheme.colorScheme.primary)
    }
    val cap = state.capacity
    if (cap == null) {
        Text(stringResource(if (state.capacityError) R.string.home_storage_unavailable else R.string.home_storage_loading))
        if (state.capacityError) TextButton(onClick = onRefresh) { Text(stringResource(R.string.home_retry)) }
    } else {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }
                .format(cap.available.coerceAtLeast(0) / 1_000_000_000.0),
                fontSize = 40.sp, fontWeight = FontWeight.Bold, lineHeight = 46.sp,
                modifier = Modifier.alignByBaseline())
            Text(stringResource(R.string.home_gb_free), color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp, modifier = Modifier.alignByBaseline())
        }
        val progress by animateFloatAsState(cap.fraction, tween(CleanovaMotion.storageProgressMs), label = "homeStorage")
        Box(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape)
            .background(Color(0xFFE8E8ED))
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(cap.fraction, 0f..1f) }) {
            Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.home_used, gb(cap.used)), Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.home_total, gb(cap.total)),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.capacityError) {
            Text(stringResource(R.string.home_stale_storage), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onRefresh) { Text(stringResource(R.string.home_retry)) }
        }
    }
}

@Composable
private fun SmartCleanCard(
    state: HomeState,
    permission: MediaAccess,
    onScan: () -> Unit,
    onCancel: () -> Unit,
    onChangeAccess: () -> Unit
) = Panel {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(27.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.weight(1f))
        Text(stringResource(R.string.home_smart_clean), style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
    Text(stringResource(R.string.home_make_room), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    Text(stringResource(R.string.home_smart_description), color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium)
    if (state.scanning) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.home_cancel_scan))
        }
    } else {
        Button(onClick = onScan, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006CE5), contentColor = Color.White),
            shape = RoundedCornerShape(CleanovaRadius.button)) {
            Icon(Icons.Outlined.CenterFocusStrong, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(if (state.inventory != null) R.string.home_scan_again else R.string.home_smart_scan),
                fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
    if (permission.hasAny && !permission.isFull && !state.scanning) {
        TextButton(onClick = onChangeAccess) { Text(stringResource(R.string.home_change_access)) }
    }
    state.scanError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    state.inventory?.takeIf { it.access == permission }?.let { result ->
        HorizontalDivider()
        Text(stringResource(R.string.home_inventory), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Detail(stringResource(R.string.home_photos), if (result.access.images == Grant.NONE) stringResource(R.string.home_no_access) else gb(result.photoBytes))
        Detail(stringResource(R.string.home_videos), if (result.access.videos == Grant.NONE) stringResource(R.string.home_no_access) else gb(result.videoBytes))
        if (result.incompleteSize) Text(stringResource(R.string.home_incomplete), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.home_inventory_disclaimer), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun UpcomingCard(modifier: Modifier, icon: ImageVector, title: String, description: String) {
    Surface(modifier = modifier, shape = RoundedCornerShape(CleanovaRadius.card), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Outlined.ArrowOutward, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.home_coming_soon), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
