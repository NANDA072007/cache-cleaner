package com.example.data.model

data class RecommendationItem(
    val id: String,
    val title: String,
    val description: String,
    val category: String, // "STORAGE", "MEMORY", "BATTERY", "CPU", "NOTIFICATIONS"
    val severity: String, // "CRITICAL", "WARNING", "INFO"
    val actionText: String?,
    val actionRoute: String? // Route to navigate to inside the app (e.g. "cleaner", "apps", "settings")
)
