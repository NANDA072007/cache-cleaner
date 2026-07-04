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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.DeviceScoreLog
import com.example.core.model.OptimizationLog
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val autoScanDaily by viewModel.autoScanDaily.collectAsStateWithLifecycle()
    val autoRemindCleanup by viewModel.autoRemindCleanup.collectAsStateWithLifecycle()
    val appTheme by viewModel.theme.collectAsStateWithLifecycle()
    val appLanguage by viewModel.language.collectAsStateWithLifecycle()
    val animationsEnabled by viewModel.animationsEnabled.collectAsStateWithLifecycle()
    val privacyMode by viewModel.privacyMode.collectAsStateWithLifecycle()

    val optimizationHistory by viewModel.optimizationHistory.collectAsStateWithLifecycle()
    val scoreHistory by viewModel.scoreHistory.collectAsStateWithLifecycle()

    var themeExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // --- SECTION 1: OPTIMIZATION CONFIGURATION ---
        item {
            Text(
                text = "OPTIMIZER CONFIGURATION",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Theme Selector Dropdown
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { themeExpanded = true }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("App Display Theme", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("Toggle light / dark UI parameters", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Box {
                            Text(
                                text = appTheme,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            DropdownMenu(
                                expanded = themeExpanded,
                                onDismissRequest = { themeExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("SYSTEM DEFAULT") },
                                    onClick = {
                                        viewModel.setTheme("SYSTEM")
                                        themeExpanded = false
                                    },
                                    modifier = Modifier.testTag("theme_option_system")
                                )
                                DropdownMenuItem(
                                    text = { Text("LIGHT MODE") },
                                    onClick = {
                                        viewModel.setTheme("LIGHT")
                                        themeExpanded = false
                                    },
                                    modifier = Modifier.testTag("theme_option_light")
                                )
                                DropdownMenuItem(
                                    text = { Text("DARK MODE") },
                                    onClick = {
                                        viewModel.setTheme("DARK")
                                        themeExpanded = false
                                    },
                                    modifier = Modifier.testTag("theme_option_dark")
                                )
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // Language Dropdown
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { languageExpanded = true }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Language Selector", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("Choose language representations", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Box {
                            Text(
                                text = when (appLanguage) {
                                    "es" -> "Spanish"
                                    "fr" -> "French"
                                    "de" -> "German"
                                    else -> "English"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            DropdownMenu(
                                expanded = languageExpanded,
                                onDismissRequest = { languageExpanded = false }
                            ) {
                                DropdownMenuItem(text = { Text("English") }, onClick = { viewModel.setLanguage("en"); languageExpanded = false })
                                DropdownMenuItem(text = { Text("Español") }, onClick = { viewModel.setLanguage("es"); languageExpanded = false })
                                DropdownMenuItem(text = { Text("Français") }, onClick = { viewModel.setLanguage("fr"); languageExpanded = false })
                                DropdownMenuItem(text = { Text("Deutsch") }, onClick = { viewModel.setLanguage("de"); languageExpanded = false })
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // Animations Enabled Toggle
                    SettingsToggleRow(
                        title = "Enable Motion Transitions",
                        subtitle = "Smooth scale and fade visual animations",
                        checked = animationsEnabled,
                        icon = Icons.Filled.Animation,
                        onCheckedChange = { viewModel.setAnimationsEnabled(it) }
                    )

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // Privacy Mode Toggle
                    SettingsToggleRow(
                        title = "Anonymize Data Metrics",
                        subtitle = "Purges package and paths from reporting logs",
                        checked = privacyMode,
                        icon = Icons.Filled.PrivacyTip,
                        onCheckedChange = { viewModel.setPrivacyMode(it) }
                    )

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // Daily Auto Scan
                    SettingsToggleRow(
                        title = "Background Diagnostics Scan",
                        subtitle = "Execute offline disk checks periodically",
                        checked = autoScanDaily,
                        icon = Icons.Filled.Update,
                        onCheckedChange = { viewModel.setAutoScanDaily(it) }
                    )

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // Auto remind cleanup
                    SettingsToggleRow(
                        title = "Storage Overfill Reminders",
                        subtitle = "Alert when storage capacity is over 80%",
                        checked = autoRemindCleanup,
                        icon = Icons.Filled.NotificationsActive,
                        onCheckedChange = { viewModel.setAutoRemindCleanup(it) }
                    )
                }
            }
        }

        // --- SECTION 2: DEVICE DIAGNOSTIC SCORE HISTORIC TREND ---
        item {
            Text(
                text = "DIAGNOSTIC HEALTH PROGRESSION",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "STORAGE & PERFORMANCE HISTORY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    if (scoreHistory.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No history yet. Click 'Run Intelligent Diagnostic' in Dashboard.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Custom Canvas Trend Line Graph
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                                .padding(8.dp)
                        ) {
                            val activeScores = scoreHistory.map { it.overallScore }.reversed()
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                if (activeScores.size > 1) {
                                    val widthInterval = size.width / (activeScores.size - 1)
                                    val path = Path()

                                    activeScores.forEachIndexed { idx, point ->
                                        // Map score 40-100 to canvas height
                                        val normalized = (point - 40f) / 60f
                                        val x = idx * widthInterval
                                        val y = size.height - (normalized * size.height)

                                        if (idx == 0) {
                                            path.moveTo(x, y)
                                        } else {
                                            path.lineTo(x, y)
                                        }
                                        
                                        // Draw a cute dot at each node
                                        drawCircle(
                                            color = Color(0xFF6200EE),
                                            radius = 4.dp.toPx(),
                                            center = Offset(x, y)
                                        )
                                    }

                                    // Fill under the line graph
                                    val fillPath = Path().apply {
                                        addPath(path)
                                        lineTo(size.width, size.height)
                                        lineTo(0f, size.height)
                                        close()
                                    }

                                    drawPath(
                                        path = fillPath,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(Color(0xFF6200EE).copy(alpha = 0.2f), Color.Transparent)
                                        )
                                    )

                                    // Line
                                    drawPath(
                                        path = path,
                                        color = Color(0xFF6200EE),
                                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                } else if (activeScores.size == 1) {
                                    // Single data entry dot
                                    val normalized = (activeScores[0] - 40f) / 60f
                                    drawCircle(
                                        color = Color(0xFF6200EE),
                                        radius = 6.dp.toPx(),
                                        center = Offset(size.width / 2, size.height - (normalized * size.height))
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Historic Entries: ${scoreHistory.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "Latest Rating: ${scoreHistory.first().overallScore}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // --- SECTION 3: CLEANUP ACCOMPLISHMENTS LOGS ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OPTIMIZATION REWARDS HISTORY",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
                if (optimizationHistory.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearHistory() }) {
                        Text("CLEAR HISTORY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (optimizationHistory.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Clean files or boost RAM to write historical records.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(optimizationHistory, key = { it.id }) { log ->
                val logDate = remember(log.timestamp) {
                    val formatter = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
                    formatter.format(Date(log.timestamp))
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (log.type) {
                                    "JUNK_CLEAN" -> Icons.Filled.CleaningServices
                                    "MEMORY_BOOST" -> Icons.Filled.Memory
                                    else -> Icons.Filled.BatteryChargingFull
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1.0f)) {
                            Text(
                                text = if (log.bytesFreed > 0) "Freed " + viewModel.formatSize(log.bytesFreed) else "RAM Boosted",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(log.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(logDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1.0f)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag("toggle_${title.lowercase().replace(" ", "_")}")
        )
    }
}
