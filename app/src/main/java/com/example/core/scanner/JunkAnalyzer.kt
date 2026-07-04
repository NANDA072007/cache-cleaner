package com.example.core.scanner

import com.example.core.model.StorageItem
import java.io.File

class JunkAnalyzer {

    fun analyze(items: List<StorageItem>): List<StorageItem> {
        val result = mutableListOf<StorageItem>()
        val fileMap = mutableMapOf<Long, MutableList<StorageItem>>()

        // Populate duplicates mapping for any non-directory file with positive size
        for (item in items) {
            if (!item.isDirectory && item.size > 0) {
                val group = fileMap.getOrPut(item.size) { mutableListOf() }
                group.add(item)
            }
            result.add(item)
        }

        // Add explicit duplicate entries
        for ((size, fileList) in fileMap) {
            if (fileList.size > 1) {
                // Keep index 0 as the "original", classify index 1..N as DUPLICATES
                for (i in 1 until fileList.size) {
                    val dup = fileList[i]
                    result.add(dup.copy(category = "DUPLICATES"))
                }
            }
        }

        // Sort resulting storage items descending by size to present largest targets first
        return result.sortedByDescending { it.size }
    }
}
