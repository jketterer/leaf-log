package dev.jketterer.leaflog.data.local

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.openZip
import okio.use
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ZipWriterTest {

    private val fileSystem = FileSystem.SYSTEM
    private val dir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "zip-writer-test-${Random.nextLong()}")
        .also { fileSystem.createDirectories(it) }

    // Bigger than the writer's 8 KB chunk, so the CRC runs across several reads
    private val photo = Random(1).nextBytes(20_000)
    private val json = """{"teas":[]}""".encodeToByteArray()

    @AfterTest
    fun cleanUp() = fileSystem.deleteRecursively(dir)

    @Test
    fun `okio reads back what the writer wrote`() {
        val zipPath = writeBackup()

        val zip = fileSystem.openZip(zipPath)

        assertContentEquals(json, zip.read("/data.json".toPath()) { readByteArray() })
        assertContentEquals(photo, zip.read("/images/a.jpg".toPath()) { readByteArray() })
    }

    @Test
    fun `java's zip reader accepts the writer's headers and CRCs`() {
        val entries = mutableMapOf<String, ByteArray>()

        // ZipInputStream reads local headers front to back and fails a STORE entry on a bad CRC
        ZipInputStream(fileSystem.read(writeBackup()) { readByteArray() }.inputStream()).use {
            while (true) {
                val entry = it.nextEntry ?: break
                entries[entry.name] = it.readBytes()
            }
        }

        assertEquals(setOf("data.json", "images/a.jpg"), entries.keys)
        assertContentEquals(photo, entries["images/a.jpg"])
    }

    @Test
    fun `an Android backup written with DEFLATE and data descriptors reads on the common path`() {
        val zipPath = dir / "android.zip"
        // ZipOutputStream's defaults are what Android exported for backup version 1
        ZipOutputStream(fileSystem.sink(zipPath).buffer().outputStream()).use {
            it.putNextEntry(ZipEntry("data.json"))
            it.write(json)
            it.putNextEntry(ZipEntry("images/session_photos/0.jpg"))
            it.write(photo)
        }

        val zip = fileSystem.openZip(zipPath)

        assertContentEquals(json, zip.read("/data.json".toPath()) { readByteArray() })
        assertContentEquals(photo, zip.read("/images/session_photos/0.jpg".toPath()) { readByteArray() })
    }

    private fun writeBackup() = (dir / "backup.zip").also { zipPath ->
        val photoFile = dir / "a.jpg"
        fileSystem.write(photoFile) { write(photo) }
        ZipWriter(fileSystem, zipPath).use {
            it.add("data.json", json)
            it.add("images/a.jpg", photoFile)
        }
    }
}
