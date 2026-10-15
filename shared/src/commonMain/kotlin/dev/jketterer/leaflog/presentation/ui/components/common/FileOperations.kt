package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.runtime.Composable

/**
 * Delivers the backup at [exportFilePath] when [shouldLaunch]: Android saves it where the user
 * picks, iOS shares it. [onLaunched] fires as the system UI opens, so the caller can clear
 * [shouldLaunch] and a recreated screen doesn't open it twice. [onExported] means the file
 * reached its destination.
 */
@Composable
expect fun ExportFileEffect(
    exportFilePath: String?,
    shouldLaunch: Boolean,
    onLaunched: () -> Unit,
    onExported: () -> Unit,
    onCancelled: () -> Unit,
    onError: (String) -> Unit,
)

/** Lets the user pick a backup, copied to a path the app can read. [onLaunched] as above. */
@Composable
expect fun ImportFileLauncher(
    shouldLaunch: Boolean,
    onLaunched: () -> Unit,
    onFilePath: (String) -> Unit,
    onCancelled: () -> Unit,
    onError: (String) -> Unit,
)
