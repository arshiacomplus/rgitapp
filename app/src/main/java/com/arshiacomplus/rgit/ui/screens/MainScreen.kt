package com.arshiacomplus.rgit.ui.screens

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.StrictMode
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arshiacomplus.rgit.core.archiver.ZipExtractor
import com.arshiacomplus.rgit.core.downloader.RGitDownloader
import com.arshiacomplus.rgit.data.preferences.ProxyDataStore
import com.arshiacomplus.rgit.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(proxyDataStore: ProxyDataStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var url by remember { mutableStateOf("") }
    var repoUrl by remember { mutableStateOf("") }
    var filePath by remember { mutableStateOf("") }
    var isGithubMitmMode by remember { mutableStateOf(true) }
    var isSplit by remember { mutableStateOf(false) }
    var partsCount by remember { mutableStateOf("1") }
    var autoUnzip by remember { mutableStateOf(false) }

    var progress by remember { mutableStateOf(0) }
    var currentFileStatus by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var showProxyDialog by remember { mutableStateOf(false) }
    var downloadFinished by remember { mutableStateOf(false) }

    var finalFileToOpen by remember { mutableStateOf<File?>(null) }

    val appConfig by proxyDataStore.proxySettingsFlow.collectAsState(initial = null)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            isDownloading = true
            downloadFinished = false
            progress = 0
            currentFileStatus = "Starting..."
            finalFileToOpen = null

            scope.launch {
                try {
                    val downloader = RGitDownloader()
                    val extractor = ZipExtractor()
                    val count = if (isSplit) partsCount.toIntOrNull() ?: 1 else 1

                    val downloadedFiles = if (isGithubMitmMode) {
                        val mitmDownloader = com.arshiacomplus.rgit.core.downloader.GitMitmDownloader()
                        val tempDir = File(context.cacheDir, "git_temp")
                        val resultFile = mitmDownloader.startDownload(
                            repoUrl = appConfig?.repoUrl?.takeIf { it.isNotBlank() } ?: throw Exception("Repo URL is empty in Settings!"),
                            filePath = filePath,
                            appConfig = appConfig!!,
                            tempDir = tempDir,
                            onProgress = { p, statusMsg ->
                                progress = p
                                currentFileStatus = statusMsg
                            }
                        )
                        listOf(resultFile)
                    } else {
                        val downloader = RGitDownloader()
                        downloader.startDownload(
                            originalUrl = url,
                            partsCount = count,
                            appConfig = appConfig ?: throw Exception("Config not loaded"),
                            onProgress = { p, fileName ->
                                progress = p
                                currentFileStatus = "Downloading $fileName..."
                            }
                        )
                    }

                    if (downloadedFiles.isNotEmpty()) {
                        var finalFile = downloadedFiles.first()

                        if (isSplit && downloadedFiles.size > 1) {
                            currentFileStatus = "Combining parts..."
                            val regex = Regex("\\.\\d{3,}\$")
                            val baseName = finalFile.name.replace(regex, "")
                            val combinedFile = File(finalFile.parentFile, baseName)

                            val combined = withContext(Dispatchers.IO) {
                                try {
                                    FileOutputStream(combinedFile).use { out ->
                                        for (part in downloadedFiles) {
                                            FileInputStream(part).use { input ->
                                                input.copyTo(out)
                                            }
                                        }
                                    }
                                    downloadedFiles.forEach { it.delete() }
                                    true
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    false
                                }
                            }

                            if (combined) {
                                finalFile = combinedFile
                            } else {
                                throw Exception("Combining Failed!")
                            }
                        }

                        finalFileToOpen = finalFile

                        if (autoUnzip) {
                            currentFileStatus = "Extracting files..."
                            val extSuccess = extractor.extract(finalFile, finalFile.parentFile)
                            if (extSuccess) {
                                currentFileStatus = "Extracted Successfully!"
                            } else {
                                throw Exception("Extraction Failed!")
                            }
                        } else {
                            currentFileStatus = "Process Complete!"
                        }
                    } else {
                        throw Exception("Download Failed! No files received.")
                    }

                } catch (e: Exception) {
                    currentFileStatus = "Error: ${e.message}"
                } finally {
                    isDownloading = false
                    downloadFinished = true
                }
            }
        } else {
            currentFileStatus = "Error: Storage Permission Denied!"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RGit", color = GhTextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GhSurface),
                actions = {
                    IconButton(onClick = { showProxyDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = GhTextPrimary)
                    }
                }
            )
        },
        containerColor = GhBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = isGithubMitmMode,
                    onCheckedChange = { isGithubMitmMode = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GhTextPrimary,
                        checkedTrackColor = GhButtonPrimary
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Use GitHub MITM Mode", color = GhTextPrimary)
            }

            if (isGithubMitmMode) {
                OutlinedTextField(
                    value = filePath,
                    onValueChange = { filePath = it },
                    label = { Text("File Path in Repo (e.g., dl/movie.mp4)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = GhSurface,
                        unfocusedContainerColor = GhSurface,
                        focusedTextColor = GhTextPrimary,
                        unfocusedTextColor = GhTextPrimary
                    )
                )
            } else {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Raw Link / First Split Link") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = GhSurface,
                        unfocusedContainerColor = GhSurface,
                        focusedTextColor = GhTextPrimary,
                        unfocusedTextColor = GhTextPrimary
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = !isSplit, onClick = { isSplit = false })
                Text("Single File", color = GhTextPrimary)
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(selected = isSplit, onClick = { isSplit = true })
                Text("Split Parts", color = GhTextPrimary)
            }

            if (isSplit) {
                OutlinedTextField(
                    value = partsCount,
                    onValueChange = { partsCount = it },
                    label = { Text("Number of Parts") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = GhSurface,
                        unfocusedContainerColor = GhSurface
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = autoUnzip, onCheckedChange = { autoUnzip = it })
                Text("Extract / Unzip after download", color = GhTextPrimary)
            }

            val isInputValid = if (isGithubMitmMode) {
                filePath.isNotEmpty() && appConfig?.repoUrl?.isNotEmpty() == true
            } else {
                url.isNotEmpty()
            }

            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    } else {
                        permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isInputValid && !isDownloading,
                colors = ButtonDefaults.buttonColors(containerColor = GhButtonPrimary)
            ) {
                Text("Start Process")
            }

            if (isDownloading || downloadFinished) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(currentFileStatus, color = GhTextPrimary)
                if (isDownloading) {
                    LinearProgressIndicator(
                        progress = progress / 100f,
                        modifier = Modifier.fillMaxWidth(),
                        color = GhButtonBlue
                    )
                }
            }


            val isSuccessState = currentFileStatus == "Process Complete!" || currentFileStatus == "Extracted Successfully!"

            if (downloadFinished && isSuccessState) {
                Button(
                    onClick = { openFileManagerSafe(context, autoUnzip, finalFileToOpen) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = GhButtonBlue)
                ) {
                    Text("Open in File Manager")
                }
            }
        }
    }

    if (showProxyDialog && appConfig != null) {
        SettingsDialog(
            currentConfig = appConfig!!,
            onDismiss = { showProxyDialog = false },
            onSave = { enabled, ip, port, threads, savedRepo ->
                scope.launch {
                    proxyDataStore.saveProxyConfig(enabled, ip, port, threads, savedRepo)
                    repoUrl = savedRepo
                    showProxyDialog = false
                }
            }
        )
    }
}

@Composable
fun SettingsDialog(
    currentConfig: com.arshiacomplus.rgit.data.preferences.AppConfig,
    onDismiss: () -> Unit,
    onSave: (Boolean, String, Int, Int, String) -> Unit
) {
    var isEnabled by remember { mutableStateOf(currentConfig.isEnabled) }
    var ip by remember { mutableStateOf(currentConfig.ip) }
    var port by remember { mutableStateOf(currentConfig.port.toString()) }
    var threads by remember { mutableStateOf(currentConfig.threads.toFloat()) }
    var repoUrlState by remember { mutableStateOf(currentConfig.repoUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GhSurface,
        title = { Text("Settings", color = GhTextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enable Proxy", color = GhTextPrimary)
                }
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("IP Address") }
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = repoUrlState, // * Use the state variable
                    onValueChange = { repoUrlState = it },
                    label = { Text("Default Git Repo URL") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Concurrent Downloads: ${threads.toInt()}", color = GhTextPrimary)
                Slider(
                    value = threads,
                    onValueChange = { threads = it },
                    valueRange = 1f..8f,
                    steps = 6,
                    colors = SliderDefaults.colors(
                        thumbColor = GhButtonBlue,
                        activeTrackColor = GhButtonBlue
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(isEnabled, ip, port.toIntOrNull() ?: 10808, threads.toInt(), repoUrlState)
            }) {
                Text("Save", color = GhButtonBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = GhTextSecondary)
            }
        }
    )
}

private fun openFileManagerSafe(context: Context, autoUnzip: Boolean, targetFile: File?) {
    try {
        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

        val intent = Intent(Intent.ACTION_VIEW)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION

        if (autoUnzip && targetFile != null) {
            val uri = android.net.Uri.fromFile(targetFile)
            val extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            val mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
            intent.setDataAndType(uri, mimeType)
            context.startActivity(intent)
        } else {
            val downloadDir = File(
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                "Rgit"
            )
            val uri = android.net.Uri.fromFile(downloadDir)
            intent.setDataAndType(uri, "*/*")
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        val fallback = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
        fallback.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        try {
            context.startActivity(fallback)
        } catch (e2: Exception) {
            e2.printStackTrace()
        }
    }
}