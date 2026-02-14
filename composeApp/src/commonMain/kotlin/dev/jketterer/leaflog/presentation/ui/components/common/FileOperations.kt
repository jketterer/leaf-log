package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.runtime.Composable

@Composable
expect fun ExportFileEffect(
    exportFilePath: String?,
    fileName: String,
    onExported: () -> Unit,
    onError: (String) -> Unit,
)

@Composable
expect fun ImportFileLauncher(
    shouldLaunch: Boolean,
    onFilePath: (String) -> Unit,
    onCancelled: () -> Unit,
    onError: (String) -> Unit,
)
