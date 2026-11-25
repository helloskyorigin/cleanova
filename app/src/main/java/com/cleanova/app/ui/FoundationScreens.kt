package com.cleanova.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cleanova.app.HomeState
import com.cleanova.app.domain.Grant
import com.cleanova.app.domain.MediaAccess

// Read-only foundation surfaces. Replace individually as each feature is approved.
@Composable internal fun InventoryPanel(
    title: String, state: HomeState, permission: MediaAccess,
    scan: () -> Unit, cancel: () -> Unit, changeAccess: () -> Unit
) {
    Panel {
        Icon(Icons.Outlined.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text("Review the media you allow Cleanova to access. Nothing is deleted.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Media access: " + permission.label, style = MaterialTheme.typography.labelMedium)
        if (state.scanning) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            OutlinedButton(onClick = cancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel scan") }
        } else PrimaryAction("Scan media", scan)
        if (permission.hasAny && !permission.isFull && !state.scanning) {
            TextButton(onClick = changeAccess) { Text("Change media access") }
        }
        state.scanError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
    state.inventory?.takeIf { it.access == permission }?.let { result ->
        Panel {
            Text("Accessible media", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Detail(if (result.access.images == Grant.NONE) "Photos" else "Photos (" + result.photos + ")", if (result.access.images == Grant.NONE) "No access" else gb(result.photoBytes))
            HorizontalDivider()
            Detail(if (result.access.videos == Grant.NONE) "Videos" else "Videos (" + result.videos + ")", if (result.access.videos == Grant.NONE) "No access" else gb(result.videoBytes))
            Text("Media size is not reclaimable storage. Duplicate detection is not included in this build.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable internal fun StorageFoundation(onRefresh: () -> Unit) = Panel {
    Text("Storage information", style = MaterialTheme.typography.titleLarge)
    Text("Capacity refers to the Android internal data volume. System and app-private files cannot be inspected by this build.")
    PrimaryAction("Refresh storage", onRefresh)
}

@Composable internal fun SettingsFoundation(appearance: String, onAppearance: (String) -> Unit) {
    Panel {
        Text("Appearance", style = MaterialTheme.typography.titleLarge)
        listOf("System", "Light", "Dark").forEach { option ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = appearance == option, onClick = { onAppearance(option) })
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
