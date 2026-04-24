package com.Saathi.offliniai.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Saathi.offliniai.data.database.ModelRepository
import com.Saathi.offliniai.ml.LocalLLMEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class PrimaryActionType {
    PREPARE_BUNDLED_MODEL,
    LOAD_MODEL,
    RETRY
}

enum class ReasoningMode(val title: String, val description: String) {
    STANDARD(
        title = "Standard Chat",
        description = "Fastest mode. The model answers directly with no extra self-check loop."
    ),
    SELF_CHECK(
        title = "Self-Check Reasoner",
        description = "The model drafts an answer, critiques its own reasoning, then improves the final response."
    ),
    DEEP_REFLECTION(
        title = "Deep Reflection",
        description = "The model plans, tests assumptions, highlights uncertainty, and then gives a refined final answer."
    )
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ModelRepository(application.applicationContext)
    private val llmEngine = LocalLLMEngine.getInstance(application.applicationContext)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _modelStatus = MutableStateFlow("Initializing your offline workspace...")
    val modelStatus: StateFlow<String> = _modelStatus.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Int?>(null)
    val downloadProgress: StateFlow<Int?> = _downloadProgress.asStateFlow()

    private val _primaryActionLabel = MutableStateFlow<String?>(null)
    val primaryActionLabel: StateFlow<String?> = _primaryActionLabel.asStateFlow()

    private val _primaryActionType = MutableStateFlow<PrimaryActionType?>(null)
    val primaryActionType: StateFlow<PrimaryActionType?> = _primaryActionType.asStateFlow()

    private val _canSend = MutableStateFlow(false)
    val canSend: StateFlow<Boolean> = _canSend.asStateFlow()

    private val _selectedReasoningMode = MutableStateFlow(ReasoningMode.SELF_CHECK)
    val selectedReasoningMode: StateFlow<ReasoningMode> = _selectedReasoningMode.asStateFlow()

    init {
        ensureWelcomeMessage()
        prepareModel()
    }

    fun handlePrimaryAction() {
        when (_primaryActionType.value) {
            PrimaryActionType.PREPARE_BUNDLED_MODEL -> prepareBundledModel()
            PrimaryActionType.LOAD_MODEL -> loadCurrentModel()
            PrimaryActionType.RETRY -> prepareModel()
            null -> Unit
        }
    }

    fun selectReasoningMode(modeTitle: String) {
        val mode = ReasoningMode.entries.firstOrNull { it.title == modeTitle } ?: return
        _selectedReasoningMode.value = mode
        addOrReplaceAssistantMessage(
            when (mode) {
                ReasoningMode.STANDARD ->
                    "Reasoning mode set to Standard Chat. Replies will be quicker and more direct."
                ReasoningMode.SELF_CHECK ->
                    "Reasoning mode set to Self-Check Reasoner. The assistant will draft, critique, and improve before answering."
                ReasoningMode.DEEP_REFLECTION ->
                    "Reasoning mode set to Deep Reflection. The assistant will plan, test assumptions, and refine the final answer."
            }
        )
    }

    fun prepareModel() {
        viewModelScope.launch {
            val model = repository.getPrimaryModel()
            val importedFile = repository.getImportedModelFile()
            val recommendedFile = repository.getRecommendedModelFile()
            val bundledAvailable = repository.hasBundledModelAsset()

            when {
                importedFile.exists() -> {
                    _modelStatus.value = "Imported model found. Ready to load."
                    _primaryActionLabel.value = "Load Imported Model"
                    _primaryActionType.value = PrimaryActionType.LOAD_MODEL
                    _canSend.value = false
                    addOrReplaceAssistantMessage(
                        "I found your imported .task model. Tap Load Imported Model when you're ready."
                    )
                }

                recommendedFile.exists() -> {
                    _modelStatus.value = "Bundled model prepared. Ready to load."
                    _primaryActionLabel.value = "Load Bundled Model"
                    _primaryActionType.value = PrimaryActionType.LOAD_MODEL
                    _canSend.value = false
                    addOrReplaceAssistantMessage(
                        "The bundled offline model is already unpacked on your device. Tap Load Bundled Model when you want to bring it online."
                    )
                }

                bundledAvailable -> {
                    _modelStatus.value = "Bundled offline model included"
                    _primaryActionLabel.value = "Prepare Bundled Model"
                    _primaryActionType.value = PrimaryActionType.PREPARE_BUNDLED_MODEL
                    _canSend.value = false
                    addOrReplaceAssistantMessage(
                        "This build already carries a bundled offline model. Prepare it once, then load it and choose how much self-check reasoning you want."
                    )
                }

                model.isDownloaded -> {
                    _modelStatus.value = "Model file found. Ready to load."
                    _primaryActionLabel.value = "Load Model"
                    _primaryActionType.value = PrimaryActionType.LOAD_MODEL
                    _canSend.value = false
                    addOrReplaceAssistantMessage(
                        "I found a model file on your device. Tap Load Model when you're ready to bring it online."
                    )
                }

                else -> {
                    _modelStatus.value = "No model available yet"
                    _primaryActionLabel.value = "Retry Setup"
                    _primaryActionType.value = PrimaryActionType.RETRY
                    _canSend.value = false
                    addOrReplaceAssistantMessage(
                        "I couldn't find a usable bundled or imported model yet. Retry setup or import your own compatible .task file."
                    )
                }
            }
        }
    }

    private fun prepareBundledModel() {
        viewModelScope.launch {
            _primaryActionLabel.value = null
            _primaryActionType.value = null
            _downloadProgress.value = 0
            _modelStatus.value = "Preparing bundled model..."
            _canSend.value = false
            addOrReplaceAssistantMessage(
                "Preparing the bundled offline model from inside the app package. This only needs to happen once."
            )

            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val targetFile = repository.getRecommendedModelFile()
                    if (!targetFile.exists()) {
                        getApplication<Application>().assets
                            .open(ModelRepository.BUNDLED_MODEL_ASSET_PATH)
                            .use { input ->
                                targetFile.parentFile?.mkdirs()
                                targetFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                    }
                    targetFile
                }
            }

            _downloadProgress.value = null

            if (result.isFailure) {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                _modelStatus.value = "Preparation failed: $errorMsg"
                _primaryActionLabel.value = "Retry Setup"
                _primaryActionType.value = PrimaryActionType.RETRY
                addOrReplaceAssistantMessage(
                    "I couldn't unpack the bundled model.\n\nError: $errorMsg\n\nYou can retry or import your own compatible .task file."
                )
                return@launch
            }

            _modelStatus.value = "Bundled model prepared. Ready to load."
            _primaryActionLabel.value = "Load Bundled Model"
            _primaryActionType.value = PrimaryActionType.LOAD_MODEL
            addOrReplaceAssistantMessage(
                "The bundled model is ready on your device now. Tap Load Bundled Model when you want to activate it."
            )
        }
    }

    private fun loadCurrentModel() {
        viewModelScope.launch {
            _modelStatus.value = "Loading model..."
            _primaryActionLabel.value = null
            _primaryActionType.value = null
            _canSend.value = false

            val result = llmEngine.loadDefaultModel()
            if (result.isSuccess) {
                _modelStatus.value = "Model ready"
                _primaryActionLabel.value = null
                _primaryActionType.value = null
                _canSend.value = true
                addOrReplaceAssistantMessage(
                    "The model is loaded and ready. Your current reasoning mode is ${_selectedReasoningMode.value.title}."
                )
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                _modelStatus.value = "Load error: $errorMsg"
                _primaryActionLabel.value = "Retry"
                _primaryActionType.value = PrimaryActionType.RETRY
                _canSend.value = false
                addOrReplaceAssistantMessage(
                    "The model file exists, but loading failed.\n\nError: $errorMsg\n\nTry setup again or import a different compatible .task file."
                )
            }
        }
    }

    fun importModel(uri: Uri) {
        viewModelScope.launch {
            _modelStatus.value = "Importing model file..."
            _canSend.value = false
            addOrReplaceAssistantMessage("Importing your selected model file into the app...")

            val importResult = withContext(Dispatchers.IO) {
                runCatching {
                    val resolver = getApplication<Application>().contentResolver
                    val targetFile = repository.getImportedModelFile()
                    resolver.openInputStream(uri)?.use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    } ?: error("Unable to read the selected model file.")
                }
            }

            if (importResult.isFailure) {
                val errorMsg = importResult.exceptionOrNull()?.message ?: "Import failed"
                _modelStatus.value = "Import failed: $errorMsg"
                _primaryActionLabel.value = "Retry Setup"
                _primaryActionType.value = PrimaryActionType.RETRY
                addOrReplaceAssistantMessage("I couldn't import that model file.\n\nError: $errorMsg")
                return@launch
            }

            _modelStatus.value = "Import complete. Ready to load."
            _primaryActionLabel.value = "Load Imported Model"
            _primaryActionType.value = PrimaryActionType.LOAD_MODEL
            addOrReplaceAssistantMessage(
                "Your imported model is in place. Tap Load Imported Model when you're ready."
            )
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return

        if (!_canSend.value) {
            addAssistantMessage("The model isn't ready yet. Finish setup first, then I can answer.")
            return
        }

        viewModelScope.launch {
            val userMessage = ChatMessage(role = "user", content = content)
            _messages.value = _messages.value + userMessage

            _isLoading.value = true

            var fullResponse = ""
            llmEngine.generateResponse(buildPrompt(content)).collect { token ->
                fullResponse += token

                val currentList = _messages.value.toMutableList()
                if (currentList.lastOrNull()?.role == "assistant") {
                    currentList[currentList.size - 1] = currentList.last().copy(content = fullResponse)
                } else {
                    currentList.add(ChatMessage(role = "assistant", content = fullResponse))
                }
                _messages.value = currentList
            }

            _isLoading.value = false
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
        llmEngine.clearConversationHistory()
        ensureWelcomeMessage()
    }

    private fun buildPrompt(userInput: String): String {
        return when (_selectedReasoningMode.value) {
            ReasoningMode.STANDARD -> userInput
            ReasoningMode.SELF_CHECK -> """
                You are a self-correcting reasoning assistant.
                First think through the user's request carefully.
                Then silently critique your first draft for mistakes, gaps, or weak assumptions.
                Finally provide only the improved final answer.

                User request:
                $userInput
            """.trimIndent()

            ReasoningMode.DEEP_REFLECTION -> """
                You are a careful reasoning assistant.
                Work in this order:
                1. Build a brief internal plan.
                2. Test your assumptions and look for edge cases.
                3. Revise the answer for clarity and correctness.
                4. Return only the final polished response with uncertainty noted when needed.

                User request:
                $userInput
            """.trimIndent()
        }
    }

    private fun ensureWelcomeMessage() {
        if (_messages.value.isEmpty()) {
            _messages.value = listOf(
                ChatMessage(
                    role = "assistant",
                    content = "Offline AI Assistant is ready to wake up. This build includes a bundled model for offline setup, and you can choose how deeply it reasons before answering."
                )
            )
        }
    }

    private fun addAssistantMessage(content: String) {
        _messages.value = _messages.value + ChatMessage(role = "assistant", content = content)
    }

    private fun addOrReplaceAssistantMessage(content: String) {
        val currentMessages = _messages.value.toMutableList()
        if (currentMessages.lastOrNull()?.role == "assistant") {
            currentMessages[currentMessages.lastIndex] =
                currentMessages.last().copy(content = content)
        } else {
            currentMessages.add(ChatMessage(role = "assistant", content = content))
        }
        _messages.value = currentMessages
    }

    override fun onCleared() {
        super.onCleared()
        llmEngine.unloadModel()
    }
}
