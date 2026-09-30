package com.guddiz.app.core.receipt

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/**
 * Handles physical receipt files on the device.
 *
 * Files live in  filesDir/receipts/<name>.jpg
 * The database only stores the absolute path — never the file contents.
 */
object ReceiptStorage {

    private const val DIR_NAME = "receipts"

    fun receiptsDir(context: Context): File {
        val dir = File(context.filesDir, DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun newReceiptFile(context: Context): File {
        val name = "receipt_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
        return File(receiptsDir(context), name)
    }

    /** URI to hand to the camera intent — requires FileProvider. */
    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

    /** Copies a picked gallery image into our private storage and returns the new file. */
    fun copyFromUri(context: Context, source: Uri): File? = try {
        val dest = newReceiptFile(context)
        context.contentResolver.openInputStream(source)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        dest
    } catch (e: Exception) {
        null
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        try { File(path).delete() } catch (_: Exception) {}
    }

    fun exists(path: String?): Boolean =
        !path.isNullOrBlank() && File(path).exists()
}
