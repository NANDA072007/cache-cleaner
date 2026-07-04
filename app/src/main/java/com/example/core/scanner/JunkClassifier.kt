package com.example.core.scanner

import com.example.core.model.StorageItem
import java.io.File

class JunkClassifier {

    fun classify(file: File): StorageItem {
        val path = file.absolutePath
        val isDir = file.isDirectory
        val size = if (isDir) 0L else file.length()
        val name = file.name
        val ext = file.extension.lowercase()

        var category = "CACHE"
        if (size > 10 * 1024 * 1024) { // > 10 MB
            category = "LARGE_FILES"
        } else if (ext == "apk") {
            category = "APKS"
        } else if (ext == "log") {
            category = "LOGS"
        } else if (ext == "tmp" || ext == "temp") {
            category = "TEMP"
        } else if (path.contains("/Download/", ignoreCase = true) || path.contains("/Downloads/", ignoreCase = true)) {
            category = "DOWNLOADS"
        } else if (path.contains("whatsapp", ignoreCase = true)) {
            category = "WHATSAPP_MEDIA"
        } else if (path.contains("telegram", ignoreCase = true)) {
            category = "TELEGRAM_MEDIA"
        } else if (isDir && (file.listFiles()?.isEmpty() == true)) {
            category = "EMPTY_FOLDER"
        }

        return StorageItem(
            file = file,
            name = name,
            path = path,
            size = size,
            isDirectory = isDir,
            category = category
        )
    }
}
