package dev.jketterer.leaflog.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class ImageStorage(private val context: Context) {
    private fun getImagesDir(subdirectory: String): File =
        File(context.filesDir, subdirectory).also { it.mkdirs() }

    actual fun resolveImagePath(path: String): String =
        if (path.startsWith("/") || path.startsWith("http")) path
        else File(context.filesDir, path).absolutePath

    actual suspend fun saveImage(imageBytes: ByteArray, fileName: String, subdirectory: String): String =
        withContext(Dispatchers.IO) {
            val destFile = File(getImagesDir(subdirectory), fileName)
            destFile.writeBytes(imageBytes)
            "$subdirectory/$fileName"
        }

    actual suspend fun deleteImage(path: String) = withContext(Dispatchers.IO) {
        val file = File(resolveImagePath(path))
        if (file.exists()) {
            file.delete()
        }
        Unit
    }

    actual suspend fun readImage(path: String): ByteArray? = withContext(Dispatchers.IO) {
        File(resolveImagePath(path)).takeIf { it.exists() }?.readBytes()
    }

    actual fun getTempDir(): String = context.cacheDir.absolutePath
}
