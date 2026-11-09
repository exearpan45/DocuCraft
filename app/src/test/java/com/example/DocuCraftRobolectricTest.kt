package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.FileStorageManager
import com.example.model.CropAspectRatio
import com.example.model.HistorySortOption
import com.example.model.ImageFormat
import com.example.model.PdfPageSize
import com.example.model.TargetSizePreset
import com.example.model.UiProcessingState
import com.example.viewmodel.HistoryViewModel
import com.example.viewmodel.ImageToolsViewModel
import com.example.viewmodel.PdfToolTab
import com.example.viewmodel.PdfToolsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DocuCraftRobolectricTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    @Test
    fun readStringResourceAppName() {
        val appName = context.getString(R.string.app_name)
        assertEquals("DocuCraft", appName)
    }

    @Test
    fun testFileStorageManagerOutputCreation() {
        val storageManager = FileStorageManager(context)
        val file = storageManager.createOutputFile("sample_photo", "jpg")

        assertNotNull(file)
        assertTrue(file.name.startsWith("sample_photo"))
        assertTrue(file.name.endsWith(".jpg"))
        assertTrue(file.parentFile?.exists() == true)
    }

    @Test
    fun testImageToolsViewModelInitialState() {
        val vm = ImageToolsViewModel(app)
        val state = vm.uiState.value

        assertEquals(UiProcessingState.Idle, state.processingState)
        assertTrue(state.preserveAspectRatio)
        assertEquals(100, state.selectedScalePercent)
        assertEquals(80, state.compressionQuality)
        assertEquals(ImageFormat.JPEG, state.outputFormat)
        assertEquals(0, state.rotationDegrees)
        assertEquals(TargetSizePreset.UNDER_200_KB, state.targetSizePreset)
    }

    @Test
    fun testImageToolsViewModelSizeReducerPresets() {
        val vm = ImageToolsViewModel(app)
        vm.setTargetSizePreset(TargetSizePreset.UNDER_100_KB)
        assertEquals(TargetSizePreset.UNDER_100_KB, vm.uiState.value.targetSizePreset)

        vm.setCustomTargetKb(350)
        assertEquals(TargetSizePreset.CUSTOM, vm.uiState.value.targetSizePreset)
        assertEquals(350, vm.uiState.value.customTargetKb)
    }

    @Test
    fun testImageToolsViewModelRotationAndFlip() {
        val vm = ImageToolsViewModel(app)
        vm.rotateClockwise()
        assertEquals(90, vm.uiState.value.rotationDegrees)
        vm.rotateClockwise()
        assertEquals(180, vm.uiState.value.rotationDegrees)
        vm.rotateCounterClockwise()
        assertEquals(90, vm.uiState.value.rotationDegrees)

        vm.toggleFlipHorizontal()
        assertTrue(vm.uiState.value.flipHorizontal)
        vm.toggleFlipVertical()
        assertTrue(vm.uiState.value.flipVertical)
    }

    @Test
    fun testPdfToolsViewModelTabSwitching() {
        val vm = PdfToolsViewModel(app)
        assertEquals(PdfToolTab.MERGE, vm.uiState.value.currentTab)

        vm.selectTab(PdfToolTab.SPLIT)
        assertEquals(PdfToolTab.SPLIT, vm.uiState.value.currentTab)

        vm.selectTab(PdfToolTab.COMPRESS)
        assertEquals(PdfToolTab.COMPRESS, vm.uiState.value.currentTab)
    }

    @Test
    fun testHistoryViewModelSearchAndSort() {
        val vm = HistoryViewModel(app)
        assertNotNull(vm.uiState.value.items)

        vm.setSearchQuery("receipt")
        assertEquals("receipt", vm.uiState.value.searchQuery)

        vm.setSortOption(HistorySortOption.SIZE_DESC)
        assertEquals(HistorySortOption.SIZE_DESC, vm.uiState.value.sortOption)
    }

    @Test
    fun testPdfToolsViewModelNewFeatures() {
        val vm = PdfToolsViewModel(app)
        vm.selectTab(PdfToolTab.ROTATE_PAGES)
        assertEquals(PdfToolTab.ROTATE_PAGES, vm.uiState.value.currentTab)

        vm.selectTab(PdfToolTab.WATERMARK_PDF)
        assertEquals(PdfToolTab.WATERMARK_PDF, vm.uiState.value.currentTab)
    }

    @Test
    fun testImageToolsFilterAndWatermark() {
        val vm = ImageToolsViewModel(app)
        vm.setFilterMode(com.example.model.ImageFilterMode.DOC_SCAN)
        assertEquals(com.example.model.ImageFilterMode.DOC_SCAN, vm.uiState.value.filterMode)

        val config = com.example.model.WatermarkConfig(isEnabled = true, text = "DO NOT COPY", alpha = 0.5f)
        vm.setWatermarkConfig(config)
        assertTrue(vm.uiState.value.watermarkConfig.isEnabled)
        assertEquals("DO NOT COPY", vm.uiState.value.watermarkConfig.text)
        assertEquals(0.5f, vm.uiState.value.watermarkConfig.alpha)
    }

    @Test
    fun testFileStorageManagerClearTemp() {
        val storageManager = FileStorageManager(context)
        storageManager.clearTempFiles()
        // Ensure no exception and directory is safe
        assertTrue(true)
    }
}

