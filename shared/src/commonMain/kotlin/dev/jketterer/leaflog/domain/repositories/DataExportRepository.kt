package dev.jketterer.leaflog.domain.repositories

import dev.jketterer.leaflog.domain.models.ImportResult

interface DataExportRepository {
    /**
     * Exports all data to a ZIP file containing data.json and image files.
     * @return path to the temp ZIP file
     */
    suspend fun exportAll(): Result<String>

    /**
     * Imports data from a file at [filePath].
     * Supports both ZIP (new format with images) and plain JSON (legacy format).
     */
    suspend fun importFrom(filePath: String): Result<ImportResult>
}
