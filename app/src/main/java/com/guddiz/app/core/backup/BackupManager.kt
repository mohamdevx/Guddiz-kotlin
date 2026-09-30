package com.guddiz.app.core.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.guddiz.app.data.local.GuddizDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

sealed interface BackupResult {
    data object Success : BackupResult
    data class Error(val message: String) : BackupResult
}

class BackupManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("guddiz_backup", Context.MODE_PRIVATE)

    fun lastBackupTime(): Long = prefs.getLong(KEY_LAST_BACKUP, 0L)

    private fun recordBackup() {
        prefs.edit().putLong(KEY_LAST_BACKUP, System.currentTimeMillis()).apply()
    }

    suspend fun createBackup(destination: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            GuddizDatabase.close()

            val output = context.contentResolver.openOutputStream(destination)
                ?: return@withContext BackupResult.Error("Could not open the destination file.")

            ZipOutputStream(BufferedOutputStream(output)).use { zip ->
                zip.putNextEntry(ZipEntry("metadata.txt"))
                val meta = "Guddiz Backup\nversion=1\ntimestamp=" + System.currentTimeMillis() + "\n"
                zip.write(meta.toByteArray())
                zip.closeEntry()

                val dbFile = context.getDatabasePath(DB_NAME)
                if (dbFile.exists()) {
                    zip.putNextEntry(ZipEntry(DB_NAME))
                    dbFile.inputStream().use { input -> input.copyTo(zip) }
                    zip.closeEntry()
                }

                val receiptsDir = File(context.filesDir, RECEIPTS_DIR)
                if (receiptsDir.exists()) {
                    val files = receiptsDir.listFiles()
                    if (files != null) {
                        for (file in files) {
                            if (file.isFile) {
                                val entryName = RECEIPTS_DIR + "/" + file.name
                                zip.putNextEntry(ZipEntry(entryName))
                                file.inputStream().use { input -> input.copyTo(zip) }
                                zip.closeEntry()
                            }
                        }
                    }
                }
            }

            recordBackup()
            BackupResult.Success
        } catch (e: Exception) {
            BackupResult.Error(e.message ?: "Backup failed.")
        }
    }

    suspend fun restoreBackup(source: Uri): BackupResult = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "restore_temp")
        tempDir.deleteRecursively()
        tempDir.mkdirs()

        try {
            GuddizDatabase.close()

            val input = context.contentResolver.openInputStream(source)
                ?: return@withContext BackupResult.Error("Could not open the backup file.")

            var foundDb = false
            ZipInputStream(BufferedInputStream(input)).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val target = File(tempDir, entry.name)
                        val parent = target.parentFile
                        if (parent != null) parent.mkdirs()

                        val canonical = target.canonicalPath
                        val baseCanonical = tempDir.canonicalPath
                        if (!canonical.startsWith(baseCanonical)) {
                            tempDir.deleteRecursively()
                            return@withContext BackupResult.Error("This backup file is not valid.")
                        }

                        target.outputStream().use { out -> zip.copyTo(out) }
                        if (entry.name == DB_NAME) foundDb = true
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            if (!foundDb) {
                tempDir.deleteRecursively()
                return@withContext BackupResult.Error(
                    "This file is not a Guddiz backup - no database found inside."
                )
            }

            val testDb = File(tempDir, DB_NAME)
            try {
                val probe = SQLiteDatabase.openDatabase(
                    testDb.absolutePath, null, SQLiteDatabase.OPEN_READONLY
                )
                probe.close()
            } catch (e: Exception) {
                tempDir.deleteRecursively()
                return@withContext BackupResult.Error(
                    "This backup file is damaged and cannot be restored."
                )
            }

            val liveDb = context.getDatabasePath(DB_NAME)
            val liveDir = liveDb.parentFile
            if (liveDir != null) liveDir.mkdirs()
            testDb.copyTo(liveDb, overwrite = true)
            File(liveDb.path + "-wal").delete()
            File(liveDb.path + "-shm").delete()
            File(liveDb.path + "-journal").delete()

            val liveReceipts = File(context.filesDir, RECEIPTS_DIR)
            liveReceipts.deleteRecursively()
            liveReceipts.mkdirs()

            val tempReceipts = File(tempDir, RECEIPTS_DIR)
            if (tempReceipts.exists()) {
                val files = tempReceipts.listFiles()
                if (files != null) {
                    for (f in files) {
                        if (f.isFile) {
                            f.copyTo(File(liveReceipts, f.name), overwrite = true)
                        }
                    }
                }
            }

            tempDir.deleteRecursively()
            BackupResult.Success
        } catch (e: Exception) {
            tempDir.deleteRecursively()
            BackupResult.Error(e.message ?: "Restore failed.")
        }
    }

    companion object {
        private const val KEY_LAST_BACKUP = "last_backup"
        private const val DB_NAME = "guddiz.db"
        private const val RECEIPTS_DIR = "receipts"
    }
}
