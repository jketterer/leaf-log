package dev.jketterer.leaflog.data.local

expect class ImageStorage {
    suspend fun saveImage(imageBytes: ByteArray, fileName: String): String
    suspend fun deleteImage(path: String)
}
