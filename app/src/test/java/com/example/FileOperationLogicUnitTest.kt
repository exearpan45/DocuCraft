package com.example

import com.example.domain.PdfCompressionLevel
import com.example.model.HistoryItem
import com.example.model.HistorySortOption
import com.example.model.OutputFileType
import com.example.model.TargetSizePreset
import com.example.model.calculateAspectRatio
import com.example.model.filterAndSortHistory
import com.example.model.formatFileSize
import com.example.viewmodel.parsePageRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileOperationLogicUnitTest {

    @Test
    fun testFormatFileSize() {
        assertEquals("0 B", formatFileSize(0))
        assertEquals("512 B", formatFileSize(512))
        assertEquals("1.0 KB", formatFileSize(1024))
        assertEquals("1.5 KB", formatFileSize(1536))
        assertEquals("2.00 MB", formatFileSize(2 * 1024 * 1024))
        assertEquals("1.25 GB", formatFileSize((1.25 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun testCalculateAspectRatio() {
        assertEquals("16:9", calculateAspectRatio(1920, 1080))
        assertEquals("4:3", calculateAspectRatio(1024, 768))
        assertEquals("1:1", calculateAspectRatio(500, 500))
        assertEquals("1:1", calculateAspectRatio(0, 0))
    }

    @Test
    fun testParsePageRangeSingleAndMultiple() {
        val result = parsePageRange("1, 3, 5", 10)
        assertEquals(setOf(0, 2, 4), result)
    }

    @Test
    fun testParsePageRangeHyphenated() {
        val result = parsePageRange("1-3, 5-6", 10)
        assertEquals(setOf(0, 1, 2, 4, 5), result)
    }

    @Test
    fun testParsePageRangeClamping() {
        val result = parsePageRange("1-10", 5)
        assertEquals(setOf(0, 1, 2, 3, 4), result)
    }

    @Test
    fun testParsePageRangeInvalidTokens() {
        val result = parsePageRange("invalid, -5, abc, 2", 10)
        assertEquals(setOf(1), result)
    }

    @Test
    fun testParsePageRangeEmpty() {
        val result = parsePageRange("", 10)
        assertTrue(result.isEmpty())
    }

    @Test
    fun testTargetSizePresets() {
        assertEquals(100 * 1024L, TargetSizePreset.UNDER_100_KB.targetBytes)
        assertEquals(200 * 1024L, TargetSizePreset.UNDER_200_KB.targetBytes)
        assertEquals(500 * 1024L, TargetSizePreset.UNDER_500_KB.targetBytes)
        assertEquals(1024 * 1024L, TargetSizePreset.UNDER_1_MB.targetBytes)
        assertTrue(TargetSizePreset.UNDER_100_KB.targetBytes < TargetSizePreset.UNDER_200_KB.targetBytes)
    }

    @Test
    fun testFilterAndSortHistoryByNameAndDate() {
        val sampleItems = listOf(
            HistoryItem("1", "contract_scan.pdf", OutputFileType.PDF, 500000L, 1000L, "/path/1"),
            HistoryItem("2", "family_vacation.jpg", OutputFileType.IMAGE, 1200000L, 2000L, "/path/2"),
            HistoryItem("3", "receipt_november.pdf", OutputFileType.PDF, 150000L, 3000L, "/path/3")
        )

        // Filter by name
        val filteredByName = filterAndSortHistory(sampleItems, "receipt", HistorySortOption.NEWEST)
        assertEquals(1, filteredByName.size)
        assertEquals("receipt_november.pdf", filteredByName[0].fileName)

        // Filter by file type
        val filteredByType = filterAndSortHistory(sampleItems, "pdf", HistorySortOption.NEWEST)
        assertEquals(2, filteredByType.size)

        // Sort by Size descending
        val sortedBySize = filterAndSortHistory(sampleItems, "", HistorySortOption.SIZE_DESC)
        assertEquals("family_vacation.jpg", sortedBySize[0].fileName)
        assertEquals("receipt_november.pdf", sortedBySize[2].fileName)

        // Sort by Name ascending
        val sortedByName = filterAndSortHistory(sampleItems, "", HistorySortOption.NAME_ASC)
        assertEquals("contract_scan.pdf", sortedByName[0].fileName)
    }

    @Test
    fun testPdfCompressionLevelSettings() {
        assertTrue(PdfCompressionLevel.STRONG.resolutionScale <= PdfCompressionLevel.BALANCED.resolutionScale)
        assertTrue(PdfCompressionLevel.STRONG.jpegQuality <= PdfCompressionLevel.BALANCED.jpegQuality)
        assertTrue(PdfCompressionLevel.BALANCED.jpegQuality <= PdfCompressionLevel.LIGHT.jpegQuality)
    }
}
