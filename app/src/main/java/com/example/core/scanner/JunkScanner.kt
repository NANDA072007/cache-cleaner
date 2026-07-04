package com.example.core.scanner

import android.content.Context
import android.os.Environment
import com.example.core.model.StorageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

class JunkScanner(private val context: Context) {

    private val pauseLock = Mutex()
    private var isPaused = false

    suspend fun pause() {
        pauseLock.withLock {
            isPaused = true
        }
    }

    suspend fun resume() {
        pauseLock.withLock {
            isPaused = false
        }
    }

    private suspend fun checkPaused() {
        while (true) {
            coroutineContext.ensureActive()
            pauseLock.withLock {
                if (!isPaused) return
            }
            kotlinx.coroutines.delay(100)
        }
    }

    suspend fun scan(
        onProgress: (String, Int) -> Unit
    ): List<File> = withContext(Dispatchers.IO) {
        val filesFound = mutableListOf<File>()
        val dirsToScan = mutableListOf<File>()

        // Core app internal dirs
        dirsToScan.add(context.cacheDir)
        dirsToScan.add(context.filesDir)

        // Core app external dirs
        context.externalCacheDir?.let { dirsToScan.add(it) }
        context.getExternalFilesDir(null)?.let { dirsToScan.add(it) }

        // Root External storage if readable, otherwise standard public folders
        try {
            val root = Environment.getExternalStorageDirectory()
            if (root.exists() && root.canRead()) {
                dirsToScan.add(root)
            } else {
                val publicDirs = listOf(
                    Environment.DIRECTORY_DOWNLOADS,
                    Environment.DIRECTORY_DCIM,
                    Environment.DIRECTORY_PICTURES,
                    Environment.DIRECTORY_MOVIES,
                    Environment.DIRECTORY_MUSIC
                )
                for (dirType in publicDirs) {
                    val dir = Environment.getExternalStoragePublicDirectory(dirType)
                    if (dir.exists() && dir.canRead()) {
                        dirsToScan.add(dir)
                    }
                }
            }
        } catch (e: Exception) {
            // Safe fallback
        }

        var count = 0
        for (dir in dirsToScan) {
            checkPaused()
            coroutineContext.ensureActive()
            
            onProgress("Scanning ${dir.name}...", count)
            scanDirRecursively(dir, filesFound, maxDepth = 15, currentDepth = 0) {
                count++
                if (count % 20 == 0) {
                    onProgress("Discovered $count file handles...", count)
                }
            }
        }
        filesFound
    }

    private suspend fun scanDirRecursively(
        directory: File,
        filesFound: MutableList<File>,
        maxDepth: Int,
        currentDepth: Int,
        onFileFound: suspend () -> Unit
    ) {
        checkPaused()
        coroutineContext.ensureActive()

        if (currentDepth > maxDepth) return
        val files = try {
            directory.listFiles()
        } catch (e: Exception) {
            null
        } ?: return

        if (files.isEmpty()) {
            filesFound.add(directory)
            onFileFound()
            return
        }

        for (file in files) {
            if (file.isDirectory) {
                scanDirRecursively(file, filesFound, maxDepth, currentDepth + 1, onFileFound)
            } else {
                filesFound.add(file)
                onFileFound()
            }
        }
    }
}
