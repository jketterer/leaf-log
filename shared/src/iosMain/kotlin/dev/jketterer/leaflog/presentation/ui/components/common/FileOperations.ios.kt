package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import co.touchlab.kermit.Logger
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIViewController
import platform.UIKit.UIWindowScene
import platform.UniformTypeIdentifiers.UTTypeData
import platform.UniformTypeIdentifiers.UTTypeZIP
import platform.darwin.NSObject

@Composable
actual fun ExportFileEffect(
    exportFilePath: String?,
    shouldLaunch: Boolean,
    onLaunched: () -> Unit,
    onExported: () -> Unit,
    onCancelled: () -> Unit,
    onError: () -> Unit,
) {
    val exported by rememberUpdatedState(onExported)
    val cancelled by rememberUpdatedState(onCancelled)
    val failed by rememberUpdatedState(onError)

    LaunchedEffect(shouldLaunch) {
        if (!shouldLaunch || exportFilePath == null) return@LaunchedEffect
        onLaunched()

        val presenter = topViewController() ?: run {
            Logger.w(tag = "Backup") { "No view controller to present the share sheet" }
            onError()
            return@LaunchedEffect
        }
        // The file is already named for the user, so it's shared as it is
        val sheet = UIActivityViewController(
            activityItems = listOf(NSURL.fileURLWithPath(exportFilePath)),
            applicationActivities = null,
        )
        // Called once the sheet closes: only then is the file somewhere
        sheet.completionWithItemsHandler = { _, completed, _, error ->
            when {
                completed -> exported()
                error != null -> {
                    Logger.w(tag = "Backup") { "Sharing failed: ${error.localizedDescription}" }
                    failed()
                }
                else -> cancelled()
            }
        }
        presenter.presentViewController(sheet, animated = true, completion = null)
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
actual fun ImportFileLauncher(
    shouldLaunch: Boolean,
    onLaunched: () -> Unit,
    onFilePath: (String) -> Unit,
    onCancelled: () -> Unit,
    onError: () -> Unit,
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
        onLaunched()

        val presenter = topViewController() ?: run {
            Logger.w(tag = "Backup") { "No view controller to present the file picker" }
            onError()
            return@LaunchedEffect
        }
        val picker = UIDocumentPickerViewController(
            forOpeningContentTypes = listOf(UTTypeZIP, UTTypeData),
        )
        picker.delegate = delegate
        picker.allowsMultipleSelection = false
        presenter.presentViewController(picker, animated = true, completion = null)
    }
}

/** The view controller on top of the foreground window, which is the one that can present. */
private fun topViewController(): UIViewController? {
    val scene = UIApplication.sharedApplication.connectedScenes
        .filterIsInstance<UIWindowScene>()
        .firstOrNull { it.activationState == UISceneActivationStateForegroundActive }
    var controller = scene?.keyWindow?.rootViewController
    while (controller?.presentedViewController != null) {
        controller = controller.presentedViewController
    }
    return controller
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class DocumentPickerDelegate(
    private val onFilePath: (String) -> Unit,
    private val onCancelled: () -> Unit,
    private val onError: () -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        val sourcePath = url?.path
        if (url == null || sourcePath == null) {
            onCancelled()
            return
        }

        // Copied so the repository can read it by path once access to the original ends
        val tempPath = "${NSTemporaryDirectory()}leaflog_import_temp"
        val fileManager = NSFileManager.defaultManager
        if (fileManager.fileExistsAtPath(tempPath)) {
            fileManager.removeItemAtPath(tempPath, error = null)
        }
        val accessGranted = url.startAccessingSecurityScopedResource()
        val copied = try {
            fileManager.copyItemAtPath(sourcePath, toPath = tempPath, error = null)
        } finally {
            if (accessGranted) url.stopAccessingSecurityScopedResource()
        }

        if (copied) {
            onFilePath(tempPath)
        } else {
            Logger.w(tag = "Backup") { "Couldn't copy the picked file" }
            onError()
        }
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onCancelled()
    }
}
