package com.arshiacomplus.rgit.core.downloader
import android.os.Environment
import com.arshiacomplus.rgit.data.preferences.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URLDecoder
import java.util.concurrent.TimeUnit
class RGitDownloader {
    suspend fun startDownload(
        originalUrl: String,
        partsCount: Int,
        appConfig: AppConfig,
        onProgress: (Int, String) -> Unit
    ): List<File> = coroutineScope {
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
        if (appConfig.isEnabled && appConfig.ip.isNotEmpty()) {
            val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(appConfig.ip, appConfig.port))
            clientBuilder.proxy(proxy)
        }
        val client = clientBuilder.build()
        val downloadDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Rgit"
        )
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        val semaphore = Semaphore(appConfig.threads)
        val downloadedFiles = mutableListOf<File>()
        val deferredParts = (1..partsCount).map { i ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    val currentUrl = generateSplitUrl(originalUrl, i, partsCount)
                    val rawFileName = currentUrl.substringAfterLast("/")
                    val fileName = URLDecoder.decode(rawFileName, "UTF-8")
                    val outputFile = File(downloadDir, fileName)
                    var downloadedLength = 0L
                    if (outputFile.exists()) {
                        downloadedLength = outputFile.length()
                    }
                    val requestBuilder = Request.Builder().url(currentUrl)
                    if (downloadedLength > 0) {
                        requestBuilder.addHeader("Range", "bytes=$downloadedLength-")
                    }
                    val response = client.newCall(requestBuilder.build()).execute()
                    if (response.code == 416) {
                        onProgress(100, "$fileName (Resumed)")
                        return@withPermit outputFile
                    }
                    if (!response.isSuccessful) {
                        response.close()
                        throw Exception("HTTP ${response.code} for $fileName")
                    }
                    val body = response.body ?: throw Exception("Empty body for $fileName")
                    val isResumeSupported = response.code == 206
                    val totalBytes = if (isResumeSupported) downloadedLength + body.contentLength() else body.contentLength()
                    if (!isResumeSupported && downloadedLength > 0) {
                        downloadedLength = 0L
                        outputFile.delete()
                    }
                    val inputStream = body.byteStream()
                    val outputStream = FileOutputStream(outputFile, isResumeSupported)
                    var bytesCopied = downloadedLength
                    val buffer = ByteArray(32 * 1024)
                    var bytes = inputStream.read(buffer)
                    while (bytes >= 0) {
                        outputStream.write(buffer, 0, bytes)
                        bytesCopied += bytes
                        if (totalBytes > 0) {
                            val progress = ((bytesCopied * 100) / totalBytes).toInt()
                            onProgress(progress, fileName)
                        }
                        bytes = inputStream.read(buffer)
                    }
                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()
                    response.close()
                    outputFile
                }
            }
        }
        val results = deferredParts.awaitAll()
        downloadedFiles.addAll(results)
        downloadedFiles
    }
    private fun generateSplitUrl(originalUrl: String, currentIndex: Int, totalParts: Int): String {
        if (totalParts == 1) return originalUrl
        val regex = Regex("(\\d+)$")
        val matchResult = regex.find(originalUrl)
        return if (matchResult != null) {
            val numberString = matchResult.value
            val paddingLength = numberString.length
            val newNumberString = String.format("%0${paddingLength}d", currentIndex)
            originalUrl.replaceRange(matchResult.range, newNumberString)
        } else {
            originalUrl
        }
    }
}