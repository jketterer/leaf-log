package dev.jketterer.leaflog.data.local

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class ImageStorage {
    private fun getImagesDir(subdirectory: String): String {
        val dir = NSHomeDirectory() + "/Documents/$subdirectory"
        NSFileManager.defaultManager.createDirectoryAtPath(
            dir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        return dir
    }

    actual fun resolveImagePath(path: String): String {
        if (path.startsWith("http")) return path
        if (!path.startsWith("/")) {
            return NSHomeDirectory() + "/Documents/$path"
        }
        val docsMarker = "/Documents/"
        val docsIndex = path.indexOf(docsMarker)
        if (docsIndex < 0) return path
        val relativePart = path.substring(docsIndex + docsMarker.length)
        return NSHomeDirectory() + "/Documents/$relativePart"
    }

    actual suspend fun saveImage(
        imageBytes: ByteArray,
        fileName: String,
        subdirectory: String
    ): String =
        withContext(Dispatchers.IO) {
            val destPath = "${getImagesDir(subdirectory)}/$fileName"
            val data = memScoped {
                NSData.create(bytes = allocArrayOf(imageBytes), length = imageBytes.size.toULong())
            }
            data.writeToFile(destPath, atomically = true)
            "$subdirectory/$fileName"
        }

    actual suspend fun deleteImage(path: String) = withContext(Dispatchers.IO) {
        NSFileManager.defaultManager.removeItemAtPath(resolveImagePath(path), error = null)
        Unit
    }

    actual suspend fun readImage(path: String): ByteArray? = withContext(Dispatchers.IO) {
        val data = NSData.create(contentsOfFile = resolveImagePath(path)) ?: return@withContext null
        val length = data.length.toInt()
        if (length == 0) return@withContext null
        ByteArray(length).also { bytes ->
            bytes.usePinned { pinned ->
                memcpy(pinned.addressOf(0), data.bytes, length.toULong())
            }
        }
    }

    actual fun getTempDir(): String = NSTemporaryDirectory()
}
