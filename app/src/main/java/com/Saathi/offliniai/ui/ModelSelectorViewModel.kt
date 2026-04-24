package com.Saathi.offliniai.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Saathi.offliniai.data.database.AIModel
import com.Saathi.offliniai.data.database.ModelRepository
import com.Saathi.offliniai.ml.ModelDownloader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ModelSelectorViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val repository = ModelRepository(context)
    private val downloader = ModelDownloader(context)

    private val _models = MutableStateFlow<List<AIModel>>(emptyList())
    val models: StateFlow<List<AIModel>> = _models

    private val _downloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Int>> = _downloadProgress

    init {
        refreshModels()
    }

    fun refreshModels() {
        viewModelScope.launch {
            _models.value = repository.getAvailableModels().first()
        }
    }

    fun downloadModel(model: AIModel) {
        viewModelScope.launch {
            downloader.downloadModel(model.downloadUrl, model.fileName).collect { progress ->
                _downloadProgress.value = _downloadProgress.value + (model.id to progress.percentage)

                if (progress.filePath != null || progress.percentage == 100) {
                    repository.updateModelDownloadStatus(model.id, true, progress.filePath)
                    _downloadProgress.value = _downloadProgress.value - model.id
                    refreshModels()
                }

                if (progress.error != null) {
                    _downloadProgress.value = _downloadProgress.value - model.id
                }
            }
        }
    }

    fun deleteModel(model: AIModel) {
        viewModelScope.launch {
            repository.deleteModel(model)
            repository.updateModelDownloadStatus(model.id, false, null)
            refreshModels()
        }
    }
}
