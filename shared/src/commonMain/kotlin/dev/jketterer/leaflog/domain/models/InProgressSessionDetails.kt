package dev.jketterer.leaflog.domain.models

/**
 * Enriched information about an in-progress session for display in the conflict resolution dialog.
 */
data class InProgressSessionDetails(
    val session: TeaSession,
    val teaName: String,
    val vesselName: String,
)
