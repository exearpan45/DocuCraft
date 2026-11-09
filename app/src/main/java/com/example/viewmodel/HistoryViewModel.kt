package com.example.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.FileStorageManager
import com.example.model.HistoryItem
import com.example.model.HistorySortOption
import com.example.model.OutputFileType
import com.example.model.filterAndSortHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class HistoryUiState(
    val items: List<HistoryItem> = emptyList(),
    val searchQuery: String = "",
    val sortOption: HistorySortOption = HistorySortOption.NEWEST,
    val isLoading: Boolean = false
) {
    val displayedItems: List<HistoryItem>
        get() = filterAndSortHistory(items, searchQuery, sortOption)
}

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    val storageManager = FileStorageManager(application)

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            val files = storageManager.getOutputFiles()
            val items = files.map { file ->
                val isPdf = file.extension.equals("pdf", ignoreCase = true)
                HistoryItem(
                    id = file.absolutePath,
                    fileName = file.name,
                    fileType = if (isPdf) OutputFileType.PDF else OutputFileType.IMAGE,
                    sizeBytes = file.length(),
                    timestamp = file.lastModified(),
                    filePath = file.absolutePath
                )
            }
            _uiState.update { it.copy(items = items, isLoading = false) }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSortOption(sort: HistorySortOption) {
        _uiState.update { it.copy(sortOption = sort) }
    }

    fun deleteItem(item: HistoryItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(item.filePath)
            storageManager.deleteOutputFile(file)
            loadHistory()
        }
    }

    fun createShareIntent(item: HistoryItem): Intent {
        val file = File(item.filePath)
        val mime = if (item.fileType == OutputFileType.PDF) "application/pdf" else "image/*"
        return storageManager.createShareIntent(file, mime)
    }

    fun createViewIntent(item: HistoryItem): Intent {
        val file = File(item.filePath)
        val mime = if (item.fileType == OutputFileType.PDF) "application/pdf" else "image/*"
        return storageManager.createViewIntent(file, mime)
    }
}
