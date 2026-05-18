package com.arshiacomplus.rgit.core.downloader

import android.os.Environment
import com.arshiacomplus.rgit.data.preferences.ProxyConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URLDecoder

class RGitDownloader {

    suspend fun startDownload(
        originalUrl: String,
        partsCount: Int,
        proxyConfig: ProxyConfig,
        onProgress: (Int, String) -> Unit
    ): List<File>? = withContext(Dispatchers.IO) {

        val clientBuilder = OkHttpClient.Builder()
        if (proxyConfig.isEnabled && proxyConfig.ip.isNotEmpty()) {
            val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyConfig.ip, proxyConfig.port))
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

        val downloadedFiles = mutableListOf<File>()

        try {
            for (i in 1..partsCount) {
                val currentUrl = generateSplitUrl(originalUrl, i, partsCount)
                // * Decode URL to fix Persian names
                val rawFileName = currentUrl.substringAfterLast("/")
                val fileName = URLDecoder.decode(rawFileName, "UTF-8")

                val outputFile = File(downloadDir, fileName)

                val request = Request.Builder().url(currentUrl).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    throw Exception("Failed to download $fileName: HTTP ${response.code}")
                }

                val body = response.body ?: throw Exception("Empty body for $fileName")
                val totalBytes = body.contentLength()
                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(outputFile)

                var bytesCopied: Long = 0
                val buffer = ByteArray(8 * 1024)
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

                downloadedFiles.add(outputFile)
            }
            return@withContext downloadedFiles

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
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