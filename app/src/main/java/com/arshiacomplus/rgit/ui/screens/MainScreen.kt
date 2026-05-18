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
    var isSplit by remember { mutableStateOf(false) }
    var partsCount by remember { mutableStateOf("1") }
    var autoUnzip by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var currentFileStatus by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var showProxyDialog by remember { mutableStateOf(false) }
    var downloadFinished by remember { mutableStateOf(false) }
    var finalFileToOpen by remember { mutableStateOf<File?>(null) }
    val proxyConfig by proxyDataStore.proxySettingsFlow.collectAsState(initial = null)
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
                val downloader = RGitDownloader()
                val extractor = ZipExtractor()
                val count = if (isSplit) partsCount.toIntOrNull() ?: 1 else 1
                val downloadedFiles = downloader.startDownload(
                    originalUrl = url,
                    partsCount = count,
                    proxyConfig = proxyConfig ?: return@launch,
                    onProgress = { p, fileName ->
                        progress = p
                        currentFileStatus = "Downloading $fileName..."
                    }
                )
                if (downloadedFiles != null && downloadedFiles.isNotEmpty()) {
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
                            currentFileStatus = "Combining Failed!"
                        }
                    }
                    finalFileToOpen = finalFile
                    if (autoUnzip && currentFileStatus != "Combining Failed!") {
                        currentFileStatus = "Extracting files..."
                        val extSuccess = extractor.extract(finalFile, finalFile.parentFile)
                        currentFileStatus = if (extSuccess) "Extracted Successfully!" else "Extraction Failed!"
                    } else if (currentFileStatus != "Combining Failed!") {
                        currentFileStatus = "Process Complete!"
                    }
                } else {
                    currentFileStatus = "Download Failed!"
                }
                isDownloading = false
                downloadFinished = true
            }
        } else {
            currentFileStatus = "Storage Permission Denied!"
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
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    } else {
                        permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = url.isNotEmpty() && !isDownloading,
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
            if (downloadFinished) {
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
    if (showProxyDialog && proxyConfig != null) {
        ProxyDialog(
            currentConfig = proxyConfig!!,
            onDismiss = { showProxyDialog = false },
            onSave = { enabled, ip, port ->
                scope.launch {
                    proxyDataStore.saveProxyConfig(enabled, ip, port)
                    showProxyDialog = false
                }
            }
        )
    }
}
@Composable
fun ProxyDialog(
    currentConfig: com.arshiacomplus.rgit.data.preferences.ProxyConfig,
    onDismiss: () -> Unit,
    onSave: (Boolean, String, Int) -> Unit
) {
    var isEnabled by remember { mutableStateOf(currentConfig.isEnabled) }
    var ip by remember { mutableStateOf(currentConfig.ip) }
    var port by remember { mutableStateOf(currentConfig.port.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GhSurface,
        title = { Text("Proxy Settings", color = GhTextPrimary) },
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
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(isEnabled, ip, port.toIntOrNull() ?: 10808) }) {
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
            val folderUri = android.net.Uri.parse("content:
            intent.setDataAndType(folderUri, "vnd.android.document/directory")
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