package com.Saathi.offliniai.ml

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class ModelDownloader(private val context: Context) {

    companion object {
        private const val TAG = "ModelDownloader"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    data class DownloadProgress(
        val percentage: Int,
        val bytesDownloaded: Long = 0,
        val totalBytes: Long = 0,
        val filePath: String? = null,
        val error: String? = null
    )

    fun downloadModel(downloadUrl: String, destinationFileName: String): Flow<DownloadProgress> = flow {
        val request = Request.Builder().url(downloadUrl).build()

        try {
            val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }

            if (!response.isSuccessful) {
                throw IOException("Failed to download: ${response.code}")
            }

            response.use { resp ->
                resp.body?.let { body ->
                    val contentLength = body.contentLength()
                    val inputStream = body.byteStream()

                    val modelsDir = File(context.filesDir, "models")
                    modelsDir.mkdirs()

                    val modelFile = File(modelsDir, destinationFileName)
                    val outputStream = FileOutputStream(modelFile)

                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L

                    try {
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead

                            if (contentLength > 0) {
                                val progress = (totalBytesRead * 100 / contentLength).toInt()
                                emit(DownloadProgress(progress, totalBytesRead, contentLength))
                            }
                        }
                    } finally {
                        outputStream.close()
                        inputStream.close()
                    }

                    emit(DownloadProgress(100, totalBytesRead, contentLength, modelFile.absolutePath))
                    Log.d(TAG, "Download complete: ${modelFile.absolutePath}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}")
            emit(DownloadProgress(0, error = e.message))
        }
    }.flowOn(Dispatchers.IO)

    fun getDownloadedModels(): List<File> {
        val modelsDir = File(context.filesDir, "models")
        return modelsDir.listFiles()?.filter { it.isFile } ?: emptyList()
    }
}
