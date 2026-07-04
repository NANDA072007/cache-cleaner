package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.StorageItem
import com.example.data.repository.DeviceScanner
import com.example.viewmodel.MainViewModel
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalyzerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storageItems by viewModel.storageItems.collectAsStateWithLifecycle()
    val storageUsage by viewModel.storageUsage.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Disk Breakdown", "Large Files", "Duplicate Finder")

    // Filtered lists
    val largeFiles = remember(storageItems) { 
        storageItems.filter { it.category == "LARGE_FILES" }.sortedByDescending { it.size } 
    }
    
    val duplicates = remember(storageItems) { 
        storageItems.filter { it.category == "DUPLICATES" }.sortedByDescending { it.size } 
    }

    val downloads = remember(storageItems) { storageItems.filter { it.category == "DOWNLOADS" } }
    val apks = remember(storageItems) { storageItems.filter { it.category == "APKS" } }
    val appCaches = remember(storageItems) { storageItems.filter { it.category == "CACHE" } }
    val logsTemp = remember(storageItems) { storageItems.filter { it.category == "LOGS" || it.category == "TEMP" } }

    val hasScannedData = storageItems.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main Tab Row
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("analyzer_tab_$index")
                )
            }
        }

        if (!hasScannedData) {
            // Unscanned Empty State
            Column(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxWidth()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PieChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(52.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "Deep Disk Breakdown", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Unlock details about hidden duplicates, huge system files, installer leftovers, and downloads using the active storage scan tool.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.runFullScan() },
                    modifier = Modifier.testTag("analyzer_first_scan_button")
                ) {
                    Text("START ANALYSIS SCAN")
                }
            }
        } else {
            // Actionable tabs
            Box(modifier = Modifier.weight(1.0f)) {
                when (selectedTabIndex) {
                    0 -> DiskBreakdownTab(
                        viewModel = viewModel,
                        storageUsage = storageUsage,
                        downloadsSize = downloads.sumOf { it.size },
                        apksSize = apks.sumOf { it.size },
                        cacheSize = appCaches.sumOf { it.size } + logsTemp.sumOf { it.size },
                        largeSize = largeFiles.sumOf { it.size },
                        duplicateSize = duplicates.sumOf { it.size }
                    )
                    1 -> LargeFilesTab(
                        viewModel = viewModel,
                        largeFiles = largeFiles,
                        onCleanTrigger = { files ->
                            viewModel.cleanJunk(files) { bytes ->
                                Toast.makeText(context, "Cleared ${viewModel.formatSize(bytes)} of large files!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    2 -> DuplicateFinderTab(
                        viewModel = viewModel,
                        duplicates = duplicates,
                        onCleanTrigger = { files ->
                            viewModel.cleanJunk(files) { bytes ->
                                Toast.makeText(context, "Purged ${viewModel.formatSize(bytes)} duplicates!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

// --- TAB 1: DISK BREAKDOWN COMPOSABLE ---
@Composable
fun DiskBreakdownTab(
    viewModel: MainViewModel,
    storageUsage: DeviceScanner.StorageUsage,
    downloadsSize: Long,
    apksSize: Long,
    cacheSize: Long,
    largeSize: Long,
    duplicateSize: Long
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Beautiful segmented progress bar representing real storage portions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "DISK CAPACITY RATIO",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    val colorDownloads = MaterialTheme.colorScheme.primary
                    val colorApks = MaterialTheme.colorScheme.secondary
                    val colorCache = MaterialTheme.colorScheme.tertiary
                    val colorLarge = MaterialTheme.colorScheme.error
                    val colorDuplicates = MaterialTheme.colorScheme.inversePrimary
                    val colorFree = MaterialTheme.colorScheme.surfaceVariant

                    // Segmented Canvas drawing
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        val total = storageUsage.totalBytes.toFloat()
                        if (total > 0) {
                            val wDownloads = (downloadsSize / total) * size.width
                            val wApks = (apksSize / total) * size.width
                            val wCache = (cacheSize / total) * size.width
                            val wLarge = (largeSize / total) * size.width
                            val wDuplicates = (duplicateSize / total) * size.width
                            val wUsedSystem = ((storageUsage.usedBytes - downloadsSize - apksSize - cacheSize - largeSize - duplicateSize).coerceAtLeast(0L) / total) * size.width
                            
                            var currentOffset = 0f

                            // Downloads segment
                            drawRect(color = colorDownloads, topLeft = Offset(currentOffset, 0f), size = Size(wDownloads, size.height))
                            currentOffset += wDownloads

                            // Apks segment
                            drawRect(color = colorApks, topLeft = Offset(currentOffset, 0f), size = Size(wApks, size.height))
                            currentOffset += wApks

                            // Cache segment
                            drawRect(color = colorCache, topLeft = Offset(currentOffset, 0f), size = Size(wCache, size.height))
                            currentOffset += wCache

                            // Duplicates segment
                            drawRect(color = colorDuplicates, topLeft = Offset(currentOffset, 0f), size = Size(wDuplicates, size.height))
                            currentOffset += wDuplicates

                            // Large segment
                            drawRect(color = colorLarge, topLeft = Offset(currentOffset, 0f), size = Size(wLarge, size.height))
                            currentOffset += wLarge

                            // Other Used segment
                            drawRect(color = Color.Gray.copy(alpha = 0.5f), topLeft = Offset(currentOffset, 0f), size = Size(wUsedSystem, size.height))
                            currentOffset += wUsedSystem

                            // Free space segment
                            val remainingWidth = (size.width - currentOffset).coerceAtLeast(0f)
                            drawRect(color = colorFree, topLeft = Offset(currentOffset, 0f), size = Size(remainingWidth, size.height))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Legend values list
                    LegendRow(label = "Downloads Cache", bytes = downloadsSize, color = colorDownloads, viewModel = viewModel)
                    LegendRow(label = "APK Files", bytes = apksSize, color = colorApks, viewModel = viewModel)
                    LegendRow(label = "Sandbox Cache & Logs", bytes = cacheSize, color = colorCache, viewModel = viewModel)
                    LegendRow(label = "Duplicate Files", bytes = duplicateSize, color = colorDuplicates, viewModel = viewModel)
                    LegendRow(label = "Huge Files (>10MB)", bytes = largeSize, color = colorLarge, viewModel = viewModel)
                    LegendRow(label = "Free Disk Space", bytes = storageUsage.freeBytes, color = colorFree, viewModel = viewModel)
                }
            }
        }

        // Additional information categories cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CategoryInfoMiniCard(
                    title = "WhatsApp Media",
                    sizeText = "Local Cache Mode",
                    icon = Icons.Filled.PhotoLibrary,
                    color = Color(0xFF25D366),
                    modifier = Modifier.weight(1.0f)
                )
                CategoryInfoMiniCard(
                    title = "Telegram Audio",
                    sizeText = "Local Cache Mode",
                    icon = Icons.Filled.LibraryMusic,
                    color = Color(0xFF0088CC),
                    modifier = Modifier.weight(1.0f)
                )
            }
        }
    }
}

@Composable
fun LegendRow(label: String, bytes: Long, color: Color, viewModel: MainViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, RoundedCornerShape(3.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = viewModel.formatSize(bytes),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun CategoryInfoMiniCard(title: String, sizeText: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = sizeText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// --- TAB 2: LARGE FILES COMPOSABLE ---
@Composable
fun LargeFilesTab(
    viewModel: MainViewModel,
    largeFiles: List<StorageItem>,
    onCleanTrigger: (List<StorageItem>) -> Unit
) {
    val selectedFiles = remember { mutableStateListOf<StorageItem>() }
    val totalSelectedSize = remember(selectedFiles.toList()) { selectedFiles.sumOf { it.size } }

    if (largeFiles.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No large files (over 10MB) detected on your storage", style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Large files consume massive amounts of disk space. Check the files you don't need and click delete.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1.0f),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(largeFiles, key = { it.path }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (selectedFiles.contains(item)) selectedFiles.remove(item) else selectedFiles.add(item)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedFiles.contains(item)) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedFiles.contains(item),
                                onCheckedChange = { check ->
                                    if (check == true) selectedFiles.add(item) else selectedFiles.remove(item)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1.0f)) {
                                Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(item.path, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(viewModel.formatSize(item.size), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black)
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }

            Box(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        onCleanTrigger(selectedFiles.toList())
                        selectedFiles.clear()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = selectedFiles.isNotEmpty()
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("DELETE SELECTED (${viewModel.formatSize(totalSelectedSize)})", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- TAB 3: DUPLICATE FINDER COMPOSABLE ---
@Composable
fun DuplicateFinderTab(
    viewModel: MainViewModel,
    duplicates: List<StorageItem>,
    onCleanTrigger: (List<StorageItem>) -> Unit
) {
    val selectedFiles = remember { mutableStateListOf<StorageItem>() }
    val totalSelectedSize = remember(selectedFiles.toList()) { selectedFiles.sumOf { it.size } }

    if (duplicates.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Zero Duplicates Found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("No matching file sizes or copy duplicates occupy space on this storage.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Exact duplicate files located in downloads and custom folders. Keep the original and purge duplicate clones.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1.0f),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(duplicates, key = { it.path }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (selectedFiles.contains(item)) selectedFiles.remove(item) else selectedFiles.add(item)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedFiles.contains(item)) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedFiles.contains(item),
                                onCheckedChange = { check ->
                                    if (check == true) selectedFiles.add(item) else selectedFiles.remove(item)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1.0f)) {
                                Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text("CLONE PATH: " + item.path, style = MaterialTheme.typography.labelSmall, color = Color(0xFFE57373), maxLines = 1)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(viewModel.formatSize(item.size), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black)
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }

            Box(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        onCleanTrigger(selectedFiles.toList())
                        selectedFiles.clear()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = selectedFiles.isNotEmpty()
                ) {
                    Icon(Icons.Filled.CleaningServices, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PURGE CLONES (${viewModel.formatSize(totalSelectedSize)})", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
