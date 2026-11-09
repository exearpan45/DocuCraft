package com.example.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.model.CropAspectRatio
import com.example.model.ImageFilterMode
import com.example.model.ImageDetails
import com.example.model.ImageFormat
import com.example.model.PdfPageSize
import com.example.model.WatermarkConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ImageProcessor(
    private val context: Context,
    private val storageManager: FileStorageManager
) {

    suspend fun getImageDetails(uri: Uri): ImageDetails = withContext(Dispatchers.IO) {
        val (name, size) = storageManager.getFileNameAndSize(uri)
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: throw IllegalArgumentException("Could not read image from URI")

        val width = options.outWidth
        val height = options.outHeight
        val mimeType = options.outMimeType ?: "image/jpeg"

        ImageDetails(
            uri = uri,
            fileName = name,
            sizeBytes = size,
            width = max(1, width),
            height = max(1, height),
            mimeType = mimeType
        )
    }

    suspend fun decodeBitmapSafely(uri: Uri, maxDimension: Int = 4096): Bitmap = withContext(Dispatchers.IO) {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, boundsOptions)
        } ?: throw IllegalArgumentException("Cannot open image stream")

        var sampleSize = 1
        val rawWidth = boundsOptions.outWidth
        val rawHeight = boundsOptions.outHeight

        while ((rawWidth / sampleSize) > maxDimension || (rawHeight / sampleSize) > maxDimension) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: throw IllegalStateException("Failed to decode bitmap from URI")
    }

    suspend fun processImage(
        uri: Uri,
        targetWidth: Int,
        targetHeight: Int,
        quality: Int,
        outputFormat: ImageFormat,
        rotationDegrees: Int = 0,
        flipHorizontal: Boolean = false,
        flipVertical: Boolean = false,
        cropRectNormalized: RectF? = null,
        filterMode: ImageFilterMode = ImageFilterMode.ORIGINAL,
        watermarkConfig: WatermarkConfig = WatermarkConfig()
    ): File = withContext(Dispatchers.IO) {
        var bitmap = decodeBitmapSafely(uri)

        // 1. Crop if specified
        if (cropRectNormalized != null) {
            val bmpWidth = bitmap.width
            val bmpHeight = bitmap.height

            val left = (cropRectNormalized.left.coerceIn(0f, 1f) * bmpWidth).toInt().coerceIn(0, bmpWidth - 1)
            val top = (cropRectNormalized.top.coerceIn(0f, 1f) * bmpHeight).toInt().coerceIn(0, bmpHeight - 1)
            val right = (cropRectNormalized.right.coerceIn(0f, 1f) * bmpWidth).toInt().coerceIn(left + 1, bmpWidth)
            val bottom = (cropRectNormalized.bottom.coerceIn(0f, 1f) * bmpHeight).toInt().coerceIn(top + 1, bmpHeight)

            val cropWidth = right - left
            val cropHeight = bottom - top

            if (cropWidth > 0 && cropHeight > 0 && (cropWidth < bmpWidth || cropHeight < bmpHeight)) {
                val cropped = Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
                if (cropped != bitmap) {
                    bitmap.recycle()
                    bitmap = cropped
                }
            }
        }

        // 2. Rotate & Flip
        if (rotationDegrees != 0 || flipHorizontal || flipVertical) {
            val matrix = Matrix()
            if (rotationDegrees != 0) {
                matrix.postRotate(rotationDegrees.toFloat())
            }
            val scaleX = if (flipHorizontal) -1f else 1f
            val scaleY = if (flipVertical) -1f else 1f
            if (scaleX != 1f || scaleY != 1f) {
                matrix.postScale(scaleX, scaleY)
            }
            val transformed = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (transformed != bitmap) {
                bitmap.recycle()
                bitmap = transformed
            }
        }

        // 3. Resize
        val safeTargetW = max(1, targetWidth)
        val safeTargetH = max(1, targetHeight)
        if (bitmap.width != safeTargetW || bitmap.height != safeTargetH) {
            val scaled = Bitmap.createScaledBitmap(bitmap, safeTargetW, safeTargetH, true)
            if (scaled != bitmap) {
                bitmap.recycle()
                bitmap = scaled
            }
        }

        // 4. Color Filter Enhancement
        if (filterMode != ImageFilterMode.ORIGINAL) {
            val filtered = applyColorFilter(bitmap, filterMode)
            if (filtered != bitmap) {
                bitmap.recycle()
                bitmap = filtered
            }
        }

        // 5. Watermark Overlay
        if (watermarkConfig.isEnabled && watermarkConfig.text.isNotBlank()) {
            val watermarked = stampWatermark(bitmap, watermarkConfig)
            if (watermarked != bitmap) {
                bitmap.recycle()
                bitmap = watermarked
            }
        }

        // 6. Compress & Save
        val (originalName, _) = storageManager.getFileNameAndSize(uri)
        val outputFile = storageManager.createOutputFile(originalName, outputFormat.extension)

        FileOutputStream(outputFile).use { outStream ->
            val safeQuality = quality.coerceIn(5, 100)
            bitmap.compress(outputFormat.compressFormat, safeQuality, outStream)
        }
        bitmap.recycle()

        outputFile
    }

    fun applyColorFilter(source: Bitmap, mode: ImageFilterMode): Bitmap {
        if (mode == ImageFilterMode.ORIGINAL) return source
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        val cm = android.graphics.ColorMatrix()

        when (mode) {
            ImageFilterMode.ORIGINAL -> return source
            ImageFilterMode.GRAYSCALE -> {
                cm.setSaturation(0f)
            }
            ImageFilterMode.DOC_SCAN -> {
                cm.setSaturation(0f)
                val contrast = 1.6f
                val brightness = -30f
                val matrix = floatArrayOf(
                    contrast, 0f, 0f, 0f, brightness,
                    0f, contrast, 0f, 0f, brightness,
                    0f, 0f, contrast, 0f, brightness,
                    0f, 0f, 0f, 1f, 0f
                )
                cm.postConcat(android.graphics.ColorMatrix(matrix))
            }
            ImageFilterMode.SEPIA -> {
                cm.set(floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            ImageFilterMode.INVERT -> {
                cm.set(floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
        }

        paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    fun stampWatermark(source: Bitmap, config: WatermarkConfig): Bitmap {
        val mutableBmp = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBmp)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.RED
            alpha = (config.alpha.coerceIn(0.1f, 0.9f) * 255).toInt()
            textSize = (source.width * 0.08f).coerceIn(24f, 160f)
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            setShadowLayer(8f, 2f, 2f, android.graphics.Color.argb(80, 0, 0, 0))
        }

        canvas.save()
        val centerX = source.width / 2f
        val centerY = source.height / 2f

        if (config.isDiagonal) {
            canvas.rotate(-35f, centerX, centerY)
        }
        canvas.drawText(config.text, centerX, centerY, paint)
        canvas.restore()

        return mutableBmp
    }

    suspend fun reduceImageToTargetSize(
        uri: Uri,
        targetSizeBytes: Long,
        outputFormat: ImageFormat = ImageFormat.JPEG,
        onProgress: (Float, String) -> Unit
    ): File = withContext(Dispatchers.IO) {
        onProgress(0.15f, "Analyzing image structure...")
        val originalBitmap = decodeBitmapSafely(uri)
        val (originalName, _) = storageManager.getFileNameAndSize(uri)

        var scale = 1.0f
        var bestBytes: ByteArray? = null
        val safeTarget = max(20 * 1024L, targetSizeBytes)
        val format = if (outputFormat == ImageFormat.PNG) ImageFormat.JPEG else outputFormat

        for (attempt in 0..4) {
            onProgress(0.2f + (attempt * 0.15f), "Optimizing compression (attempt ${attempt + 1}/5)...")
            val testW = max(1, (originalBitmap.width * scale).roundToInt())
            val testH = max(1, (originalBitmap.height * scale).roundToInt())

            val scaled = if (testW != originalBitmap.width || testH != originalBitmap.height) {
                Bitmap.createScaledBitmap(originalBitmap, testW, testH, true)
            } else {
                originalBitmap
            }

            var lowQ = 10
            var highQ = 90
            var chosenBytes: ByteArray? = null

            while (lowQ <= highQ) {
                val midQ = (lowQ + highQ) / 2
                val baos = ByteArrayOutputStream()
                scaled.compress(format.compressFormat, midQ, baos)
                val bytes = baos.toByteArray()

                if (bytes.size <= safeTarget) {
                    chosenBytes = bytes
                    lowQ = midQ + 1
                } else {
                    highQ = midQ - 1
                }
            }

            if (scaled != originalBitmap) scaled.recycle()

            if (chosenBytes != null && chosenBytes.size <= safeTarget) {
                bestBytes = chosenBytes
                break
            } else {
                scale *= 0.75f
            }
        }

        if (bestBytes == null) {
            val scaled = Bitmap.createScaledBitmap(
                originalBitmap,
                max(1, (originalBitmap.width * 0.5f).roundToInt()),
                max(1, (originalBitmap.height * 0.5f).roundToInt()),
                true
            )
            val baos = ByteArrayOutputStream()
            scaled.compress(format.compressFormat, 25, baos)
            bestBytes = baos.toByteArray()
            if (scaled != originalBitmap) scaled.recycle()
        }

        originalBitmap.recycle()

        onProgress(0.95f, "Saving reduced image file...")
        val outputFile = storageManager.createOutputFile("${originalName}_reduced", format.extension)
        FileOutputStream(outputFile).use { out ->
            out.write(bestBytes)
        }
        onProgress(1.0f, "Completed")
        outputFile
    }


    suspend fun estimateSize(
        uri: Uri,
        targetWidth: Int,
        targetHeight: Int,
        quality: Int,
        outputFormat: ImageFormat
    ): Long = withContext(Dispatchers.IO) {
        try {
            // For fast preview, decode at reasonable bounded size
            val sampleMax = 1200
            val bitmap = decodeBitmapSafely(uri, maxDimension = sampleMax)
            val scale = min(1f, min(targetWidth.toFloat() / bitmap.width, targetHeight.toFloat() / bitmap.height))
            val scaledW = max(1, (bitmap.width * scale).roundToInt())
            val scaledH = max(1, (bitmap.height * scale).roundToInt())

            val scaled = if (scaledW != bitmap.width || scaledH != bitmap.height) {
                Bitmap.createScaledBitmap(bitmap, scaledW, scaledH, true)
            } else {
                bitmap
            }

            val baos = ByteArrayOutputStream()
            scaled.compress(outputFormat.compressFormat, quality.coerceIn(5, 100), baos)
            val sampleBytes = baos.size().toLong()

            if (scaled != bitmap) scaled.recycle()
            bitmap.recycle()

            // Scale sample byte size to estimated full dimensions ratio
            val pixelRatio = (targetWidth.toLong() * targetHeight) / (scaledW.toLong() * scaledH).coerceAtLeast(1)
            val estimated = (sampleBytes * Math.sqrt(pixelRatio.toDouble())).toLong()
            max(sampleBytes, estimated)
        } catch (_: Exception) {
            0L
        }
    }

    suspend fun convertImagesToPdf(
        imageUris: List<Uri>,
        pageSize: PdfPageSize,
        marginDp: Int = 16,
        quality: Int = 85,
        onProgress: (Float, String) -> Unit
    ): File = withContext(Dispatchers.IO) {
        if (imageUris.isEmpty()) throw IllegalArgumentException("No images provided for PDF conversion")

        val pdfDoc = PdfDocument()
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        val total = imageUris.size

        try {
            for (i in imageUris.indices) {
                val uri = imageUris[i]
                onProgress((i.toFloat() / total), "Processing image ${i + 1} of $total...")

                val bitmap = decodeBitmapSafely(uri, maxDimension = 2400)

                val pageW: Int
                val pageH: Int
                if (pageSize == PdfPageSize.FIT_IMAGE) {
                    pageW = bitmap.width
                    pageH = bitmap.height
                } else {
                    pageW = pageSize.widthPoints
                    pageH = pageSize.heightPoints
                }

                val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, i + 1).create()
                val page = pdfDoc.startPage(pageInfo)
                val canvas: Canvas = page.canvas

                // Fill page background with white
                canvas.drawColor(android.graphics.Color.WHITE)

                val margin = if (pageSize == PdfPageSize.FIT_IMAGE) 0f else marginDp.toFloat()
                val availW = pageW - (margin * 2)
                val availH = pageH - (margin * 2)

                val scale = min(availW / bitmap.width.toFloat(), availH / bitmap.height.toFloat())
                val destW = bitmap.width * scale
                val destH = bitmap.height * scale

                val left = margin + (availW - destW) / 2f
                val top = margin + (availH - destH) / 2f

                val destRect = RectF(left, top, left + destW, top + destH)
                canvas.drawBitmap(bitmap, null, destRect, paint)

                pdfDoc.finishPage(page)
                bitmap.recycle()
            }

            onProgress(0.95f, "Finalizing PDF file...")
            val outputFile = storageManager.createOutputFile("docucraft_images_to_pdf", "pdf")
            FileOutputStream(outputFile).use { out ->
                pdfDoc.writeTo(out)
            }
            onProgress(1.0f, "Completed")
            outputFile
        } finally {
            pdfDoc.close()
        }
    }
}
