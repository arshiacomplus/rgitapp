package com.arshiacomplus.rgit.core.downloader
import android.os.Environment
import com.arshiacomplus.rgit.data.preferences.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.PathFilter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
class GitMitmDownloader {
    suspend fun startDownload(
        repoUrl: String,
        filePath: String,
        appConfig: AppConfig,
        tempDir: File,
        onProgress: (Int, String) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val downloadDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Rgit"
        )
        if (!downloadDir.exists()) downloadDir.mkdirs()
        val fileName = filePath.substringAfterLast("/")
        val outputFile = File(downloadDir, fileName)
        val originalProxySelector = ProxySelector.getDefault()
        var git: Git? = null
        try {
            if (appConfig.isEnabled && appConfig.ip.isNotEmpty()) {
                val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(appConfig.ip, appConfig.port))
                ProxySelector.setDefault(object : ProxySelector() {
                    override fun select(uri: URI?): List<Proxy> = listOf(proxy)
                    override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {}
                })
            }
            if (tempDir.exists()) {
                tempDir.deleteRecursively()
            }
            tempDir.mkdirs()
            onProgress(20, "Cloning repository info...")
            git = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(tempDir)
                .setDepth(1)
                .setBare(true)
                .call()
            onProgress(70, "Extracting file from repository...")
            val repo = git.repository
            val head = repo.resolve("HEAD") ?: throw Exception("Cannot resolve repository HEAD")
            val revWalk = RevWalk(repo)
            val commit = revWalk.parseCommit(head)
            val tree = commit.tree
            val treeWalk = TreeWalk(repo)
            treeWalk.addTree(tree)
            treeWalk.isRecursive = true
            treeWalk.filter = PathFilter.create(filePath)
            if (!treeWalk.next()) {
                throw Exception("File not found in repository: $filePath")
            }
            val objectId = treeWalk.getObjectId(0)
            val loader = repo.open(objectId)
            onProgress(90, "Writing $fileName to storage...")
            FileOutputStream(outputFile).use { out ->
                loader.copyTo(out)
            }
            onProgress(100, "$fileName downloaded successfully")
            return@withContext outputFile
        } finally {
            ProxySelector.setDefault(originalProxySelector)
            git?.close()
            if (tempDir.exists()) {
                tempDir.deleteRecursively()
            }
        }
    }
}