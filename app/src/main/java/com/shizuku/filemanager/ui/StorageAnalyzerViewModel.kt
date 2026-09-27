package com.shizuku.filemanager.ui

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shizuku.filemanager.sys.AppManager
import com.shizuku.filemanager.sys.AppStorageItem
import com.shizuku.filemanager.sys.LargeFile
import com.shizuku.filemanager.sys.StorageCategory
import com.shizuku.filemanager.sys.StorageScanner
import com.shizuku.filemanager.sys.StorageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class StorageAnalyzerUiState(
    val stats: StorageStats? = null,
    val oldFiles: List<LargeFile> = emptyList(),
    val largeFiles: List<LargeFile> = emptyList(),
    val appStorageItems: List<AppStorageItem> = emptyList(),
    val isLoading: Boolean = true,
    val selectedCategory: StorageCategory? = null,
    val categoryFiles: List<LargeFile> = emptyList(),
    val isCategoryLoading: Boolean = false
)

class StorageAnalyzerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(StorageAnalyzerUiState())
    val uiState: StateFlow<StorageAnalyzerUiState> = _uiState.asStateFlow()

    init {
        loadStorageStats()
    }

    fun loadStorageStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val root = Environment.getExternalStorageDirectory()
            val context = getApplication<Application>()
            val stats = StorageScanner.getStorageStats(root, context)
            val oldFiles = StorageScanner.getOldFiles(root)
            val largeFiles = StorageScanner.getLargeFiles(root)
            val appStorageItems = AppManager.getAppsWithStorageStats(context)

            val currentSelected = _uiState.value.selectedCategory
            val categoryFiles = if (currentSelected != null) {
                StorageScanner.getCategoryFiles(root, currentSelected.name, context)
            } else {
                emptyList()
            }

            _uiState.update {
                it.copy(
                    stats = stats,
                    oldFiles = oldFiles,
                    largeFiles = largeFiles,
                    appStorageItems = appStorageItems,
                    categoryFiles = categoryFiles,
                    isLoading = false
                )
            }
        }
    }

    fun selectCategory(category: StorageCategory?) {
        if (category == null) {
            _uiState.update { it.copy(selectedCategory = null, categoryFiles = emptyList()) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(selectedCategory = category, isCategoryLoading = true) }
            val root = Environment.getExternalStorageDirectory()
            val context = getApplication<Application>()
            val files = StorageScanner.getCategoryFiles(root, category.name, context)
            val appStorageItems = if (_uiState.value.appStorageItems.isEmpty() || category.name.contains("App", ignoreCase = true) || category.name.contains("Tool", ignoreCase = true)) {
                AppManager.getAppsWithStorageStats(context)
            } else {
                _uiState.value.appStorageItems
            }

            _uiState.update {
                it.copy(
                    categoryFiles = files,
                    appStorageItems = appStorageItems,
                    isCategoryLoading = false
                )
            }
        }
    }

    fun renameFile(file: File, newName: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val destination = File(file.parentFile, newName)
            val success = file.renameTo(destination)
            withContext(Dispatchers.Main) {
                onResult(success)
                if (success) {
                    loadStorageStats()
                }
            }
        }
    }

    fun deleteFile(file: File, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = file.delete()
            withContext(Dispatchers.Main) {
                onResult(success)
                if (success) {
                    loadStorageStats()
                }
            }
        }
    }
}
