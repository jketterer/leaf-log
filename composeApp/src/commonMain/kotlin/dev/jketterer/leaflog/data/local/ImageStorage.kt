package dev.jketterer.leaflog.data.local

expect class ImageStorage {
    suspend fun saveImage(imageBytes: ByteArray, fileName: String, subdirectory: String = "vessel_images"): String
    suspend fun deleteImage(path: String)
    suspend fun readImage(path: String): ByteArray?
    fun resolveImagePath(path: String): String
    fun getTempDir(): String
}
