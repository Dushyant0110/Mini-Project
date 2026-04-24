package com.Saathi.offliniai.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.Saathi.offliniai.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ChatViewModel
    private lateinit var adapter: ChatAdapter

    private val modelPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri ?: return@registerForActivityResult
            contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.importModel(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[ChatViewModel::class.java]

        setupRecyclerView()
        setupUi()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.messagesRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ChatAdapter()
        binding.messagesRecyclerView.adapter = adapter
    }

    private fun setupUi() {
        val reasoningModeTitles = ReasoningMode.entries.map { it.title }
        val reasoningModeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            reasoningModeTitles
        )
        binding.reasoningModeDropdown.setAdapter(reasoningModeAdapter)
        binding.reasoningModeDropdown.setText(viewModel.selectedReasoningMode.value.title, false)
        binding.reasoningModeDropdown.setOnItemClickListener { _, _, position, _ ->
            viewModel.selectReasoningMode(reasoningModeTitles[position])
        }

        binding.sendButton.setOnClickListener {
            val message = binding.messageInput.text.toString()
            if (message.isNotBlank()) {
                viewModel.sendMessage(message)
                binding.messageInput.text.clear()
            }
        }

        binding.primaryActionButton.setOnClickListener {
            viewModel.handlePrimaryAction()
        }

        binding.importButton.setOnClickListener {
            modelPicker.launch(arrayOf("*/*"))
        }

        binding.clearButton.setOnClickListener {
            viewModel.clearChat()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.messages.collect { messages ->
                        adapter.submitList(messages)
                        binding.emptyStateText.visibility =
                            if (messages.isEmpty()) View.VISIBLE else View.GONE
                        if (messages.isNotEmpty()) {
                            binding.messagesRecyclerView.smoothScrollToPosition(messages.size - 1)
                        }
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        val canSend = !isLoading && viewModel.canSend.value
                        binding.sendButton.isEnabled = canSend
                        binding.messageInput.isEnabled = canSend
                        binding.loadingText.visibility = if (isLoading) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.modelStatus.collect { status ->
                        supportActionBar?.subtitle = status
                        binding.statusTextView.text = status
                    }
                }

                launch {
                    viewModel.primaryActionLabel.collect { label ->
                        binding.primaryActionButton.visibility =
                            if (label == null) View.GONE else View.VISIBLE
                        binding.primaryActionButton.text = label ?: ""
                    }
                }

                launch {
                    viewModel.downloadProgress.collect { progress ->
                        val inProgress = progress != null
                        binding.downloadProgressBar.visibility =
                            if (inProgress) View.VISIBLE else View.GONE
                        binding.downloadProgressText.visibility =
                            if (inProgress) View.VISIBLE else View.GONE

                        if (progress != null) {
                            binding.downloadProgressBar.progress = progress
                            binding.downloadProgressText.text = "Download progress: $progress%"
                        }
                    }
                }

                launch {
                    viewModel.canSend.collect { canSend ->
                        binding.sendButton.isEnabled = canSend && !viewModel.isLoading.value
                        binding.messageInput.isEnabled = canSend && !viewModel.isLoading.value
                        binding.setupHintText.visibility =
                            if (canSend) View.GONE else View.VISIBLE
                    }
                }

                launch {
                    viewModel.selectedReasoningMode.collect { mode ->
                        binding.reasoningModeDropdown.setText(mode.title, false)
                        binding.reasoningModeDescription.text = mode.description
                    }
                }
            }
        }
    }
}
