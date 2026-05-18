package com.arshiacomplus.rgit.core.downloader
import com.arshiacomplus.rgit.data.preferences.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
class GitMitmDownloader {
    suspend fun startDownload(
        repoUrl: String,
        filePath: String,
        appConfig: AppConfig,
        tempDir: File,
        onProgress: (Int, String) -> Unit
    ): File = withContext(Dispatchers.IO) {
        disableSslVerification()
        if (appConfig.isEnabled && appConfig.ip.isNotEmpty()) {
            System.setProperty("http.proxyHost", appConfig.ip)
            System.setProperty("http.proxyPort", appConfig.port.toString())
            System.setProperty("https.proxyHost", appConfig.ip)
            System.setProperty("https.proxyPort", appConfig.port.toString())
        } else {
            System.clearProperty("http.proxyHost")
            System.clearProperty("http.proxyPort")
            System.clearProperty("https.proxyHost")
            System.clearProperty("https.proxyPort")
        }
        if (tempDir.exists()) tempDir.deleteRecursively()
        tempDir.mkdirs()
        onProgress(10, "Connecting to Repository...")
        val cloneCommand = Git.cloneRepository()
            .setURI(repoUrl)
            .setDirectory(tempDir)
            .setDepth(1)
            .setNoCheckout(true)
        if (appConfig.githubToken.isNotEmpty()) {
            cloneCommand.setCredentialsProvider(
                UsernamePasswordCredentialsProvider(appConfig.githubToken, "")
            )
        }
        val git = cloneCommand.call()
        onProgress(60, "Fetching File: $filePath...")
        git.checkout()
            .addPath(filePath)
            .call()
        val downloadedFile = File(tempDir, filePath)
        if (!downloadedFile.exists()) {
            throw Exception("File not found in repository: $filePath")
        }
        val finalDir = File(
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
            "Rgit"
        )
        finalDir.mkdirs()
        val finalFile = File(finalDir, downloadedFile.name)
        downloadedFile.copyTo(finalFile, overwrite = true)
        git.close()
        tempDir.deleteRecursively()
        finalFile
    }
    private fun disableSslVerification() {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate>? = null
                override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
            })
            val sc = SSLContext.getInstance("TLS")
            sc.init(null, trustAllCerts, SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}