package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.FileStorageManager
import com.example.domain.ImageProcessor
import com.example.domain.PdfCompressionLevel
import com.example.domain.PdfProcessor
import com.example.model.OutputFileType
import com.example.model.PdfDetails
import com.example.model.PdfPageItem
import com.example.model.PdfPageSize
import com.example.model.UiProcessingState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.max
import kotlin.math.min

enum class PdfToolTab {
    MERGE,
    SPLIT,
    REORDER,
    COMPRESS,
    IMAGES_TO_PDF,
    ROTATE_PAGES,
    WATERMARK_PDF
}

data class PdfMergeItem(
    val uri: Uri,
    val details: PdfDetails
)

data class PdfToolsUiState(
    val currentTab: PdfToolTab = PdfToolTab.MERGE,
    
    // Merge State
    val mergePdfs: List<PdfMergeItem> = emptyList(),
    val isInspectingMergeFile: Boolean = false,
    
    // Single PDF inspection (Split / Reorder / Compress)
    val selectedPdfUri: Uri? = null,
    val pdfDetails: PdfDetails? = null,
    val isInspectingPdf: Boolean = false,
    val pdfPages: List<PdfPageItem> = emptyList(),
    val isLoadingPages: Boolean = false,
    
    // Split specific state
    val splitRangeText: String = "",
    
    // Compress specific state
    val compressionLevel: PdfCompressionLevel = PdfCompressionLevel.BALANCED,
    
    // Images to PDF state
    val selectedImageUris: List<Uri> = emptyList(),
    val imagesToPdfPageSize: PdfPageSize = PdfPageSize.A4,
    val imagesToPdfQuality: Int = 85,
    val imagesToPdfMarginDp: Int = 16,
    
    // General Processing State
    val processingState: UiProcessingState = UiProcessingState.Idle
)

class PdfToolsViewModel(application: Application) : AndroidViewModel(application) {

    val storageManager = FileStorageManager(application)
    val pdfProcessor = PdfProcessor(application, storageManager)
    val imageProcessor = ImageProcessor(application, storageManager)

    private val _uiState = MutableStateFlow(PdfToolsUiState())
    val uiState: StateFlow<PdfToolsUiState> = _uiState.asStateFlow()

    private var activeJob: Job? = null

    fun selectTab(tab: PdfToolTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    // --- Merge PDF Methods ---

    fun addPdfsForMerge(uris: List<Uri>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isInspectingMergeFile = true) }
            val currentList = _uiState.value.mergePdfs.toMutableList()
            for (uri in uris) {
                // Avoid duplicate identical URIs
                if (currentList.none { it.uri == uri }) {
                    val details = pdfProcessor.getPdfDetails(uri)
                    if (details.isEncrypted) {
                        _uiState.update {
                            it.copy(
                                processingState = UiProcessingState.Error(
                                    title = "Password Protected PDF",
                                    message = "File '${details.fileName}' is password-encrypted and cannot be merged."
                                )
                            )
                        }
                    } else if (details.isCorrupted || details.pageCount == 0) {
                        _uiState.update {
                            it.copy(
                                processingState = UiProcessingState.Error(
                                    title = "Corrupted PDF",
                                    message = "File '${details.fileName}' is corrupted or empty and cannot be merged."
                                )
                            )
                        }
                    } else {
                        currentList.add(PdfMergeItem(uri, details))
                    }
                }
            }
            _uiState.update { it.copy(mergePdfs = currentList, isInspectingMergeFile = false) }
        }
    }

    fun removeMergeItem(index: Int) {
        val currentList = _uiState.value.mergePdfs.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _uiState.update { it.copy(mergePdfs = currentList) }
        }
    }

    fun moveMergeItemUp(index: Int) {
        if (index <= 0) return
        val currentList = _uiState.value.mergePdfs.toMutableList()
        val item = currentList.removeAt(index)
        currentList.add(index - 1, item)
        _uiState.update { it.copy(mergePdfs = currentList) }
    }

    fun moveMergeItemDown(index: Int) {
        val currentList = _uiState.value.mergePdfs.toMutableList()
        if (index >= currentList.size - 1) return
        val item = currentList.removeAt(index)
        currentList.add(index + 1, item)
        _uiState.update { it.copy(mergePdfs = currentList) }
    }

    fun executeMerge() {
        val items = _uiState.value.mergePdfs
        if (items.size < 2) {
            _uiState.update {
                it.copy(
                    processingState = UiProcessingState.Error(
                        title = "Merge Error",
                        message = "Please add at least 2 PDF files to merge."
                    )
                )
            }
            return
        }

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _uiState.update {
                it.copy(processingState = UiProcessingState.Processing(0.1f, "Merging PDF documents..."))
            }

            try {
                val uris = items.map { it.uri }
                val totalOriginalSize = items.sumOf { it.details.sizeBytes }
                val outputFile = pdfProcessor.mergePdfs(uris) { progress, message ->
                    _uiState.update {
                        it.copy(processingState = UiProcessingState.Processing(progress, message))
                    }
                }

                _uiState.update {
                    it.copy(
                        processingState = UiProcessingState.Success(
                            resultFile = outputFile,
                            title = "PDFs Merged Successfully",
                            message = "Merged ${items.size} documents (${items.sumOf { it.details.pageCount }} pages total).",
                            originalSize = totalOriginalSize,
                            newSize = outputFile.length(),
                            fileType = OutputFileType.PDF
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        processingState = UiProcessingState.Error(
                            title = "Merge Failed",
                            message = e.localizedMessage ?: "Failed to merge selected PDFs."
                        )
                    )
                }
            }
        }
    }

    // --- Single PDF Inspection (Split, Reorder, Compress) ---

    fun selectSinglePdf(uri: Uri) {
        _uiState.update {
            it.copy(
                selectedPdfUri = uri,
                isInspectingPdf = true,
                pdfPages = emptyList(),
                splitRangeText = ""
            )
        }

        viewModelScope.launch {
            try {
                val details = pdfProcessor.getPdfDetails(uri)
                if (details.isEncrypted) {
                    _uiState.update {
                        it.copy(
                            isInspectingPdf = false,
                            processingState = UiProcessingState.Error(
                                title = "Encrypted Document",
                                message = "This PDF file is password protected and cannot be modified."
                            )
                        )
                    }
                    return@launch
                }
                if (details.isCorrupted || details.pageCount == 0) {
                    _uiState.update {
                        it.copy(
                            isInspectingPdf = false,
                            processingState = UiProcessingState.Error(
                                title = "Corrupted Document",
                                message = "This PDF file cannot be read or contains no valid pages."
                            )
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        pdfDetails = details,
                        isInspectingPdf = false,
                        isLoadingPages = true,
                        splitRangeText = "1-${details.pageCount}"
                    )
                }

                // Load page thumbnails
                val pages = pdfProcessor.loadPageThumbnails(uri)
                _uiState.update { it.copy(pdfPages = pages, isLoadingPages = false) }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isInspectingPdf = false,
                        isLoadingPages = false,
                        processingState = UiProcessingState.Error(
                            title = "Error Opening PDF",
                            message = e.localizedMessage ?: "Unable to inspect PDF."
                        )
                    )
                }
            }
        }
    }

    fun togglePageSelection(pageIndex: Int) {
        val currentPages = _uiState.value.pdfPages.map {
            if (it.originalPageIndex == pageIndex) it.copy(isSelected = !it.isSelected) else it
        }
        _uiState.update { it.copy(pdfPages = currentPages) }
    }

    fun setSplitRangeText(text: String) {
        _uiState.update { it.copy(splitRangeText = text) }
    }

    fun applyRangeSelection(rangeStr: String) {
        val details = _uiState.value.pdfDetails ?: return
        val maxPages = details.pageCount
        val selectedIndices = parsePageRange(rangeStr, maxPages)

        val updatedPages = _uiState.value.pdfPages.map { page ->
            page.copy(isSelected = selectedIndices.contains(page.originalPageIndex))
        }
        _uiState.update { it.copy(pdfPages = updatedPages, splitRangeText = rangeStr) }
    }

    fun executeSplit() {
        val uri = _uiState.value.selectedPdfUri ?: return
        val details = _uiState.value.pdfDetails ?: return
        val selectedPages = _uiState.value.pdfPages.filter { it.isSelected }.map { it.originalPageIndex }

        if (selectedPages.isEmpty()) {
            _uiState.update {
                it.copy(
                    processingState = UiProcessingState.Error(
                        title = "No Pages Selected",
                        message = "Please select at least one page to extract."
                    )
                )
            }
            return
        }

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _uiState.update {
                it.copy(processingState = UiProcessingState.Processing(0.1f, "Extracting ${selectedPages.size} pages..."))
            }

            try {
                val outputFile = pdfProcessor.splitPdf(uri, selectedPages) { progress, message ->
                    _uiState.update {
                        it.copy(processingState = UiProcessingState.Processing(progress, message))
                    }
                }

                _uiState.update {
                    it.copy(
                        processingState = UiProcessingState.Success(
                            resultFile = outputFile,
                            title = "Pages Extracted Successfully",
                            message = "Created new PDF with ${selectedPages.size} pages.",
                            originalSize = details.sizeBytes,
                            newSize = outputFile.length(),
                            fileType = OutputFileType.PDF
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        processingState = UiProcessingState.Error(
                            title = "Extraction Failed",
                            message = e.localizedMessage ?: "Failed to extract pages."
                        )
                    )
                }
            }
        }
    }

    // --- Reorder & Remove Pages ---

    fun movePageLeft(index: Int) {
        if (index <= 0) return
        val pages = _uiState.value.pdfPages.toMutableList()
        val item = pages.removeAt(index)
        pages.add(index - 1, item)
        _uiState.update { it.copy(pdfPages = pages) }
    }

    fun movePageRight(index: Int) {
        val pages = _uiState.value.pdfPages.toMutableList()
        if (index >= pages.size - 1) return
        val item = pages.removeAt(index)
        pages.add(index + 1, item)
        _uiState.update { it.copy(pdfPages = pages) }
    }

    fun deletePage(index: Int) {
        val pages = _uiState.value.pdfPages.toMutableList()
        if (pages.size <= 1) {
            _uiState.update {
                it.copy(
                    processingState = UiProcessingState.Error(
                        title = "Cannot Delete Last Page",
                        message = "A PDF must contain at least one page."
                    )
                )
            }
            return
        }
        if (index in pages.indices) {
            pages.removeAt(index)
            _uiState.update { it.copy(pdfPages = pages) }
        }
    }

    fun executeReorderAndSave() {
        val uri = _uiState.value.selectedPdfUri ?: return
        val details = _uiState.value.pdfDetails ?: return
        val pageIndices = _uiState.value.pdfPages.map { it.originalPageIndex }

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _uiState.update {
                it.copy(processingState = UiProcessingState.Processing(0.1f, "Assembling reordered PDF..."))
            }

            try {
                val outputFile = pdfProcessor.reorderAndRemovePages(uri, pageIndices) { progress, message ->
                    _uiState.update {
                        it.copy(processingState = UiProcessingState.Processing(progress, message))
                    }
                }

                _uiState.update {
                    it.copy(
                        processingState = UiProcessingState.Success(
                            resultFile = outputFile,
                            title = "PDF Reordered Successfully",
                            message = "Generated new document with ${pageIndices.size} pages in custom order.",
                            originalSize = details.sizeBytes,
                            newSize = outputFile.length(),
                            fileType = OutputFileType.PDF
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        processingState = UiProcessingState.Error(
                            title = "Reorder Failed",
                            message = e.localizedMessage ?: "Failed to reorder and save PDF."
                        )
                    )
                }
            }
        }
    }

    // --- Compress PDF ---

    fun setCompressionLevel(level: PdfCompressionLevel) {
        _uiState.update { it.copy(compressionLevel = level) }
    }

    fun executeCompressPdf() {
        val uri = _uiState.value.selectedPdfUri ?: return
        val details = _uiState.value.pdfDetails ?: return
        val level = _uiState.value.compressionLevel

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _uiState.update {
                it.copy(processingState = UiProcessingState.Processing(0.1f, "Compressing PDF pages..."))
            }

            try {
                val outputFile = pdfProcessor.compressPdf(uri, level) { progress, message ->
                    _uiState.update {
                        it.copy(processingState = UiProcessingState.Processing(progress, message))
                    }
                }

                _uiState.update {
                    it.copy(
                        processingState = UiProcessingState.Success(
                            resultFile = outputFile,
                            title = "PDF Compressed Successfully",
                            message = "Processed with ${level.title}.",
                            originalSize = details.sizeBytes,
                            newSize = outputFile.length(),
                            fileType = OutputFileType.PDF
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        processingState = UiProcessingState.Error(
                            title = "Compression Failed",
                            message = e.localizedMessage ?: "Failed to compress PDF."
                        )
                    )
                }
            }
        }
    }

    // --- Images to PDF ---

    fun addImagesForPdf(uris: List<Uri>) {
        val current = _uiState.value.selectedImageUris.toMutableList()
        for (u in uris) {
            if (!current.contains(u)) current.add(u)
        }
        _uiState.update { it.copy(selectedImageUris = current) }
    }

    fun removeImageFromPdf(index: Int) {
        val current = _uiState.value.selectedImageUris.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _uiState.update { it.copy(selectedImageUris = current) }
        }
    }

    fun moveImageUp(index: Int) {
        if (index <= 0) return
        val current = _uiState.value.selectedImageUris.toMutableList()
        val item = current.removeAt(index)
        current.add(index - 1, item)
        _uiState.update { it.copy(selectedImageUris = current) }
    }

    fun moveImageDown(index: Int) {
        val current = _uiState.value.selectedImageUris.toMutableList()
        if (index >= current.size - 1) return
        val item = current.removeAt(index)
        current.add(index + 1, item)
        _uiState.update { it.copy(selectedImageUris = current) }
    }

    fun setImagesToPdfPageSize(pageSize: PdfPageSize) {
        _uiState.update { it.copy(imagesToPdfPageSize = pageSize) }
    }

    fun executeConvertImagesToPdf() {
        val uris = _uiState.value.selectedImageUris
        if (uris.isEmpty()) {
            _uiState.update {
                it.copy(
                    processingState = UiProcessingState.Error(
                        title = "No Images Selected",
                        message = "Please pick one or more images to build a PDF document."
                    )
                )
            }
            return
        }

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _uiState.update {
                it.copy(processingState = UiProcessingState.Processing(0.1f, "Converting images to PDF..."))
            }

            try {
                val totalOriginalSize = uris.sumOf { storageManager.getFileNameAndSize(it).second }
                val outputFile = imageProcessor.convertImagesToPdf(
                    imageUris = uris,
                    pageSize = _uiState.value.imagesToPdfPageSize,
                    marginDp = _uiState.value.imagesToPdfMarginDp,
                    quality = _uiState.value.imagesToPdfQuality
                ) { progress, message ->
                    _uiState.update {
                        it.copy(processingState = UiProcessingState.Processing(progress, message))
                    }
                }

                _uiState.update {
                    it.copy(
                        processingState = UiProcessingState.Success(
                            resultFile = outputFile,
                            title = "Images Converted to PDF",
                            message = "Created PDF document with ${uris.size} pages.",
                            originalSize = totalOriginalSize,
                            newSize = outputFile.length(),
                            fileType = OutputFileType.PDF
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        processingState = UiProcessingState.Error(
                            title = "Conversion Failed",
                            message = e.localizedMessage ?: "Failed to convert images to PDF."
                        )
                    )
                }
            }
        }
    }

    fun executeRotatePdf(degrees: Int = 90) {
        val uri = _uiState.value.selectedPdfUri ?: return
        val details = _uiState.value.pdfDetails ?: return

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _uiState.update {
                it.copy(processingState = UiProcessingState.Processing(0.1f, "Rotating PDF pages by ${degrees}°..."))
            }

            try {
                val outputFile = pdfProcessor.rotatePdfPages(uri, degrees) { progress, message ->
                    _uiState.update {
                        it.copy(processingState = UiProcessingState.Processing(progress, message))
                    }
                }

                _uiState.update {
                    it.copy(
                        processingState = UiProcessingState.Success(
                            resultFile = outputFile,
                            title = "PDF Rotated Successfully",
                            message = "Rotated all pages by ${degrees}° clockwise.",
                            originalSize = details.sizeBytes,
                            newSize = outputFile.length(),
                            fileType = OutputFileType.PDF
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        processingState = UiProcessingState.Error(
                            title = "Rotation Failed",
                            message = e.localizedMessage ?: "Failed to rotate PDF."
                        )
                    )
                }
            }
        }
    }

    fun executeWatermarkPdf(watermarkText: String) {
        val uri = _uiState.value.selectedPdfUri ?: return
        val details = _uiState.value.pdfDetails ?: return
        val text = watermarkText.ifBlank { "CONFIDENTIAL" }

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _uiState.update {
                it.copy(processingState = UiProcessingState.Processing(0.1f, "Applying watermark to PDF..."))
            }

            try {
                val outputFile = pdfProcessor.watermarkPdf(uri, text) { progress, message ->
                    _uiState.update {
                        it.copy(processingState = UiProcessingState.Processing(progress, message))
                    }
                }

                _uiState.update {
                    it.copy(
                        processingState = UiProcessingState.Success(
                            resultFile = outputFile,
                            title = "PDF Watermarked!",
                            message = "Stamped '$text' across all pages.",
                            originalSize = details.sizeBytes,
                            newSize = outputFile.length(),
                            fileType = OutputFileType.PDF
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        processingState = UiProcessingState.Error(
                            title = "Watermark Failed",
                            message = e.localizedMessage ?: "Failed to watermark PDF."
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

    fun clearPdfSelection() {
        _uiState.update {
            it.copy(
                selectedPdfUri = null,
                pdfDetails = null,
                pdfPages = emptyList(),
                splitRangeText = ""
            )
        }
    }

    fun clearAll() {
        _uiState.update { PdfToolsUiState(currentTab = it.currentTab) }
    }
}

/**
 * Parses user input page ranges like "1-3, 5, 7-9" into 0-based set of integer indices.
 */
fun parsePageRange(input: String, totalPages: Int): Set<Int> {
    val result = mutableSetOf<Int>()
    val tokens = input.split(",", ";", " ").filter { it.isNotBlank() }

    for (token in tokens) {
        val trimmed = token.trim()
        if (trimmed.contains("-")) {
            val parts = trimmed.split("-").map { it.trim().toIntOrNull() }
            if (parts.size == 2 && parts[0] != null && parts[1] != null) {
                val start = parts[0]!!
                val end = parts[1]!!
                val minPage = max(1, min(start, end))
                val maxPage = min(totalPages, max(start, end))
                for (p in minPage..maxPage) {
                    result.add(p - 1) // convert 1-based to 0-based
                }
            }
        } else {
            val pageNum = trimmed.toIntOrNull()
            if (pageNum != null && pageNum in 1..totalPages) {
                result.add(pageNum - 1)
            }
        }
    }
    return result
}
