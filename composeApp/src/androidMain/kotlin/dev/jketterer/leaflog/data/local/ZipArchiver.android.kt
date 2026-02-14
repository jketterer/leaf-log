package dev.jketterer.leaflog.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry as JavaZipEntry

actual class ZipArchiver actual constructor() {
    actual suspend fun createZip(zipPath: String, entries: List<ZipEntry>) = withContext(Dispatchers.IO) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipPath))).use { zos ->
            for (entry in entries) {
                zos.putNextEntry(JavaZipEntry(entry.name))
                zos.write(entry.data)
                zos.closeEntry()
            }
        }
    }

    actual suspend fun extractZip(zipPath: String): List<ZipEntry> = withContext(Dispatchers.IO) {
        val result = mutableListOf<ZipEntry>()
        ZipInputStream(FileInputStream(zipPath)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val baos = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    var len: Int
                    while (zis.read(buffer).also { len = it } != -1) {
                        baos.write(buffer, 0, len)
                    }
                    result.add(ZipEntry(name = entry.name, data = baos.toByteArray()))
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        result
    }

    actual suspend fun readFileHeader(filePath: String, byteCount: Int): ByteArray? = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) return@withContext null
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(byteCount)
            val read = fis.read(buffer)
            if (read <= 0) null else buffer.copyOf(read)
        }
    }

    actual suspend fun readFile(filePath: String): ByteArray? = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) return@withContext null
        file.readBytes()
    }
}
