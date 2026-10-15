package dev.jketterer.leaflog.domain.repositories

import dev.jketterer.leaflog.domain.models.ImportSummary

interface BackupRepository {
    /**
     * Writes everything to a backup file, named for the user to keep, and returns its path. Pass
     * it to [discard] once delivered.
     */
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

    /** Deletes a backup file once it's done with: an export delivered, or a preview not imported. */
    suspend fun discard(filePath: String)

    /** Bytes the photo files on this device take: roughly what an export adds for them. */
    suspend fun photoStorageBytes(): Result<Long>
}
