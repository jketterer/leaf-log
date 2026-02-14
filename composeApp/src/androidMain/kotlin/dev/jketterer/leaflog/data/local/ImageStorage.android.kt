package dev.jketterer.leaflog.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class ImageStorage(private val context: Context) {
    private fun getImagesDir(subdirectory: String): File =
        File(context.filesDir, subdirectory).also { it.mkdirs() }

    actual suspend fun saveImage(imageBytes: ByteArray, fileName: String, subdirectory: String): String =
        withContext(Dispatchers.IO) {
            val destFile = File(getImagesDir(subdirectory), fileName)
            destFile.writeBytes(imageBytes)
            destFile.absolutePath
        }

    actual suspend fun deleteImage(path: String) = withContext(Dispatchers.IO) {
        val file = File(path)
        if (file.exists()) {
            file.delete()
        }
        Unit
    }

    actual suspend fun readImage(path: String): ByteArray? = withContext(Dispatchers.IO) {
        File(path).takeIf { it.exists() }?.readBytes()
    }

    actual fun getTempDir(): String = context.cacheDir.absolutePath
}
