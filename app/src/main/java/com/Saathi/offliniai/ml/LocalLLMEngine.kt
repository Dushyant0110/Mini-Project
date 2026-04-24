package com.Saathi.offliniai.ml

import android.content.Context
import android.util.Log
import com.Saathi.offliniai.data.database.ModelRepository
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class LocalLLMEngine(private val context: Context) {

    companion object {
        private const val TAG = "LocalLLMEngine"

        @Volatile
        private var instance: LocalLLMEngine? = null

        fun getInstance(context: Context): LocalLLMEngine {
            return instance ?: synchronized(this) {
                val newInstance = LocalLLMEngine(context.applicationContext)
                instance = newInstance
                newInstance
            }
        }
    }

    private val repository = ModelRepository(context)
    private var currentLlm: LlmInference? = null
    private var isModelLoaded = false

    suspend fun loadModel(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val modelFile = getModelFile()

                if (modelFile == null || !modelFile.exists()) {
                    return@withContext Result.failure(Exception("Model file not found on device."))
                }

                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(512)
                    .setTemperature(0.7f)
                    .setTopK(40)
                    .build()

                currentLlm = LlmInference.createFromOptions(context, options)
                isModelLoaded = true

                Log.d(TAG, "Model loaded successfully from ${modelFile.absolutePath}")
                Result.success(true)
            } catch (e: Exception) {
                Log.e(TAG, "Model loading failed: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun loadDefaultModel() = loadModel()

    private suspend fun getModelFile(): File? {
        return withContext(Dispatchers.IO) {
            repository.getActiveModelFile()?.let { return@withContext it }

            if (repository.hasBundledModelAsset()) {
                val bundledTarget = repository.getRecommendedModelFile()
                if (!bundledTarget.exists()) {
                    try {
                        context.assets.open(ModelRepository.BUNDLED_MODEL_ASSET_PATH).use { inputStream ->
                            bundledTarget.parentFile?.mkdirs()
                            FileOutputStream(bundledTarget).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to unpack bundled model asset", e)
                    }
                }

                if (bundledTarget.exists()) {
                    return@withContext bundledTarget
                }
            }

            null
        }
    }

    fun generateResponse(prompt: String): Flow<String> = flow {
        val llm = currentLlm
        if (llm == null) {
            emit("Model not loaded.")
            return@flow
        }

        try {
            emit(llm.generateResponse(prompt))
        } catch (e: Exception) {
            emit("Error: ${e.message}")
        }
    }

    fun isLoaded(): Boolean = isModelLoaded

    fun clearConversationHistory() {
        // Placeholder for future session-aware engines.
    }

    fun unloadModel() {
        currentLlm?.close()
        currentLlm = null
        isModelLoaded = false
    }
}
