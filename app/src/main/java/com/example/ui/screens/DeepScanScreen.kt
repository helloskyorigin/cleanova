package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CleanovaUiState
import com.example.data.model.FolderFileCategory
import com.example.data.model.ScannedFolderFile
import com.example.data.repository.StorageScannerRepository.Companion.formatBytes
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.theme.CleanovaAmber
import com.example.ui.theme.CleanovaBlue
import com.example.ui.theme.CleanovaCoral

@Composable
fun DeepScanScreen(
    uiState: CleanovaUiState,
    onFolderPicked: (Uri) -> Unit,
    onFilterSelected: (FolderFileCategory) -> Unit,
    onToggleFileSelection: (String) -> Unit,
    onSelectAllVisible: (Boolean) -> Unit,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showConfirmDelete by remember { mutableStateOf(false) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            if (uri != null) {
                onFolderPicked(uri)
            }
        }
    )

    val filteredFiles = if (uiState.selectedFolderFilter == FolderFileCategory.ALL) {
        uiState.scannedFiles
    } else {
        uiState.scannedFiles.filter { it.category == uiState.selectedFolderFilter }
    }
    val selectedCount = uiState.scannedFiles.count { it.isSelected }

    if (showConfirmDelete && selectedCount > 0) {
        ConfirmDeleteDialog(
            itemCount = selectedCount,
            reclaimLabel = formatBytes(uiState.selectedFolderReclaimBytes),
            onConfirm = {
                showConfirmDelete = false
                onDeleteSelected()
            },
            onDismiss = { showConfirmDelete = false }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Deep Folder Scan",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Inspect Downloads, Documents, or any folder via Android's scoped Storage Access Framework.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { folderPickerLauncher.launch(null) },
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .height(48.dp)
                        .testTag("pick_folder_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.scannedFolderName != null) {
                            "Rescan / Switch Folder"
                        } else {
                            stringResource(R.string.pick_folder_cta)
                        }
                    )
                }

                if (selectedCount > 0) {
                    Button(
                        onClick = { showConfirmDelete = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        shape = CircleShape,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .height(48.dp)
                            .testTag("delete_selected_files_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete selected files",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(formatBytes(uiState.selectedFolderReclaimBytes))
                    }
                }
            }
        }

        if (uiState.isScanningFolder) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        if (uiState.scannedFiles.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${uiState.scannedFolderName ?: "Folder"} • ${uiState.scannedFiles.size} files",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = { onSelectAllVisible(true) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("select_all_files_button")
                        ) {
                            Text("Select All")
                        }
                        TextButton(
                            onClick = { onSelectAllVisible(false) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("clear_all_files_button")
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FolderFileCategory.entries) { category ->
                        FilterChip(
                            selected = uiState.selectedFolderFilter == category,
                            onClick = { onFilterSelected(category) },
                            label = { Text(category.label) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("folder_filter_${category.name.lowercase()}")
                        )
                    }
                }
            }
        }

        if (filteredFiles.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(CleanovaAmber.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = "Select folder to scan",
                                tint = CleanovaAmber,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = if (uiState.scannedFiles.isEmpty()) {
                                "Choose a Folder to Deep Scan"
                            } else {
                                "No Files in This Category"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Pick Downloads or any subfolder to uncover large files (>10 MB), stale APKs/ZIPs, and exact-size duplicates.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredFiles, key = { it.uriString }) { file ->
                ScannedFileRow(
                    file = file,
                    onToggle = { onToggleFileSelection(file.uriString) }
                )
            }
        }
    }
}

@Composable
private fun ScannedFileRow(
    file: ScannedFolderFile,
    onToggle: () -> Unit
) {
    val accent = when (file.category) {
        FolderFileCategory.DUPLICATE_FILE -> CleanovaCoral
        FolderFileCategory.APK_OR_ARCHIVE -> CleanovaAmber
        FolderFileCategory.LARGE_FILE -> CleanovaBlue
        FolderFileCategory.GENERAL, FolderFileCategory.ALL -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val icon = when (file.category) {
        FolderFileCategory.APK_OR_ARCHIVE -> Icons.Rounded.Inventory2
        else -> Icons.Rounded.Description
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onToggle)
            .testTag("folder_file_card"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (file.isSelected) 1.5.dp else 1.dp,
            color = if (file.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = file.category.label,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = file.hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = if (file.isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = if (file.isSelected) "Selected" else "Unselected",
                tint = if (file.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
