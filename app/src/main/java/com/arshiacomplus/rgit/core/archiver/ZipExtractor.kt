package com.arshiacomplus.rgit.core.archiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import java.io.File
class ZipExtractor {
    suspend fun extract(
        archiveFile: File,
        destinationDir: File,
        password: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!archiveFile.exists()) {
                return@withContext false
            }
            val zipFile = ZipFile(archiveFile)
            if (zipFile.isEncrypted) {
                if (password.isNullOrEmpty()) {
                    return@withContext false
                }
                zipFile.setPassword(password.toCharArray())
            }
            zipFile.extractAll(destinationDir.absolutePath)
            return@withContext true
        } catch (e: ZipException) {
            e.printStackTrace()
            return@withContext false
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
}