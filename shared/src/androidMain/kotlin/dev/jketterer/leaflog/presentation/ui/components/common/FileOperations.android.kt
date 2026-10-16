package dev.jketterer.leaflog.presentation.ui.components.common

import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
actual fun ExportFileEffect(
    exportFilePath: String?,
    shouldLaunch: Boolean,
    onLaunched: () -> Unit,
    onExported: () -> Unit,
    onCancelled: () -> Unit,
    onError: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Read when the result arrives: after a recreation, that's in a new composition
    val source by rememberUpdatedState(exportFilePath)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri: Uri? ->
        val path = source
        if (uri == null || path == null) {
            onCancelled()
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val output = context.contentResolver.openOutputStream(uri)
                        ?: error("Couldn't open the file to save to")
                    output.use { File(path).inputStream().use { input -> input.copyTo(output) } }
                }
                onExported()
            } catch (e: Exception) {
                // The system created the file when it was picked; a partial one mustn't pass for
                // a backup
                runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri) }
                Logger.w(e, tag = "Backup") { "Couldn't save the export" }
                onError()
                if (e is CancellationException) throw e
            }
        }
    }

    LaunchedEffect(shouldLaunch) {
        if (shouldLaunch && exportFilePath != null) {
            onLaunched()
            launcher.launch(exportFilePath.substringAfterLast('/'))
        }
    }
}

@Composable
actual fun ImportFileLauncher(
    shouldLaunch: Boolean,
    onLaunched: () -> Unit,
    onFilePath: (String) -> Unit,
    onCancelled: () -> Unit,
    onError: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) {
            onCancelled()
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            try {
                // Copied so the repository can read it by path
                val path = withContext(Dispatchers.IO) {
                    val temp = File(context.cacheDir, "leaflog_import_temp")
                    val input = context.contentResolver.openInputStream(uri)
                        ?: error("Couldn't open the file")
                    input.use { temp.outputStream().use { output -> input.copyTo(output) } }
                    temp.absolutePath
                }
                onFilePath(path)
            } catch (e: Exception) {
                Logger.w(e, tag = "Backup") { "Couldn't copy the picked file" }
                onError()
                if (e is CancellationException) throw e
            }
        }
    }

    LaunchedEffect(shouldLaunch) {
        if (shouldLaunch) {
            onLaunched()
            // Some providers label a ZIP as something else, so anything can be picked
            launcher.launch(arrayOf("application/zip", "*/*"))
        }
    }
}
