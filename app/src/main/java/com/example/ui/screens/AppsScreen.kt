package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppInfoItem
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("SIZE") } // "SIZE", "DATE", "PERMISSIONS", "BATTERY"
    val sortOptions = listOf("SIZE" to "Size", "DATE" to "Date", "PERMISSIONS" to "Permissions", "BATTERY" to "Battery Drain")

    var expandedPackage by remember { mutableStateOf<String?>(null) }

    // Prepare sorted list
    val processedApps = remember(installedApps, searchQuery, sortBy) {
        installedApps
            .filter { it.label.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true) }
            .sortedWith { a, b ->
                when (sortBy) {
                    "DATE" -> b.installDate.compareTo(a.installDate)
                    "PERMISSIONS" -> b.permissions.size.compareTo(a.permissions.size)
                    "BATTERY" -> b.estimatedBatteryDrainPercent.compareTo(a.estimatedBatteryDrainPercent)
                    else -> b.apkSize.compareTo(a.apkSize) // Default size
                }
            }
    }

    LaunchedEffect(Unit) {
        if (installedApps.isEmpty()) {
            viewModel.runFullScan()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search & Filter Panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Search TextField
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_search_field"),
                    placeholder = { Text("Search installed applications...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Sort Row
                Text(
                    text = "SORT APPLICATIONS BY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sortOptions.forEach { (key, name) ->
                        FilterChip(
                            selected = sortBy == key,
                            onClick = { sortBy = key },
                            label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("sort_chip_$key")
                        )
                    }
                }
            }
        }

        // List representation
        if (isScanning && installedApps.isEmpty()) {
            Box(modifier = Modifier.weight(1.0f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Scanning packages database...", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else if (processedApps.isEmpty()) {
            Box(modifier = Modifier.weight(1.0f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No applications match your search filters.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1.0f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(processedApps, key = { it.packageName }) { app ->
                    val isExpanded = expandedPackage == app.packageName
                    AppInfoItemCard(
                        app = app,
                        isExpanded = isExpanded,
                        viewModel = viewModel,
                        onExpandClick = {
                            expandedPackage = if (isExpanded) null else app.packageName
                        },
                        onOpenSettings = {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${app.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open app details screen.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onUninstall = {
                            try {
                                val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                                    data = Uri.parse("package:${app.packageName}")
                                    putExtra(Intent.EXTRA_RETURN_RESULT, true)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Uninstallation service failed.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onShare = {
                            try {
                                val sizeStr = viewModel.formatSize(app.apkSize)
                                val msg = "SmartBoost AI Package Info:\nName: ${app.label}\nPackage: ${app.packageName}\nAPK Size: $sizeStr\nDeclared Permissions: ${app.permissions.size}"
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, msg)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share App Info"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Sharing failed.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppInfoItemCard(
    app: AppInfoItem,
    isExpanded: Boolean,
    viewModel: MainViewModel,
    onExpandClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onUninstall: () -> Unit,
    onShare: () -> Unit
) {
    val dateStr = remember(app.installDate) {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        formatter.format(Date(app.installDate))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_info_card_${app.packageName}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandClick() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Application Placeholder Icon utilizing a beautiful letter badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.label.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1.0f)) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = viewModel.formatSize(app.apkSize),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black
                    )
                    if (app.isSystemApp) {
                        Text(
                            text = "SYSTEM",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Details section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            DetailTextRow(label = "First Installed", value = dateStr)
                            DetailTextRow(label = "Declared Permissions", value = "${app.permissions.size} requested")
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            DetailTextRow(
                                label = "Battery Impact Estimate",
                                value = String.format("%.1f%% / day", app.estimatedBatteryDrainPercent),
                                valueColor = if (app.estimatedBatteryDrainPercent > 8.0f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            DetailTextRow(
                                label = "Background Class",
                                value = when (app.launchFrequencyIndex) {
                                    3 -> "Active Backgrounder"
                                    2 -> "Periodic Walker"
                                    else -> "Idle Sleeper"
                                }
                            )
                        }
                    }

                    // Dangerous permissions list
                    val dangerousPermissions = app.permissions.filter {
                        it.contains("LOCATION", true) || it.contains("CAMERA", true) ||
                        it.contains("CONTACTS", true) || it.contains("STORAGE", true) ||
                        it.contains("RECORD_AUDIO", true)
                    }.map { it.substringAfterLast(".") }

                    if (dangerousPermissions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "HIGH-SENSITIVITY PERMISSIONS:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dangerousPermissions.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ACTION BUTTONS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Settings Button
                        FilledTonalButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .weight(1.0f)
                                .height(40.dp)
                                .testTag("btn_app_settings_${app.packageName}"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SETTINGS", style = MaterialTheme.typography.labelSmall)
                        }

                        // Share Button
                        OutlinedButton(
                            onClick = onShare,
                            modifier = Modifier
                                .weight(1.0f)
                                .height(40.dp)
                                .testTag("btn_app_share_${app.packageName}"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SHARE", style = MaterialTheme.typography.labelSmall)
                        }

                        // Uninstall Button (If not system app)
                        if (!app.isSystemApp) {
                            Button(
                                onClick = onUninstall,
                                modifier = Modifier
                                    .weight(1.0f)
                                    .height(40.dp)
                                    .testTag("btn_app_uninstall_${app.packageName}"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("UNINSTALL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailTextRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}
