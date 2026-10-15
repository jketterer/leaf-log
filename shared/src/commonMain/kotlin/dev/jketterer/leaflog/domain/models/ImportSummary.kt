package dev.jketterer.leaflog.domain.models

import kotlin.time.Instant

/**
 * What importing a backup changes, counted in records as the app shows them: a row that arrives
 * already deleted isn't new, and one whose delete lands is removed. Everything else is unchanged.
 */
data class ImportSummary(
    val exportedAt: Instant,
    val added: Int,
    val updated: Int,
    val removed: Int,
    /** Rows left out because their tea, type or vessel is in neither the backup nor the app. */
    val skipped: Int = 0,
    /** The backup's settings replace this device's: only on a device that has never had a tea. */
    val restoresSettings: Boolean = false,
) {
    val changesNothing get() = added == 0 && updated == 0 && removed == 0 && !restoresSettings
}
