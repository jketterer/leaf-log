package dev.jketterer.leaflog.presentation.ui.components.common

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileInputStream

@Composable
actual fun ExportFileEffect(
    exportFilePath: String?,
    fileName: String,
    onExported: () -> Unit,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    var pendingFilePath by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri: Uri? ->
        if (uri != null && pendingFilePath != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    FileInputStream(File(pendingFilePath!!)).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                onExported()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to write file")
            }
        } else {
            onExported()
        }
        pendingFilePath = null
    }

    LaunchedEffect(exportFilePath) {
        if (exportFilePath != null) {
            pendingFilePath = exportFilePath
            launcher.launch(fileName)
        }
    }
}

@Composable
actual fun ImportFileLauncher(
    shouldLaunch: Boolean,
    onFilePath: (String) -> Unit,
    onCancelled: () -> Unit,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // Copy the selected file to a temp location so the use case can read it by path
                val tempFile = File(context.cacheDir, "leaflog_import_temp")
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    tempFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                onFilePath(tempFile.absolutePath)
            } catch (e: Exception) {
                onError(e.message ?: "Failed to read file")
            }
        } else {
            onCancelled()
        }
    }

    LaunchedEffect(shouldLaunch) {
        if (shouldLaunch) {
            launcher.launch(arrayOf("application/zip", "application/json", "*/*"))
        }
    }
}
