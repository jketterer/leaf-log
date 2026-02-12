package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfURL
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
actual fun ExportFileEffect(
    jsonContent: String?,
    fileName: String,
    onExported: () -> Unit,
    onError: (String) -> Unit,
) {
    LaunchedEffect(jsonContent) {
        if (jsonContent == null) return@LaunchedEffect

        try {
            val tempDir = NSTemporaryDirectory()
            val filePath = "$tempDir$fileName"
            val nsString = NSString.create(string = jsonContent)
            nsString.writeToFile(filePath, atomically = true, encoding = NSUTF8StringEncoding, error = null)

            val fileUrl = NSURL.fileURLWithPath(filePath)
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
    onFileContent: (String) -> Unit,
    onCancelled: () -> Unit,
    onError: (String) -> Unit,
) {
    val delegate = remember {
        DocumentPickerDelegate(
            onFileContent = onFileContent,
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
            forOpeningContentTypes = listOf(UTTypeJSON),
        )
        picker.delegate = delegate
        picker.allowsMultipleSelection = false
        rootViewController.presentViewController(picker, animated = true, completion = null)
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class DocumentPickerDelegate(
    private val onFileContent: (String) -> Unit,
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
            val content = NSString.stringWithContentsOfURL(
                url = url,
                encoding = NSUTF8StringEncoding,
                error = null,
            )
            if (content != null) {
                onFileContent(content)
            } else {
                onError("Failed to read file content")
            }
        } catch (e: Exception) {
            onError(e.message ?: "Failed to read file")
        }
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onCancelled()
    }
}
