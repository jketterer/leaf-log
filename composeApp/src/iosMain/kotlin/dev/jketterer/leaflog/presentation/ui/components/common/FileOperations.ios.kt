package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeData
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.UniformTypeIdentifiers.UTTypeZIP
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
actual fun ExportFileEffect(
    exportFilePath: String?,
    fileName: String,
    onExported: () -> Unit,
    onError: (String) -> Unit,
) {
    LaunchedEffect(exportFilePath) {
        if (exportFilePath == null) return@LaunchedEffect

        try {
            // Copy temp zip to a named file for sharing
            val tempDir = NSTemporaryDirectory()
            val namedPath = "$tempDir$fileName"
            val fileManager = NSFileManager.defaultManager
            // Remove old file if exists
            if (fileManager.fileExistsAtPath(namedPath)) {
                fileManager.removeItemAtPath(namedPath, error = null)
            }
            fileManager.copyItemAtPath(exportFilePath, toPath = namedPath, error = null)

            val fileUrl = NSURL.fileURLWithPath(namedPath)
            val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
                ?: run {
                    onError("Could not find root view controller")
                    return@LaunchedEffect
                }

            val activityViewController = UIActivityViewController(
                activityItems = listOf(fileUrl),
                applicationActivities = null,
            )
            rootViewController.presentViewController(activityViewController, animated = true, completion = null)
            onExported()
        } catch (e: Exception) {
            onError(e.message ?: "Failed to export file")
        }
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
actual fun ImportFileLauncher(
    shouldLaunch: Boolean,
    onFilePath: (String) -> Unit,
    onCancelled: () -> Unit,
    onError: (String) -> Unit,
) {
    val delegate = remember {
        DocumentPickerDelegate(
            onFilePath = onFilePath,
            onCancelled = onCancelled,
            onError = onError,
        )
    }

    LaunchedEffect(shouldLaunch) {
        if (!shouldLaunch) return@LaunchedEffect

        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            ?: run {
                onError("Could not find root view controller")
                return@LaunchedEffect
            }

        val picker = UIDocumentPickerViewController(
            forOpeningContentTypes = listOf(UTTypeZIP, UTTypeJSON, UTTypeData),
        )
        picker.delegate = delegate
        picker.allowsMultipleSelection = false
        rootViewController.presentViewController(picker, animated = true, completion = null)
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class DocumentPickerDelegate(
    private val onFilePath: (String) -> Unit,
    private val onCancelled: () -> Unit,
    private val onError: (String) -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        if (url == null) {
            onCancelled()
            return
        }

        try {
            // Copy to temp to ensure we have file access
            val tempDir = NSTemporaryDirectory()
            val tempPath = "${tempDir}leaflog_import_temp"
            val fileManager = NSFileManager.defaultManager
            if (fileManager.fileExistsAtPath(tempPath)) {
                fileManager.removeItemAtPath(tempPath, error = null)
            }

            val sourceAccessGranted = url.startAccessingSecurityScopedResource()
            try {
                val sourcePath = url.path ?: run {
                    onError("Invalid file URL")
                    return
                }
                fileManager.copyItemAtPath(sourcePath, toPath = tempPath, error = null)
            } finally {
                if (sourceAccessGranted) {
                    url.stopAccessingSecurityScopedResource()
                }
            }

            onFilePath(tempPath)
        } catch (e: Exception) {
            onError(e.message ?: "Failed to read file")
        }
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onCancelled()
    }
}
