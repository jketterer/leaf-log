package dev.jketterer.leaflog.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class ImageStorage(private val context: Context) {
    private val imagesDir: File
        get() = File(context.filesDir, "vessel_images").also { it.mkdirs() }

    actual suspend fun saveImage(imageBytes: ByteArray, fileName: String): String =
        withContext(Dispatchers.IO) {
            val destFile = File(imagesDir, fileName)
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
}
