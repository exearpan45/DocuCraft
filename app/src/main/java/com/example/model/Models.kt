package com.example.model

import android.graphics.Bitmap
import android.net.Uri
import java.io.File

enum class ImageFormat(val displayName: String, val extension: String, val mimeType: String, val compressFormat: Bitmap.CompressFormat) {
    JPEG("JPEG (.jpg)", "jpg", "image/jpeg", Bitmap.CompressFormat.JPEG),
    PNG("PNG (.png)", "png", "image/png", Bitmap.CompressFormat.PNG),
    WEBP("WEBP (.webp)", "webp", "image/webp", Bitmap.CompressFormat.WEBP)
}

data class ImageDetails(
    val uri: Uri,
    val fileName: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val mimeType: String
) {
    val formattedDimensions: String get() = "${width} × ${height} px"
    val formattedSize: String get() = formatFileSize(sizeBytes)
    val aspectRatioString: String get() = calculateAspectRatio(width, height)
}

data class PdfDetails(
    val uri: Uri,
    val fileName: String,
    val sizeBytes: Long,
    val pageCount: Int,
    val isEncrypted: Boolean = false,
    val isCorrupted: Boolean = false
) {
    val formattedSize: String get() = formatFileSize(sizeBytes)
}

data class PdfPageItem(
    val originalPageIndex: Int, // 0-based
    val displayPageNumber: Int,  // 1-based
    val isSelected: Boolean = true,
    val thumbnail: Bitmap? = null
)

enum class OutputFileType {
    IMAGE,
    PDF
}

sealed interface UiProcessingState {
    data object Idle : UiProcessingState
    data class Processing(
        val progress: Float = 0f,
        val message: String = "Processing...",
        val cancellable: Boolean = true
    ) : UiProcessingState
    data class Success(
        val resultFile: File,
        val title: String,
        val message: String,
        val originalSize: Long,
        val newSize: Long,
        val fileType: OutputFileType
    ) : UiProcessingState
    data class Error(
        val title: String = "Operation Failed",
        val message: String
    ) : UiProcessingState
}

data class HistoryItem(
    val id: String,
    val fileName: String,
    val fileType: OutputFileType,
    val sizeBytes: Long,
    val timestamp: Long,
    val filePath: String
) {
    val formattedSize: String get() = formatFileSize(sizeBytes)
}

enum class PdfPageSize(val displayName: String, val widthPoints: Int, val heightPoints: Int) {
    A4("A4 (595 × 842)", 595, 842),
    LETTER("US Letter (612 × 792)", 612, 792),
    FIT_IMAGE("Fit Image Dimensions", 0, 0)
}

enum class CropAspectRatio(val label: String, val ratio: Float?) {
    FREE("Free", null),
    SQUARE("1:1", 1.0f),
    FOUR_THREE("4:3", 4f / 3f),
    SIXTEEN_NINE("16:9", 16f / 9f),
    THREE_TWO("3:2", 3f / 2f),
    NINE_SIXTEEN("9:16", 9f / 16f)
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.2f MB", mb)
        kb >= 1.0 -> String.format("%.1f KB", kb)
        else -> "$bytes B"
    }
}

fun calculateAspectRatio(width: Int, height: Int): String {
    if (width <= 0 || height <= 0) return "1:1"
    fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
    val g = gcd(width, height)
    val rW = width / g
    val rH = height / g
    return if (rW in 1..20 && rH in 1..20) {
        "$rW:$rH"
    } else {
        val ratio = width.toFloat() / height.toFloat()
        String.format("%.2f:1", ratio)
    }
}

enum class TargetSizePreset(val label: String, val targetBytes: Long) {
    UNDER_100_KB("< 100 KB", 100 * 1024L),
    UNDER_200_KB("< 200 KB", 200 * 1024L),
    UNDER_500_KB("< 500 KB", 500 * 1024L),
    UNDER_1_MB("< 1 MB", 1024 * 1024L),
    CUSTOM("Custom KB", 0L)
}

enum class HistorySortOption(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    NAME_ASC("Name A-Z"),
    SIZE_DESC("Largest")
}

fun filterAndSortHistory(
    items: List<HistoryItem>,
    query: String,
    sortOption: HistorySortOption
): List<HistoryItem> {
    val filtered = if (query.isBlank()) {
        items
    } else {
        val q = query.trim().lowercase()
        val sdf = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
        items.filter { item ->
            val dateStr = sdf.format(java.util.Date(item.timestamp)).lowercase()
            item.fileName.lowercase().contains(q) ||
                    dateStr.contains(q) ||
                    item.fileType.name.lowercase().contains(q)
        }
    }

    return when (sortOption) {
        HistorySortOption.NEWEST -> filtered.sortedByDescending { it.timestamp }
        HistorySortOption.OLDEST -> filtered.sortedBy { it.timestamp }
        HistorySortOption.NAME_ASC -> filtered.sortedBy { it.fileName.lowercase() }
        HistorySortOption.SIZE_DESC -> filtered.sortedByDescending { it.sizeBytes }
    }
}

enum class ImageFilterMode(val displayName: String) {
    ORIGINAL("Original"),
    GRAYSCALE("B&W / Grayscale"),
    DOC_SCAN("Document Enhance"),
    SEPIA("Warm Sepia"),
    INVERT("Color Invert")
}

data class WatermarkConfig(
    val isEnabled: Boolean = false,
    val text: String = "CONFIDENTIAL",
    val alpha: Float = 0.35f,
    val isDiagonal: Boolean = true
)


