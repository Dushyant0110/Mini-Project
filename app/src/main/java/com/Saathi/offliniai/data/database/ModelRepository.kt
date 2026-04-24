package com.Saathi.offliniai.data.database

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

data class AIModel(
    val id: String,
    val name: String,
    val description: String,
    val downloadUrl: String,
    val fileName: String,
    val fileSize: Long,
    val ramRequired: Int,
    val requiresInternet: Boolean,
    val isDownloaded: Boolean = false,
    val localPath: String? = null
)

class ModelRepository(private val context: Context) {

    companion object {
        const val RECOMMENDED_MODEL_FILENAME = "gemma.task"
        const val IMPORTED_MODEL_FILENAME = "imported-model.task"
        const val BUNDLED_MODEL_ASSET_PATH = "models/gemma.task"
    }

    private val recommendedModel = AIModel(
        id = "gemma-3-1b-it-bundled",
        name = "Bundled Gemma 3 1B IT",
        description = "Built into the app for offline setup. Load it directly, or import your own compatible MediaPipe .task model.",
        downloadUrl = "",
        fileName = RECOMMENDED_MODEL_FILENAME,
        fileSize = 555_000_000L,
        ramRequired = 2048,
        requiresInternet = false
    )

    fun getAvailableModels(): Flow<List<AIModel>> = flow {
        emit(listOf(refreshModelState(recommendedModel)))
    }

    suspend fun getPrimaryModel(): AIModel = refreshModelState(recommendedModel)

    fun getDownloadedModels(): Flow<List<AIModel>> = flow {
        val model = refreshModelState(recommendedModel)
        emit(if (model.isDownloaded) listOf(model) else emptyList())
    }

    suspend fun initializeDefaultModels() {
        // Bundled asset or file presence is the source of truth.
    }

    suspend fun updateModelDownloadStatus(
        modelId: String,
        downloaded: Boolean,
        localPath: String?
    ) {
        @Suppress("UNUSED_VARIABLE")
        val ignored = Triple(modelId, downloaded, localPath)
    }

    suspend fun deleteModel(model: AIModel) {
        val files = listOf(
            File(context.filesDir, "models/${model.fileName}"),
            File(context.filesDir, "models/$IMPORTED_MODEL_FILENAME")
        )
        files.forEach { file ->
            if (file.exists()) {
                file.delete()
            }
        }
    }

    fun getRecommendedModelFile(): File = ensureModelsDir().resolve(RECOMMENDED_MODEL_FILENAME)

    fun getImportedModelFile(): File = ensureModelsDir().resolve(IMPORTED_MODEL_FILENAME)

    fun getActiveModelFile(): File? {
        val imported = getImportedModelFile()
        if (imported.exists()) return imported

        val recommended = getRecommendedModelFile()
        if (recommended.exists()) return recommended

        return null
    }

    fun hasBundledModelAsset(): Boolean {
        return runCatching {
            context.assets.open(BUNDLED_MODEL_ASSET_PATH).close()
            true
        }.getOrDefault(false)
    }

    private fun ensureModelsDir(): File {
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        return modelsDir
    }

    private fun refreshModelState(model: AIModel): AIModel {
        val activeFile = getActiveModelFile()
        val bundledAvailable = hasBundledModelAsset()
        return model.copy(
            isDownloaded = activeFile?.exists() == true || bundledAvailable,
            localPath = when {
                activeFile?.exists() == true -> activeFile.absolutePath
                bundledAvailable -> BUNDLED_MODEL_ASSET_PATH
                else -> null
            },
            fileSize = when {
                activeFile?.exists() == true -> activeFile.length()
                bundledAvailable -> model.fileSize
                else -> model.fileSize
            }
        )
    }
}
