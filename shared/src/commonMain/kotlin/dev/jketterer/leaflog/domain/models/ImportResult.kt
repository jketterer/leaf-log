package dev.jketterer.leaflog.domain.models

data class ImportResult(
    val teaTypesImported: Int,
    val brewingVesselsImported: Int,
    val teasImported: Int,
    val teaSessionsImported: Int,
    val configurationsImported: Int,
    /** Rows left out because their tea, type or vessel is in neither the backup nor the app. */
    val skipped: Int = 0,
)
