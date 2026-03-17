package dev.jketterer.leaflog.data.local

/**
 * Platform-specific ZIP archive operations.
 *
 * [createZip] writes a ZIP file at [zipPath] containing the given [entries].
 * [extractZip] reads a ZIP file at [zipPath] and returns all entries.
 */
expect class ZipArchiver() {
    /**
     * Creates a ZIP file at [zipPath] from the given [entries].
     * Each entry is a pair of (entryName, bytes).
     */
    suspend fun createZip(zipPath: String, entries: List<ZipEntry>)

    /**
     * Extracts all entries from the ZIP file at [zipPath].
     */
    suspend fun extractZip(zipPath: String): List<ZipEntry>

    /**
     * Reads the first few bytes of a file to check its format.
     */
    suspend fun readFileHeader(filePath: String, byteCount: Int): ByteArray?

    /**
     * Reads an entire file as bytes.
     */
    suspend fun readFile(filePath: String): ByteArray?
}

data class ZipEntry(
    val name: String,
    val data: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ZipEntry) return false
        return name == other.name && data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}
