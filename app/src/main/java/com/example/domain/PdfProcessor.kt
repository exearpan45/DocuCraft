package com.example.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.example.model.PdfDetails
import com.example.model.PdfPageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class PdfProcessor(
    private val context: Context,
    private val storageManager: FileStorageManager
) {

    suspend fun getPdfDetails(uri: Uri): PdfDetails = withContext(Dispatchers.IO) {
        val (name, size) = storageManager.getFileNameAndSize(uri)
        var tempFile: File? = null
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null

        try {
            tempFile = storageManager.copyToTempFile(uri, prefix = "inspect", extension = "pdf")
            if (tempFile.length() == 0L) {
                return@withContext PdfDetails(
                    uri = uri,
                    fileName = name,
                    sizeBytes = size,
                    pageCount = 0,
                    isCorrupted = true
                )
            }

            pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount

            PdfDetails(
                uri = uri,
                fileName = name,
                sizeBytes = if (size > 0) size else tempFile.length(),
                pageCount = pageCount,
                isEncrypted = false,
                isCorrupted = false
            )
        } catch (se: SecurityException) {
            // Password protected PDF
            PdfDetails(
                uri = uri,
                fileName = name,
                sizeBytes = size,
                pageCount = 0,
                isEncrypted = true,
                isCorrupted = false
            )
        } catch (ioe: IOException) {
            PdfDetails(
                uri = uri,
                fileName = name,
                sizeBytes = size,
                pageCount = 0,
                isEncrypted = false,
                isCorrupted = true
            )
        } catch (e: Exception) {
            PdfDetails(
                uri = uri,
                fileName = name,
                sizeBytes = size,
                pageCount = 0,
                isEncrypted = false,
                isCorrupted = true
            )
        } finally {
            renderer?.close()
            pfd?.close()
            tempFile?.delete()
        }
    }

    suspend fun loadPageThumbnails(
        uri: Uri,
        maxPages: Int = 100,
        thumbnailWidth: Int = 300
    ): List<PdfPageItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<PdfPageItem>()
        var tempFile: File? = null
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null

        try {
            tempFile = storageManager.copyToTempFile(uri, prefix = "thumb", extension = "pdf")
            pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val count = min(renderer.pageCount, maxPages)

            for (i in 0 until count) {
                val page = renderer.openPage(i)
                val aspect = page.height.toFloat() / max(1, page.width)
                val targetHeight = max(1, (thumbnailWidth * aspect).roundToInt())

                val bitmap = Bitmap.createBitmap(thumbnailWidth, targetHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                list.add(
                    PdfPageItem(
                        originalPageIndex = i,
                        displayPageNumber = i + 1,
                        isSelected = true,
                        thumbnail = bitmap
                    )
                )
            }
        } catch (e: Exception) {
            // Fallback or empty on failure
        } finally {
            renderer?.close()
            pfd?.close()
            tempFile?.delete()
        }
        list
    }

    suspend fun mergePdfs(
        uris: List<Uri>,
        onProgress: (Float, String) -> Unit
    ): File = withContext(Dispatchers.IO) {
        if (uris.size < 2) throw IllegalArgumentException("At least two PDF files are required for merging.")

        val outputDoc = PdfDocument()
        val tempFiles = mutableListOf<File>()
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        var totalGlobalPages = 0

        try {
            // Pre-scan total pages for accurate progress
            for (uri in uris) {
                val temp = storageManager.copyToTempFile(uri, prefix = "merge_in", extension = "pdf")
                tempFiles.add(temp)
                ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { r ->
                        totalGlobalPages += r.pageCount
                    }
                }
            }

            var writtenPages = 0
            for ((docIndex, tempFile) in tempFiles.withIndex()) {
                ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        for (pageIndex in 0 until renderer.pageCount) {
                            val progress = writtenPages.toFloat() / max(1, totalGlobalPages)
                            onProgress(progress, "Merging doc ${docIndex + 1}/${uris.size}, page ${pageIndex + 1}...")

                            val page = renderer.openPage(pageIndex)
                            val pageW = page.width
                            val pageH = page.height

                            val bitmap = Bitmap.createBitmap(pageW, pageH, Bitmap.Config.ARGB_8888)
                            val c = Canvas(bitmap)
                            c.drawColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                            page.close()

                            val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, writtenPages + 1).create()
                            val docPage = outputDoc.startPage(pageInfo)
                            docPage.canvas.drawBitmap(bitmap, 0f, 0f, paint)
                            outputDoc.finishPage(docPage)
                            bitmap.recycle()

                            writtenPages++
                        }
                    }
                }
            }

            onProgress(0.95f, "Writing merged PDF file...")
            val outputFile = storageManager.createOutputFile("docucraft_merged", "pdf")
            FileOutputStream(outputFile).use { out ->
                outputDoc.writeTo(out)
            }
            onProgress(1.0f, "Completed")
            outputFile
        } finally {
            outputDoc.close()
            tempFiles.forEach { it.delete() }
        }
    }

    suspend fun splitPdf(
        uri: Uri,
        pageIndices: List<Int>, // 0-based
        onProgress: (Float, String) -> Unit
    ): File = withContext(Dispatchers.IO) {
        if (pageIndices.isEmpty()) throw IllegalArgumentException("No pages selected to extract.")

        val tempFile = storageManager.copyToTempFile(uri, prefix = "split_src", extension = "pdf")
        val outputDoc = PdfDocument()
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        try {
            ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val total = pageIndices.size
                    for ((outIndex, srcPageIndex) in pageIndices.withIndex()) {
                        if (srcPageIndex < 0 || srcPageIndex >= renderer.pageCount) continue
                        onProgress(outIndex.toFloat() / total, "Extracting page ${srcPageIndex + 1} ($outIndex/$total)...")

                        val page = renderer.openPage(srcPageIndex)
                        val pageW = page.width
                        val pageH = page.height

                        val bitmap = Bitmap.createBitmap(pageW, pageH, Bitmap.Config.ARGB_8888)
                        val c = Canvas(bitmap)
                        c.drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        page.close()

                        val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, outIndex + 1).create()
                        val docPage = outputDoc.startPage(pageInfo)
                        docPage.canvas.drawBitmap(bitmap, 0f, 0f, paint)
                        outputDoc.finishPage(docPage)
                        bitmap.recycle()
                    }
                }
            }

            onProgress(0.95f, "Saving extracted pages...")
            val (origName, _) = storageManager.getFileNameAndSize(uri)
            val outputFile = storageManager.createOutputFile("${origName}_split", "pdf")
            FileOutputStream(outputFile).use { out ->
                outputDoc.writeTo(out)
            }
            onProgress(1.0f, "Completed")
            outputFile
        } finally {
            outputDoc.close()
            tempFile.delete()
        }
    }

    suspend fun reorderAndRemovePages(
        uri: Uri,
        newOrderIndices: List<Int>, // 0-based indices in desired new sequence
        onProgress: (Float, String) -> Unit
    ): File = withContext(Dispatchers.IO) {
        if (newOrderIndices.isEmpty()) throw IllegalArgumentException("Cannot create an empty PDF. Please keep at least one page.")

        val tempFile = storageManager.copyToTempFile(uri, prefix = "reorder_src", extension = "pdf")
        val outputDoc = PdfDocument()
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        try {
            ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val total = newOrderIndices.size
                    for ((outIndex, srcPageIndex) in newOrderIndices.withIndex()) {
                        if (srcPageIndex < 0 || srcPageIndex >= renderer.pageCount) continue
                        onProgress(outIndex.toFloat() / total, "Assembling page ${outIndex + 1} of $total...")

                        val page = renderer.openPage(srcPageIndex)
                        val pageW = page.width
                        val pageH = page.height

                        val bitmap = Bitmap.createBitmap(pageW, pageH, Bitmap.Config.ARGB_8888)
                        val c = Canvas(bitmap)
                        c.drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        page.close()

                        val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, outIndex + 1).create()
                        val docPage = outputDoc.startPage(pageInfo)
                        docPage.canvas.drawBitmap(bitmap, 0f, 0f, paint)
                        outputDoc.finishPage(docPage)
                        bitmap.recycle()
                    }
                }
            }

            onProgress(0.95f, "Finalizing reordered PDF...")
            val (origName, _) = storageManager.getFileNameAndSize(uri)
            val outputFile = storageManager.createOutputFile("${origName}_reordered", "pdf")
            FileOutputStream(outputFile).use { out ->
                outputDoc.writeTo(out)
            }
            onProgress(1.0f, "Completed")
            outputFile
        } finally {
            outputDoc.close()
            tempFile.delete()
        }
    }

    suspend fun compressPdf(
        uri: Uri,
        compressionLevel: PdfCompressionLevel,
        onProgress: (Float, String) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val tempFile = storageManager.copyToTempFile(uri, prefix = "compress_src", extension = "pdf")
        val outputDoc = PdfDocument()
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        try {
            ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val total = renderer.pageCount
                    for (i in 0 until total) {
                        onProgress(i.toFloat() / total, "Compressing page ${i + 1} of $total...")
                        val page = renderer.openPage(i)

                        // Scale dimensions based on compression level
                        val scale = compressionLevel.resolutionScale
                        val renderW = max(1, (page.width * scale).roundToInt())
                        val renderH = max(1, (page.height * scale).roundToInt())

                        val highResBitmap = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(highResBitmap)
                        canvas.drawColor(Color.WHITE)
                        page.render(highResBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        page.close()

                        // Compress the bitmap via JPEG stream
                        val baos = ByteArrayOutputStream()
                        highResBitmap.compress(Bitmap.CompressFormat.JPEG, compressionLevel.jpegQuality, baos)
                        highResBitmap.recycle()

                        val compressedBytes = baos.toByteArray()
                        val compressedBmp = android.graphics.BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)

                        // Draw onto standard point-sized page so physical dimensions remain standard (e.g. A4/Letter)
                        val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, i + 1).create()
                        val docPage = outputDoc.startPage(pageInfo)
                        docPage.canvas.drawBitmap(compressedBmp, null, Rect(0, 0, page.width, page.height), paint)
                        outputDoc.finishPage(docPage)
                        compressedBmp.recycle()
                    }
                }
            }

            onProgress(0.95f, "Writing compressed PDF...")
            val (origName, _) = storageManager.getFileNameAndSize(uri)
            val outputFile = storageManager.createOutputFile("${origName}_compressed", "pdf")
            FileOutputStream(outputFile).use { out ->
                outputDoc.writeTo(out)
            }
            onProgress(1.0f, "Completed")
            outputFile
        } finally {
            outputDoc.close()
            tempFile.delete()
        }
    }

    suspend fun rotatePdfPages(
        uri: Uri,
        rotationDegrees: Int,
        onProgress: (Float, String) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val tempFile = storageManager.copyToTempFile(uri, prefix = "rotate_src", extension = "pdf")
        val outputDoc = PdfDocument()
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        try {
            ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val total = renderer.pageCount
                    for (i in 0 until total) {
                        onProgress(i.toFloat() / total, "Rotating page ${i + 1} of $total...")
                        val page = renderer.openPage(i)

                        val origW = page.width
                        val origH = page.height

                        val bmp = Bitmap.createBitmap(origW, origH, Bitmap.Config.ARGB_8888)
                        val c = Canvas(bmp)
                        c.drawColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        page.close()

                        val matrix = android.graphics.Matrix().apply {
                            postRotate(rotationDegrees.toFloat())
                        }
                        val rotatedBmp = Bitmap.createBitmap(bmp, 0, 0, origW, origH, matrix, true)
                        bmp.recycle()

                        val newPageW = if (rotationDegrees == 90 || rotationDegrees == 270) origH else origW
                        val newPageH = if (rotationDegrees == 90 || rotationDegrees == 270) origW else origH

                        val pageInfo = PdfDocument.PageInfo.Builder(newPageW, newPageH, i + 1).create()
                        val docPage = outputDoc.startPage(pageInfo)
                        docPage.canvas.drawBitmap(rotatedBmp, 0f, 0f, paint)
                        outputDoc.finishPage(docPage)
                        rotatedBmp.recycle()
                    }
                }
            }

            onProgress(0.95f, "Saving rotated PDF...")
            val (origName, _) = storageManager.getFileNameAndSize(uri)
            val outputFile = storageManager.createOutputFile("${origName}_rot${rotationDegrees}", "pdf")
            FileOutputStream(outputFile).use { out ->
                outputDoc.writeTo(out)
            }
            onProgress(1.0f, "Completed")
            outputFile
        } finally {
            outputDoc.close()
            tempFile.delete()
        }
    }

    suspend fun watermarkPdf(
        uri: Uri,
        watermarkText: String,
        onProgress: (Float, String) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val tempFile = storageManager.copyToTempFile(uri, prefix = "wm_src", extension = "pdf")
        val outputDoc = PdfDocument()
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        try {
            ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val total = renderer.pageCount
                    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.RED
                        alpha = 75
                        textSize = 48f
                        textAlign = Paint.Align.CENTER
                        isFakeBoldText = true
                    }

                    for (i in 0 until total) {
                        onProgress(i.toFloat() / total, "Watermarking page ${i + 1} of $total...")
                        val page = renderer.openPage(i)
                        val w = page.width
                        val h = page.height

                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        val c = Canvas(bmp)
                        c.drawColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        page.close()

                        val pageInfo = PdfDocument.PageInfo.Builder(w, h, i + 1).create()
                        val docPage = outputDoc.startPage(pageInfo)
                        docPage.canvas.drawBitmap(bmp, 0f, 0f, paint)

                        docPage.canvas.save()
                        val centerX = w / 2f
                        val centerY = h / 2f
                        docPage.canvas.rotate(-40f, centerX, centerY)
                        docPage.canvas.drawText(watermarkText, centerX, centerY, textPaint)
                        docPage.canvas.restore()

                        outputDoc.finishPage(docPage)
                        bmp.recycle()
                    }
                }
            }

            onProgress(0.95f, "Finalizing watermarked PDF...")
            val (origName, _) = storageManager.getFileNameAndSize(uri)
            val outputFile = storageManager.createOutputFile("${origName}_watermarked", "pdf")
            FileOutputStream(outputFile).use { out ->
                outputDoc.writeTo(out)
            }
            onProgress(1.0f, "Completed")
            outputFile
        } finally {
            outputDoc.close()
            tempFile.delete()
        }
    }
}

enum class PdfCompressionLevel(
    val title: String,
    val description: String,
    val resolutionScale: Float,
    val jpegQuality: Int
) {
    BALANCED("Balanced (Recommended)", "Good text clarity with medium file reduction", 1.2f, 75),
    STRONG("Maximum Compression", "Smallest file size; pages rasterized at standard 72 DPI", 1.0f, 60),
    LIGHT("Light Compression", "High clarity and minimal visual difference", 1.5f, 85)
}
