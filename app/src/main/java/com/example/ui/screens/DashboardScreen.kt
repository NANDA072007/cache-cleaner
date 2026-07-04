package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.data.model.RecommendationItem
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val scanProgressText by viewModel.scanProgressText.collectAsStateWithLifecycle()
    
    val storageUsage by viewModel.storageUsage.collectAsStateWithLifecycle()
    val memoryUsage by viewModel.memoryUsage.collectAsStateWithLifecycle()
    val batteryStatus by viewModel.batteryStatus.collectAsStateWithLifecycle()
    val cpuStatus by viewModel.cpuStatus.collectAsStateWithLifecycle()
    
    val overallScore by viewModel.overallScore.collectAsStateWithLifecycle()
    val scores by viewModel.scores.collectAsStateWithLifecycle()
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()
    val animationsEnabled by viewModel.animationsEnabled.collectAsStateWithLifecycle()

    // CPU Usage History for Real-Time Wave Graph
    val cpuHistory = remember { mutableStateListOf<Float>() }
    LaunchedEffect(cpuStatus) {
        cpuHistory.add(cpuStatus.totalUsagePercentage.toFloat() / 100f)
        if (cpuHistory.size > 20) {
            cpuHistory.removeAt(0)
        }
    }

    // Interactive circular gauge entry animation
    val animatedScore by animateFloatAsState(
        targetValue = overallScore.toFloat(),
        animationSpec = if (animationsEnabled) tween(1000, easing = FastOutSlowInEasing) else snap(),
        label = "OverallScore"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // --- 1. CIRCULAR DEVICE HEALTH SHIELD ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("health_shield_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DEVICE OPTIMIZATION SHIELD",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.5.sp
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    // Circular Score Ring
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(180.dp)
                    ) {
                        val strokeColor = when {
                            animatedScore > 80 -> MaterialTheme.colorScheme.primary
                            animatedScore > 60 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.error
                        }
                        
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Track
                            drawCircle(
                                color = strokeColor.copy(alpha = 0.1f),
                                radius = size.minDimension / 2 - 12.dp.toPx(),
                                style = Stroke(width = 16.dp.toPx())
                            )
                            // Progress
                            drawArc(
                                color = strokeColor,
                                startAngle = -90f,
                                sweepAngle = (animatedScore / 100f) * 360f,
                                useCenter = false,
                                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round),
                                size = Size(size.width - 24.dp.toPx(), size.height - 24.dp.toPx()),
                                topLeft = Offset(12.dp.toPx(), 12.dp.toPx())
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${animatedScore.toInt()}%",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when {
                                    animatedScore > 80 -> "OPTIMIZED"
                                    animatedScore > 60 -> "FAIR"
                                    else -> "NEEDS ATTENTION"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = strokeColor,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isScanning) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = scanProgressText,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Button(
                            onClick = { viewModel.runFullScan() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("scan_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Filled.Radar, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "RUN INTELLIGENT DIAGNOSTIC",
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }

        // --- SYSTEM HEALTH SCORES SECTION ---
        item {
            Text(
                text = "SYSTEM HEALTH SCORES",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HealthScoreCard(
                    title = "Storage",
                    score = scores["STORAGE"] ?: 80,
                    icon = Icons.Outlined.Storage,
                    accentColor = MaterialTheme.colorScheme.primary,
                    statusLabel = if ((scores["STORAGE"] ?: 80) > 80) "Optimal" else "Needs Attention",
                    details = "Free: ${viewModel.formatSize(storageUsage.freeBytes)}",
                    onClick = { navController.navigate("analyzer") },
                    modifier = Modifier.weight(1f)
                )
                HealthScoreCard(
                    title = "Memory",
                    score = scores["MEMORY"] ?: 80,
                    icon = Icons.Outlined.Memory,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    statusLabel = if (memoryUsage.isSystemLowMemory) "Critical" else "Stable",
                    details = "Free: ${viewModel.formatSize(memoryUsage.totalRamBytes - memoryUsage.usedRamBytes)}",
                    onClick = { navController.navigate("apps") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HealthScoreCard(
                    title = "Battery",
                    score = scores["BATTERY"] ?: 80,
                    icon = Icons.Outlined.BatteryAlert,
                    accentColor = Color(0xFF4CAF50),
                    statusLabel = batteryStatus.healthText,
                    details = "Temp: ${batteryStatus.temperatureCelsius}°C",
                    onClick = {
                        viewModel.optimizeBattery {
                            Toast.makeText(context, "Battery rules configured. Enable battery saver inside settings.", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                HealthScoreCard(
                    title = "Processor",
                    score = scores["CPU"] ?: 80,
                    icon = Icons.Filled.DeveloperBoard,
                    accentColor = Color(0xFFFF9800),
                    statusLabel = if (cpuStatus.totalUsagePercentage > 75) "Heavy Load" else "Normal",
                    details = "Usage: ${cpuStatus.totalUsagePercentage}%",
                    onClick = { viewModel.runFullScan() },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // --- 3. HARDWARE MONITORING GRID ---
        item {
            Text(
                text = "REALTIME HARDWARE SENSORS",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }

        // STORAGE MONITOR CARD
        item {
            HardwareMonitorCard(
                title = "Disk Storage",
                icon = Icons.Outlined.Storage,
                statusText = "${viewModel.formatSize(storageUsage.usedBytes)} / ${viewModel.formatSize(storageUsage.totalBytes)} Used",
                percentage = storageUsage.usedPercentage,
                accentColor = MaterialTheme.colorScheme.primary,
                onCardClick = { navController.navigate("analyzer") }
            ) {
                LinearProgressIndicator(
                    progress = { storageUsage.usedPercentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Free: ${viewModel.formatSize(storageUsage.freeBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${storageUsage.usedPercentage}% Full",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // RAM BOOST MONITOR CARD
        item {
            HardwareMonitorCard(
                title = "RAM Memory Memory",
                icon = Icons.Outlined.Memory,
                statusText = "${viewModel.formatSize(memoryUsage.usedRamBytes)} / ${viewModel.formatSize(memoryUsage.totalRamBytes)} Used",
                percentage = memoryUsage.usedPercentage,
                accentColor = MaterialTheme.colorScheme.tertiary,
                onCardClick = {}
            ) {
                LinearProgressIndicator(
                    progress = { memoryUsage.usedPercentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Low RAM Limit: ${viewModel.formatSize(memoryUsage.lowMemoryThresholdBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (memoryUsage.isSystemLowMemory) "Status: Critical Load" else "Status: Stable",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (memoryUsage.isSystemLowMemory) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.boostMemory { message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("boost_ram_button")
                    ) {
                        Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("GC BOOST", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // REAL-TIME CPU MONITOR WITH OSCILLOSCOPE WAVE
        item {
            HardwareMonitorCard(
                title = "Processor Load",
                icon = Icons.Filled.DeveloperBoard,
                statusText = "CPU Usage: ${cpuStatus.totalUsagePercentage}% (Active Cores: ${cpuStatus.coreCount})",
                percentage = cpuStatus.totalUsagePercentage,
                accentColor = Color(0xFFFF9800),
                onCardClick = {}
            ) {
                // Real-time oscilloscope-style Canvas wave!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (cpuHistory.size > 1) {
                            val path = Path()
                            val widthInterval = size.width / (cpuHistory.size - 1)
                            
                            cpuHistory.forEachIndexed { idx, point ->
                                val x = idx * widthInterval
                                val y = size.height - (point * size.height)
                                
                                if (idx == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }
                            }

                            // Draw gradient under path
                            val fillPath = Path().apply {
                                addPath(path)
                                lineTo(size.width, size.height)
                                lineTo(0f, size.height)
                                close()
                            }
                            
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFFFF9800).copy(alpha = 0.3f), Color.Transparent)
                                )
                            )

                            // Draw line
                            drawPath(
                                path = path,
                                color = Color(0xFFFF9800),
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Processor Thermal: ${cpuStatus.thermalStatusText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "CPU Temp: ${cpuStatus.cpuTemperatureCelsius}°C",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (cpuStatus.cpuTemperatureCelsius > 42.0f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // BATTERY MONITOR CARD
        item {
            HardwareMonitorCard(
                title = "Battery Health Status",
                icon = Icons.Outlined.BatteryAlert,
                statusText = "Charge Level: ${batteryStatus.levelPercent}%",
                percentage = batteryStatus.levelPercent,
                accentColor = Color(0xFF4CAF50),
                onCardClick = {}
            ) {
                LinearProgressIndicator(
                    progress = { batteryStatus.levelPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = Color(0xFF4CAF50),
                    trackColor = Color(0xFF4CAF50).copy(alpha = 0.15f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Condition: ${batteryStatus.healthText} | State: ${batteryStatus.statusText}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Details: ${batteryStatus.voltageMilliVolts}mV | Cap: ${batteryStatus.designCapacityMah}mAh",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.optimizeBattery {
                                Toast.makeText(context, "Battery rules configured. Enable battery saver inside settings.", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("optimize_battery_button")
                    ) {
                        Icon(Icons.Outlined.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SAVE RULES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun HardwareMonitorCard(
    title: String,
    icon: ImageVector,
    statusText: String,
    percentage: Int,
    accentColor: Color,
    onCardClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .testTag("hardware_card_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Box(
                    modifier = Modifier
                        .background(accentColor.copy(alpha = 0.12f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${percentage}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = accentColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
fun HealthScoreCard(
    title: String,
    score: Int,
    icon: ImageVector,
    accentColor: Color,
    statusLabel: String,
    details: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("health_score_card_${title.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(accentColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(32.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = accentColor.copy(alpha = 0.1f),
                            radius = size.minDimension / 2 - 2.dp.toPx(),
                            style = Stroke(width = 3.dp.toPx())
                        )
                        drawArc(
                            color = accentColor,
                            startAngle = -90f,
                            sweepAngle = (score / 100f) * 360f,
                            useCenter = false,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = "$score",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = accentColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
