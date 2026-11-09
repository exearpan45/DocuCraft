package com.example.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.model.OutputFileType
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileStorageManager(private val context: Context) {

    private val outputDir: File
        get() {
            val dir = File(context.filesDir, "docucraft_outputs")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    private val tempDir: File
        get() {
            val dir = File(context.cacheDir, "docucraft_temp")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    fun getFileNameAndSize(uri: Uri): Pair<String, Long> {
        var name = "file"
        var size = 0L

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1 && !cursor.isNull(nameIndex)) {
                        name = cursor.getString(nameIndex)
                    }
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {}

        if (name == "file") {
            name = uri.lastPathSegment ?: "unknown_${System.currentTimeMillis()}"
        }
        return Pair(name, size)
    }

    /**
     * Copies a content URI into a local temp file.
     * Essential for PdfRenderer which requires a seekable FileDescriptor.
     */
    fun copyToTempFile(uri: Uri, prefix: String = "input", extension: String = "tmp"): File {
        val tempFile = File.createTempFile(prefix, ".$extension", tempDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalArgumentException("Cannot open stream for URI: $uri")
        return tempFile
    }

    fun createOutputFile(baseName: String, extension: String): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val cleanBase = baseName.substringBeforeLast(".").take(24).replace(Regex("[^a-zA-Z0-9_]"), "_")
        val fileName = "${cleanBase}_$timeStamp.$extension"
        return File(outputDir, fileName)
    }

    fun saveToDestinationUri(sourceFile: File, destinationUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(destinationUri)?.use { outStream ->
                sourceFile.inputStream().use { inStream ->
                    inStream.copyTo(outStream)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getShareUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun createShareIntent(file: File, mimeType: String): Intent {
        val uri = getShareUri(file)
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun createViewIntent(file: File, mimeType: String): Intent {
        val uri = getShareUri(file)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun getOutputFiles(): List<File> {
        return outputDir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun deleteOutputFile(file: File): Boolean {
        return if (file.exists()) file.delete() else false
    }

    fun clearTempFiles() {
        try {
            tempDir.listFiles()?.forEach { if (it.isFile) it.delete() }
        } catch (_: Exception) {}
    }
}
