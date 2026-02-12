package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.runtime.Composable

@Composable
expect fun ExportFileEffect(
    jsonContent: String?,
    fileName: String,
    onExported: () -> Unit,
    onError: (String) -> Unit,
)

@Composable
expect fun ImportFileLauncher(
    shouldLaunch: Boolean,
    onFileContent: (String) -> Unit,
    onCancelled: () -> Unit,
    onError: (String) -> Unit,
)
