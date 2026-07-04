package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.StorageItem
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CleanerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storageItems by viewModel.storageItems.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val scanProgressText by viewModel.scanProgressText.collectAsStateWithLifecycle()

    // Map files into Logical Junk Categories
    val junkLogsAndTemp = remember(storageItems) { storageItems.filter { it.category == "LOGS" || it.category == "TEMP" } }
    val junkApks = remember(storageItems) { storageItems.filter { it.category == "APKS" } }
    val junkEmptyFolders = remember(storageItems) { storageItems.filter { it.category == "EMPTY_FOLDER" } }
    val junkDownloads = remember(storageItems) { storageItems.filter { it.category == "DOWNLOADS" } }
    val junkAppCaches = remember(storageItems) { storageItems.filter { it.category == "CACHE" || it.category == "WHATSAPP_MEDIA" || it.category == "TELEGRAM_MEDIA" } }

    val hasScannedData = storageItems.isNotEmpty()

    // Track chosen files
    val selectedItems = remember { mutableStateListOf<StorageItem>() }

    // Synchronize chosen files when files scan completes
    LaunchedEffect(storageItems) {
        selectedItems.clear()
        // Default select Logs, Temps, Empty Folders, and Cache files
        selectedItems.addAll(junkLogsAndTemp)
        selectedItems.addAll(junkEmptyFolders)
        selectedItems.addAll(junkAppCaches)
    }

    val selectedBytesSum = remember(selectedItems.toList()) { selectedItems.sumOf { it.size } }

    // Folder Expansion States
    var logsExpanded by remember { mutableStateOf(false) }
    var apksExpanded by remember { mutableStateOf(false) }
    var emptyExpanded by remember { mutableStateOf(false) }
    var downloadsExpanded by remember { mutableStateOf(false) }
    var appCacheExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!hasScannedData) {
            // EMPTY STATE (FIRST-LAUNCH ACTIONABLE SCREEN)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CleaningServices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "System is Ready for Scan",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "A full diagnostic scans the internal disk for temp files, duplicates, residual caches, empty subfolders, and installation files. No user or protected files are touched.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (isScanning) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(scanProgressText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                } else {
                    Button(
                        onClick = { viewModel.runFullScan() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("cleaner_scan_trigger"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SCAN LOCAL STORAGE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // SCANNED RESULTS LIST
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Panel
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SELECTED FOR DELETION",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = viewModel.formatSize(selectedBytesSum),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "SmartBoost AI cleans safe-to-delete files in app sandbox directories, downloads, or custom logs. Protected system app caches are strictly untouched.",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            lineHeight = 14.sp
                        )
                    }
                }

                // File categories list
                LazyColumn(
                    modifier = Modifier
                        .weight(1.0f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "SELECT JUNK CATEGORIES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                        )
                    }

                    // 1. Logs and Temp Files
                    item {
                        JunkFolderCard(
                            title = "Temp & Log Cache Files",
                            subtitle = "Temporary files created by diagnostic routines",
                            sizeBytes = junkLogsAndTemp.sumOf { it.size },
                            fileCount = junkLogsAndTemp.size,
                            icon = Icons.Filled.DeleteSweep,
                            isExpanded = logsExpanded,
                            onExpandClick = { logsExpanded = !logsExpanded },
                            isSelected = selectedItems.containsAll(junkLogsAndTemp) && junkLogsAndTemp.isNotEmpty(),
                            onSelectedChange = { select ->
                                if (select) {
                                    junkLogsAndTemp.forEach { if (!selectedItems.contains(it)) selectedItems.add(it) }
                                } else {
                                    selectedItems.removeAll(junkLogsAndTemp)
                                }
                            }
                        ) {
                            JunkFilesList(junkLogsAndTemp, selectedItems)
                        }
                    }

                    // 2. Empty Folders
                    item {
                        JunkFolderCard(
                            title = "Residual Empty Directories",
                            subtitle = "Empty folders left behind by uninstalled packages",
                            sizeBytes = 0L,
                            fileCount = junkEmptyFolders.size,
                            icon = Icons.Filled.FolderOff,
                            isExpanded = emptyExpanded,
                            onExpandClick = { emptyExpanded = !emptyExpanded },
                            isSelected = selectedItems.containsAll(junkEmptyFolders) && junkEmptyFolders.isNotEmpty(),
                            onSelectedChange = { select ->
                                if (select) {
                                    junkEmptyFolders.forEach { if (!selectedItems.contains(it)) selectedItems.add(it) }
                                } else {
                                    selectedItems.removeAll(junkEmptyFolders)
                                }
                            }
                        ) {
                            JunkFilesList(junkEmptyFolders, selectedItems)
                        }
                    }

                    // 3. App caches
                    item {
                        JunkFolderCard(
                            title = "App Sandbox Cache Files",
                            subtitle = "Caches from standard sandboxed and media paths",
                            sizeBytes = junkAppCaches.sumOf { it.size },
                            fileCount = junkAppCaches.size,
                            icon = Icons.Filled.Memory,
                            isExpanded = appCacheExpanded,
                            onExpandClick = { appCacheExpanded = !appCacheExpanded },
                            isSelected = selectedItems.containsAll(junkAppCaches) && junkAppCaches.isNotEmpty(),
                            onSelectedChange = { select ->
                                if (select) {
                                    junkAppCaches.forEach { if (!selectedItems.contains(it)) selectedItems.add(it) }
                                } else {
                                    selectedItems.removeAll(junkAppCaches)
                                }
                            }
                        ) {
                            JunkFilesList(junkAppCaches, selectedItems)
                        }
                    }

                    // 4. Downloads Directory
                    item {
                        JunkFolderCard(
                            title = "Obsolete Downloads Files",
                            subtitle = "Cached documents or resources in downloads",
                            sizeBytes = junkDownloads.sumOf { it.size },
                            fileCount = junkDownloads.size,
                            icon = Icons.Filled.Download,
                            isExpanded = downloadsExpanded,
                            onExpandClick = { downloadsExpanded = !downloadsExpanded },
                            isSelected = selectedItems.containsAll(junkDownloads) && junkDownloads.isNotEmpty(),
                            onSelectedChange = { select ->
                                if (select) {
                                    junkDownloads.forEach { if (!selectedItems.contains(it)) selectedItems.add(it) }
                                } else {
                                    selectedItems.removeAll(junkDownloads)
                                }
                            }
                        ) {
                            JunkFilesList(junkDownloads, selectedItems)
                        }
                    }

                    // 5. Unused APK Files
                    item {
                        JunkFolderCard(
                            title = "Scrapped APK Packages",
                            subtitle = "Obsolete setup binaries consuming drive space",
                            sizeBytes = junkApks.sumOf { it.size },
                            fileCount = junkApks.size,
                            icon = Icons.Filled.Android,
                            isExpanded = apksExpanded,
                            onExpandClick = { apksExpanded = !apksExpanded },
                            isSelected = selectedItems.containsAll(junkApks) && junkApks.isNotEmpty(),
                            onSelectedChange = { select ->
                                if (select) {
                                    junkApks.forEach { if (!selectedItems.contains(it)) selectedItems.add(it) }
                                } else {
                                    selectedItems.removeAll(junkApks)
                                }
                            }
                        ) {
                            JunkFilesList(junkApks, selectedItems)
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }

            // Clean action FAB floating at the bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Button(
                    onClick = {
                        if (selectedItems.isEmpty()) {
                            Toast.makeText(context, "Please select at least one item to clean", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.cleanJunk(selectedItems.toList()) { bytesCleaned ->
                                Toast.makeText(context, "Cleaned ${viewModel.formatSize(bytesCleaned)}!", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("clean_selected_fab"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedItems.isEmpty()) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                        contentColor = if (selectedItems.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    enabled = selectedItems.isNotEmpty()
                ) {
                    Icon(Icons.Filled.CleaningServices, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SAFE PURGE CLEAN (${viewModel.formatSize(selectedBytesSum)})",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // --- SCANNING / DELETING FULLSCREEN OVERLAY (PREVENT ACCIDENTAL BACK CLICKS) ---
        if (isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable(enabled = false) {}, // eat clicks
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(72.dp),
                        strokeWidth = 6.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "PROCESSING DISK DIAGNOSTIC...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = scanProgressText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun JunkFolderCard(
    title: String,
    subtitle: String,
    sizeBytes: Long,
    fileCount: Int,
    icon: ImageVector,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    isSelected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    expandedContent: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("junk_folder_card_${title.lowercase().replace(" ", "_")}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelectedChange,
                    modifier = Modifier.testTag("folder_checkbox_${title.lowercase().replace(" ", "_")}")
                )

                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1.0f).clickable { onExpandClick() }) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$fileCount files | " + if (sizeBytes > 0) formatSizeLocal(sizeBytes) else "0 B (Clean)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onExpandClick) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = "Expand details"
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                        .padding(bottom = 12.dp)
                ) {
                    expandedContent()
                }
            }
        }
    }
}

@Composable
fun JunkFilesList(
    files: List<StorageItem>,
    selectedItems: MutableList<StorageItem>
) {
    if (files.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No files found in this category. Clean!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))
            files.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selectedItems.contains(item),
                        onCheckedChange = { check ->
                            if (check) {
                                if (!selectedItems.contains(item)) selectedItems.add(item)
                            } else {
                                selectedItems.remove(item)
                            }
                        },
                        modifier = Modifier.scaleCheck()
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1.0f)) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            text = item.path,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (item.size > 0) formatSizeLocal(item.size) else "0 B",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun Modifier.scaleCheck(): Modifier = this.size(36.dp)

private fun formatSizeLocal(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
