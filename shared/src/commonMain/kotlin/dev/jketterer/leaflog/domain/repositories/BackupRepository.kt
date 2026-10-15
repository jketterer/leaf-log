package dev.jketterer.leaflog.domain.repositories

import dev.jketterer.leaflog.domain.models.ImportResult

interface BackupRepository {
    /** Writes everything to a backup file and returns its path. */
    suspend fun export(): Result<String>

    /**
     * Restores the backup at [filePath] in one transaction: on failure nothing changes. The file
     * is consumed, and deleted afterward either way.
     */
    suspend fun import(filePath: String): Result<ImportResult>
}
