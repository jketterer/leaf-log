package dev.jketterer.leaflog.data.local

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.create
import platform.Foundation.writeToFile

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class ImageStorage {
    private val imagesDir: String
        get() {
            val dir = NSHomeDirectory() + "/Documents/vessel_images"
            NSFileManager.defaultManager.createDirectoryAtPath(
                dir,
                withIntermediateDirectories = true,
                attributes = null,
                error = null,
            )
            return dir
        }

    actual suspend fun saveImage(imageBytes: ByteArray, fileName: String): String =
        withContext(Dispatchers.IO) {
            val destPath = "$imagesDir/$fileName"
            val data = memScoped {
                NSData.create(bytes = allocArrayOf(imageBytes), length = imageBytes.size.toULong())
            }
            data.writeToFile(destPath, atomically = true)
            destPath
        }

    actual suspend fun deleteImage(path: String) = withContext(Dispatchers.IO) {
        NSFileManager.defaultManager.removeItemAtPath(path, error = null)
        Unit
    }
}
