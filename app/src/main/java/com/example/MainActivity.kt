package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.database.AppDatabase
import com.example.data.repository.SettingsRepository
import com.example.ui.navigation.Screen
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Support modern transparent notch and system navigation bars
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            
            // Singleton Database & Preferences Datastore
            val database = remember { AppDatabase.getDatabase(context) }
            val settingsRepository = remember { SettingsRepository(context) }

            // ViewModel instantiation using Constructor Injection and Factory Pattern
            val factory = remember { MainViewModelFactory(context, database, settingsRepository) }
            val viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)

            // Collect active configuration preferences
            val userTheme by viewModel.theme.collectAsStateWithLifecycle()
            val animationsEnabled by viewModel.animationsEnabled.collectAsStateWithLifecycle()

            val isDark = when (userTheme) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark) {
                val navController = rememberNavController()
                val items = listOf(
                    Screen.Dashboard,
                    Screen.Cleaner,
                    Screen.Analyzer,
                    Screen.Apps,
                    Screen.Settings
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.testTag("bottom_nav_bar")
                        ) {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentDestination = navBackStackEntry?.destination
                            
                            items.forEach { screen ->
                                val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) screen.filledIcon else screen.outlinedIcon,
                                            contentDescription = screen.title
                                        )
                                    },
                                    label = { Text(screen.title) },
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    modifier = Modifier.testTag("nav_item_${screen.route}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Dashboard.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Dashboard.route) {
                            DashboardScreen(navController, viewModel)
                        }
                        composable(Screen.Cleaner.route) {
                            CleanerScreen(viewModel)
                        }
                        composable(Screen.Analyzer.route) {
                            AnalyzerScreen(viewModel)
                        }
                        composable(Screen.Apps.route) {
                            AppsScreen(viewModel)
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}
