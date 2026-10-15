package dev.jketterer.leaflog.domain.repositories

import dev.jketterer.leaflog.domain.models.ImportSummary

interface BackupRepository {
    /** Writes everything to a backup file and returns its path. */
    suspend fun export(): Result<String>

    /**
     * What importing the backup at [filePath] would change, without changing anything. The file
     * is kept for [import] or [discard], unless it can't be read: then it's deleted.
     */
    suspend fun preview(filePath: String): Result<ImportSummary>

    /**
     * Restores the backup at [filePath] in one transaction: on failure nothing changes. The file
     * is consumed, and deleted afterward either way.
     */
    suspend fun import(filePath: String): Result<ImportSummary>

    /** Deletes a previewed backup that won't be imported. */
    suspend fun discard(filePath: String)
}
