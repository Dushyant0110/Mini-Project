package com.Saathi.offliniai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.DecimalFormat
import com.Saathi.offliniai.data.database.AIModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectorScreen(
    modifier: Modifier = Modifier,
    viewModel: ModelSelectorViewModel = viewModel()
) {
    val models by viewModel.models.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Models") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(models) { model ->
                ModelCard(
                    model = model,
                    isDownloaded = model.isDownloaded,
                    downloadProgress = downloadProgress[model.id],
                    onDownloadClick = {
                        viewModel.downloadModel(model)
                    },
                    onSelectClick = {
                        // Handle selection
                    }
                )
            }
        }
    }
}

@Composable
fun ModelCard(
    model: AIModel,
    isDownloaded: Boolean,
    downloadProgress: Int?,
    onDownloadClick: () -> Unit,
    onSelectClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = model.name,
                    style = MaterialTheme.typography.titleLarge
                )
                
                if (isDownloaded) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Downloaded",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Text(
                text = model.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SpecItem(
                    icon = Icons.Default.Storage,
                    label = formatFileSize(model.fileSize)
                )
                SpecItem(
                    icon = Icons.Default.Memory,
                    label = "${model.ramRequired}MB RAM"
                )
                SpecItem(
                    icon = if (model.requiresInternet) Icons.Default.Public else Icons.Default.CloudOff,
                    label = if (model.requiresInternet) "Online" else "Offline"
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (downloadProgress != null && downloadProgress < 100) {
                LinearProgressIndicator(
                    progress = { downloadProgress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Downloading: $downloadProgress%",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isDownloaded) {
                        OutlinedButton(
                            onClick = onDownloadClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download")
                        }
                    }
                    
                    Button(
                        onClick = onSelectClick,
                        modifier = Modifier.weight(1f),
                        enabled = isDownloaded
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Select")
                    }
                }
            }
        }
    }
}

@Composable
fun SpecItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}
