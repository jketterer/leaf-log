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

@Composable
actual fun ExportFileEffect(
    jsonContent: String?,
    fileName: String,
    onExported: () -> Unit,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    var pendingContent by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        if (uri != null && pendingContent != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(pendingContent!!.toByteArray(Charsets.UTF_8))
                }
                onExported()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to write file")
            }
        } else {
            onExported()
        }
        pendingContent = null
    }

    LaunchedEffect(jsonContent) {
        if (jsonContent != null) {
            pendingContent = jsonContent
            launcher.launch(fileName)
        }
    }
}

@Composable
actual fun ImportFileLauncher(
    shouldLaunch: Boolean,
    onFileContent: (String) -> Unit,
    onCancelled: () -> Unit,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader(Charsets.UTF_8).readText()
                }
                if (content != null) {
                    onFileContent(content)
                } else {
                    onError("Failed to read file")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to read file")
            }
        } else {
            onCancelled()
        }
    }

    LaunchedEffect(shouldLaunch) {
        if (shouldLaunch) {
            launcher.launch(arrayOf("application/json", "*/*"))
        }
    }
}
