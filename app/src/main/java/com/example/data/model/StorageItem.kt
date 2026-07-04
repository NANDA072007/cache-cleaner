package com.example.data.model

import java.io.File

data class StorageItem(
    val file: File,
    val name: String,
    val path: String,
    val size: Long,
    val isDirectory: Boolean,
    val category: String // "LARGE_FILES", "DUPLICATES", "DOWNLOADS", "APKS", "CACHE", "LOGS", "TEMP", "EMPTY_FOLDER"
)
