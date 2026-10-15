package dev.jketterer.leaflog.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import kotlin.math.max
import kotlin.math.roundToInt

internal const val MAX_PHOTO_EDGE = 2048
private const val PHOTO_JPEG_QUALITY = 85

actual class ImageStorage(private val context: Context) {
    private fun getImagesDir(subdirectory: String): File =
        File(context.filesDir, subdirectory).also { it.mkdirs() }

    actual fun resolveImagePath(path: String): String =
        if (path.startsWith("/") || path.startsWith("http")) path
        else File(context.filesDir, path).absolutePath

    actual suspend fun saveImage(imageBytes: ByteArray, fileName: String, subdirectory: String): String =
        withContext(Dispatchers.IO) {
            val destFile = File(getImagesDir(subdirectory), fileName)
            destFile.writeBytes(downscaleToJpeg(imageBytes))
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

/** Re-encodes a picked photo as an upright JPEG no longer than [MAX_PHOTO_EDGE] on either side. */
private fun downscaleToJpeg(bytes: ByteArray): ByteArray {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        throw IOException("This image can't be read")
    }
    // Decoding at a fraction of full size keeps a 50 MP photo from filling the heap
    val options = BitmapFactory.Options().apply {
        inSampleSize = photoSampleSize(bounds.outWidth, bounds.outHeight)
    }
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        ?: throw IOException("This image can't be read")
    val (width, height) = photoTargetSize(decoded.width, decoded.height)
    val matrix = Matrix().apply {
        setScale(width.toFloat() / decoded.width, height.toFloat() / decoded.height)
        postConcat(exifOrientation(bytes))
    }
    val upright = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    return ByteArrayOutputStream().use { out ->
        upright.compress(Bitmap.CompressFormat.JPEG, PHOTO_JPEG_QUALITY, out)
        out.toByteArray()
    }
}

/** BitmapFactory ignores EXIF orientation, so the rotation a camera recorded is applied here. */
private fun exifOrientation(bytes: ByteArray): Matrix {
    val orientation = try {
        ExifInterface(ByteArrayInputStream(bytes))
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } catch (_: IOException) {
        ExifInterface.ORIENTATION_NORMAL
    }
    return Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                postScale(-1f, 1f)
                postRotate(270f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                postScale(-1f, 1f)
                postRotate(90f)
            }
        }
    }
}

/** The largest power of two that still decodes [width] × [height] at [MAX_PHOTO_EDGE] or more. */
internal fun photoSampleSize(width: Int, height: Int): Int {
    var sample = 1
    while (max(width, height) / (sample * 2) >= MAX_PHOTO_EDGE) sample *= 2
    return sample
}

/** [width] × [height] shrunk to fit [MAX_PHOTO_EDGE], keeping its shape. Never enlarged. */
internal fun photoTargetSize(width: Int, height: Int): Pair<Int, Int> {
    val longEdge = max(width, height)
    if (longEdge <= MAX_PHOTO_EDGE) return width to height
    val scale = MAX_PHOTO_EDGE.toDouble() / longEdge
    return max(1, (width * scale).roundToInt()) to max(1, (height * scale).roundToInt())
}
