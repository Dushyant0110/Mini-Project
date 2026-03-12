package com.yourapp.offlineai.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourapp.offlineai.ml.LocalLLMEngine
import com.yourapp.offlineai.data.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ChatViewModel(
    private val context: Context,
    private val repository: ChatRepository
) : ViewModel() {
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _selectedModel = MutableStateFlow<String?>(null)
    val selectedModel: StateFlow<String?> = _selectedModel.asStateFlow()
    
    private var llmEngine: LocalLLMEngine? = null
    private var temperature = 0.7f
    private var maxTokens = 512
    private var systemPrompt = "You are a helpful assistant."
    
    init {
        loadChatHistory()
        checkForExistingModel()
    }
    
    private fun loadChatHistory() {
        viewModelScope.launch {
            repository.getAllMessages().collect { historyMessages ->
                _messages.value = historyMessages.map { entity ->
                    ChatMessage(
                        id = entity.id,
                        role = entity.role,
                        content = entity.content,
                        timestamp = formatTimestamp(entity.timestamp)
                    )
                }
            }
        }
    }
    
    private fun checkForExistingModel() {
        viewModelScope.launch {
            val modelPath = repository.getSelectedModelPath()
            if (modelPath != null) {
                _selectedModel.value = modelPath
                loadModel(modelPath)
            }
        }
    }
    
    fun loadModel(modelPath: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                llmEngine = LocalLLMEngine(context, modelPath)
                llmEngine?.loadModel(
                    onSuccess = {
                        _selectedModel.value = modelPath
                        repository.saveSelectedModelPath(modelPath)
                        _isLoading.value = false
                        addSystemMessage("Model loaded successfully! You can now chat.")
                    },
                    onError = { error ->
                        _isLoading.value = false
                        addSystemMessage("Error loading model: $error")
                    }
                )
            } catch (e: Exception) {
                _isLoading.value = false
                addSystemMessage("Failed to initialize model: ${e.message}")
            }
        }
    }
    
    fun sendMessage(content: String) {
        if (llmEngine == null) {
            addSystemMessage("Please load a model first")
            return
        }
        
        viewModelScope.launch {
            // Add user message
            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                role = "user",
                content = content,
                timestamp = getCurrentTimestamp()
            )
            _messages.value = _messages.value + userMessage
            repository.saveMessage(userMessage)
            
            _isLoading.value = true
            
            // Generate response
            llmEngine?.generate(
                prompt = content,
                temperature = temperature,
                maxTokens = maxTokens,
                systemPrompt = systemPrompt,
                onToken = { token ->
                    // Handle streaming tokens
                },
                onComplete = { response ->
                    _isLoading.value = false
                    val aiMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        role = "assistant",
                        content = response,
                        timestamp = getCurrentTimestamp()
                    )
                    _messages.value = _messages.value + aiMessage
                    repository.saveMessage(aiMessage)
                },
                onError = { error ->
                    _isLoading.value = false
                    addSystemMessage("Error: $error")
                }
            )
        }
    }
    
    private fun addSystemMessage(content: String) {
        val systemMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = "system",
            content = content,
            timestamp = getCurrentTimestamp()
        )
        _messages.value = _messages.value + systemMessage
    }
    
    fun updateSettings(temperature: Float, maxTokens: Int, systemPrompt: String) {
        this.temperature = temperature
        this.maxTokens = maxTokens
        this.systemPrompt = systemPrompt
    }
    
    fun clearChat() {
        viewModelScope.launch {
            repository.clearAllMessages()
            _messages.value = emptyList()
        }
    }
    
    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date())
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repository = ChatRepository.getInstance(context)
            return ChatViewModel(context, repository) as T
        }
    }
}