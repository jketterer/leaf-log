package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import kotlin.time.Clock
import kotlin.time.Instant

/** The instant @Preview data is built around, so a preview renders the same on every day. */
internal val PreviewInstant = Instant.fromEpochSeconds(1_790_000_000L)

/** Now, or [PreviewInstant] in a preview, so "today" and "3 days ago" don't move its screenshot. */
@Composable
internal fun currentInstant(): Instant =
    if (LocalInspectionMode.current) PreviewInstant else Clock.System.now()
