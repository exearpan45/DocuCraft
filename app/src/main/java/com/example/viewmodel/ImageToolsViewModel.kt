package com.example.viewmodel

import android.app.Application
import android.graphics.RectF
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.FileStorageManager
import com.example.domain.ImageProcessor
import com.example.model.CropAspectRatio
import com.example.model.ImageDetails
import com.example.model.ImageFormat
import com.example.model.OutputFileType
import com.example.model.UiProcessingState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

import com.example.model.TargetSizePreset
import com.example.model.formatFileSize

data class ImageToolsUiState(
    val selectedImageUri: Uri? = null,
    val imageDetails: ImageDetails? = null,
    val isLoadingDetails: Boolean = false,
    
    // Quick Size Reducer Settings
    val targetSizePreset: TargetSizePreset = TargetSizePreset.UNDER_200_KB,
    val customTargetKb: Int = 200,

    // Resize settings
    val preserveAspectRatio: Boolean = true,
    val targetWidth: Int = 0,
    val targetHeight: Int = 0,
    val selectedScalePercent: Int = 100, // 25, 50, 75, 100, or 0 (custom)
    
    // Compression & Format settings
    val compressionQuality: Int = 80,
    val outputFormat: ImageFormat = ImageFormat.JPEG,
    val estimatedSizeBytes: Long = 0L,
    val isEstimatingSize: Boolean = false,
    
    // Crop & Rotate settings
    val rotationDegrees: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val selectedCropAspect: CropAspectRatio = CropAspectRatio.FREE,
    val cropRect: RectF = RectF(0f, 0f, 1f, 1f),
    
    // Color Filter and Watermark settings
    val filterMode: com.example.model.ImageFilterMode = com.example.model.ImageFilterMode.ORIGINAL,
    val watermarkConfig: com.example.model.WatermarkConfig = com.example.model.WatermarkConfig(),

    // General processing state
    val processingState: UiProcessingState = UiProcessingState.Idle
)

class ImageToolsViewModel(application: Application) : AndroidViewModel(application) {

    val storageManager = FileStorageManager(application)
    val imageProcessor = ImageProcessor(application, storageManager)

    private val _uiState = MutableStateFlow(ImageToolsUiState())
    val uiState: StateFlow<ImageToolsUiState> = _uiState.asStateFlow()

    private var activeJob: Job? = null
    private var estimateJob: Job? = null

    fun selectImage(uri: Uri) {
        _uiState.update { it.copy(selectedImageUri = uri, isLoadingDetails = true) }
        viewModelScope.launch {
            try {
                val details = imageProcessor.getImageDetails(uri)
                val defaultFormat = when {
                    details.mimeType.contains("png", true) -> ImageFormat.PNG
                    details.mimeType.contains("webp", true) -> ImageFormat.WEBP
                    else -> ImageFormat.JPEG
                }
                _uiState.update {
                    it.copy(
                        imageDetails = details,
                        isLoadingDetails = false,
                        targetWidth = details.width,
                        targetHeight = details.height,
                        selectedScalePercent = 100,
                        outputFormat = defaultFormat,
                        rotationDegrees = 0,
                        flipHorizontal = false,
                        flipVertical = false,
                        cropRect = RectF(0f, 0f, 1f, 1f)
                    )
                }
                triggerEstimate()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingDetails = false,
                        processingState = UiProcessingState.Error(
                            title = "Cannot Open Image",
                            message = e.localizedMessage ?: "Failed to read the selected image."
                        )
                    )
                }
            }
        }
    }

    fun setScalePercent(percent: Int) {
        val details = _uiState.value.imageDetails ?: return
        val w = max(1, (details.width * (percent / 100f)).roundToInt())
        val h = max(1, (details.height * (percent / 100f)).roundToInt())
        _uiState.update {
            it.copy(
                selectedScalePercent = percent,
                targetWidth = w,
                targetHeight = h
            )
        }
        triggerEstimate()
    }

    fun setTargetDimensions(width: Int, height: Int) {
        _uiState.update {
            it.copy(
                targetWidth = width,
                targetHeight = height,
                selectedScalePercent = 0 // Custom
            )
        }
        triggerEstimate()
    }

    fun onWidthChanged(newWidth: Int) {
        val details = _uiState.value.imageDetails ?: return
        if (newWidth <= 0) return
        val newHeight = if (_uiState.value.preserveAspectRatio && details.width > 0) {
            max(1, (newWidth * (details.height.toFloat() / details.width)).roundToInt())
        } else {
            _uiState.value.targetHeight
        }
        setTargetDimensions(newWidth, newHeight)
    }

    fun onHeightChanged(newHeight: Int) {
        val details = _uiState.value.imageDetails ?: return
        if (newHeight <= 0) return
        val newWidth = if (_uiState.value.preserveAspectRatio && details.height > 0) {
            max(1, (newHeight * (details.width.toFloat() / details.height)).roundToInt())
        } else {
            _uiState.value.targetWidth
        }
        setTargetDimensions(newWidth, newHeight)
    }

    fun togglePreserveAspectRatio() {
        _uiState.update { it.copy(preserveAspectRatio = !it.preserveAspectRatio) }
    }

    fun setCompressionQuality(quality: Int) {
        _uiState.update { it.copy(compressionQuality = quality) }
        triggerEstimate()
    }

    fun setOutputFormat(format: ImageFormat) {
        _uiState.update { it.copy(outputFormat = format) }
        triggerEstimate()
    }

    fun rotateClockwise() {
        _uiState.update { it.copy(rotationDegrees = (it.rotationDegrees + 90) % 360) }
    }

    fun rotateCounterClockwise() {
        _uiState.update { it.copy(rotationDegrees = (it.rotationDegrees - 90 + 360) % 360) }
    }

    fun toggleFlipHorizontal() {
        _uiState.update { it.copy(flipHorizontal = !it.flipHorizontal) }
    }

    fun toggleFlipVertical() {
        _uiState.update { it.copy(flipVertical = !it.flipVertical) }
    }

    fun setCropAspect(aspect: CropAspectRatio) {
        _uiState.update { it.copy(selectedCropAspect = aspect) }
    }

    fun updateCropRect(rect: RectF) {
        _uiState.update { it.copy(cropRect = rect) }
    }

    fun setFilterMode(mode: com.example.model.ImageFilterMode) {
        _uiState.update { it.copy(filterMode = mode) }
    }

    fun setWatermarkConfig(config: com.example.model.WatermarkConfig) {
        _uiState.update { it.copy(watermarkConfig = config) }
    }

    private fun triggerEstimate() {
        val state = _uiState.value
        val uri = state.selectedImageUri ?: return
        val w = state.targetWidth
        val h = state.targetHeight
        val q = state.compressionQuality
        val fmt = state.outputFormat

        estimateJob?.cancel()
        estimateJob = viewModelScope.launch {
            _uiState.update { it.copy(isEstimatingSize = true) }
            val est = imageProcessor.estimateSize(uri, w, h, q, fmt)
            _uiState.update { it.copy(estimatedSizeBytes = est, isEstimatingSize = false) }
        }
    }

    fun processImage() {
        val state = _uiState.value
        val uri = state.selectedImageUri ?: return
        val details = state.imageDetails ?: return

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    processingState = UiProcessingState.Processing(
                        progress = 0.3f,
                        message = "Transforming and compressing image..."
                    )
                )
            }

            try {
                val hasCrop = state.cropRect.left > 0.01f || state.cropRect.top > 0.01f ||
                        state.cropRect.right < 0.99f || state.cropRect.bottom < 0.99f

                val outputFile = imageProcessor.processImage(
                    uri = uri,
                    targetWidth = state.targetWidth,
                    targetHeight = state.targetHeight,
                    quality = state.compressionQuality,
                    outputFormat = state.outputFormat,
                    rotationDegrees = state.rotationDegrees,
                    flipHorizontal = state.flipHorizontal,
                    flipVertical = state.flipVertical,
                    cropRectNormalized = if (hasCrop) state.cropRect else null,
                    filterMode = state.filterMode,
                    watermarkConfig = state.watermarkConfig
                )

                _uiState.update {
                    it.copy(
                        processingState = UiProcessingState.Success(
                            resultFile = outputFile,
                            title = "Image Saved Successfully",
                            message = "Processed into ${state.outputFormat.displayName} with dimensions ${state.targetWidth}×${state.targetHeight}.",
                            originalSize = details.sizeBytes,
                            newSize = outputFile.length(),
                            fileType = OutputFileType.IMAGE
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        processingState = UiProcessingState.Error(
                            title = "Image Processing Error",
                            message = e.localizedMessage ?: "An error occurred while processing the image."
                        )
                    )
                }
            }
        }
    }

    fun setTargetSizePreset(preset: TargetSizePreset) {
        _uiState.update { it.copy(targetSizePreset = preset) }
    }

    fun setCustomTargetKb(kb: Int) {
        _uiState.update { it.copy(customTargetKb = kb, targetSizePreset = TargetSizePreset.CUSTOM) }
    }

    fun executeReduceImageSize() {
        val state = _uiState.value
        val uri = state.selectedImageUri ?: return
        val details = state.imageDetails ?: return

        val targetBytes = if (state.targetSizePreset == TargetSizePreset.CUSTOM) {
            max(20, state.customTargetKb) * 1024L
        } else {
            state.targetSizePreset.targetBytes
        }

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    processingState = UiProcessingState.Processing(
                        progress = 0.2f,
                        message = "Reducing image size to target..."
                    )
                )
            }

            try {
                val outputFile = imageProcessor.reduceImageToTargetSize(
                    uri = uri,
                    targetSizeBytes = targetBytes,
                    outputFormat = state.outputFormat
                ) { progress, message ->
                    _uiState.update {
                        it.copy(processingState = UiProcessingState.Processing(progress, message))
                    }
                }

                _uiState.update {
                    it.copy(
                        processingState = UiProcessingState.Success(
                            resultFile = outputFile,
                            title = "Image Size Reduced!",
                            message = "Successfully reduced to under ${formatFileSize(targetBytes)}.",
                            originalSize = details.sizeBytes,
                            newSize = outputFile.length(),
                            fileType = OutputFileType.IMAGE
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        processingState = UiProcessingState.Error(
                            title = "Reduction Failed",
                            message = e.localizedMessage ?: "Could not reduce image file size."
                        )
                    )
                }
            }
        }
    }

    fun cancelActiveJob() {
        activeJob?.cancel()
        _uiState.update { it.copy(processingState = UiProcessingState.Idle) }
    }

    fun dismissProcessingState() {
        _uiState.update { it.copy(processingState = UiProcessingState.Idle) }
    }

    fun exportToDestination(resultFile: File, destinationUri: Uri): Boolean {
        return storageManager.saveToDestinationUri(resultFile, destinationUri)
    }

    fun clearSelection() {
        _uiState.update { ImageToolsUiState() }
    }
}
