package dev.jketterer.leaflog.data.local

import okio.Buffer
import okio.BufferedSink
import okio.ByteString.Companion.encodeUtf8
import okio.Closeable
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.use

/**
 * Writes a ZIP whose entries are all STORE: photos don't compress, and every reader handles it.
 * Read it back with okio's `FileSystem.openZip`.
 *
 * A STORE entry's header carries its CRC, so [add] reads a file twice: once for the CRC, once to
 * copy it. Nothing is held in memory but the central directory.
 */
class ZipWriter(private val fileSystem: FileSystem, zipPath: Path) : Closeable {
    private val sink = fileSystem.sink(zipPath).buffer()
    private val centralDirectory = Buffer()
    private var offset = 0L
    private var entryCount = 0

    fun add(name: String, bytes: ByteArray) {
        val crc = Crc32().apply { update(bytes, bytes.size) }
        add(name, bytes.size.toLong(), crc.value) { it.write(bytes) }
    }

    fun add(name: String, file: Path) {
        val crc = Crc32()
        var size = 0L
        fileSystem.read(file) {
            val chunk = ByteArray(8192)
            while (true) {
                val read = read(chunk)
                if (read == -1) break
                crc.update(chunk, read)
                size += read
            }
        }
        add(name, size, crc.value) { sink -> fileSystem.source(file).use { sink.writeAll(it) } }
    }

    private inline fun add(name: String, size: Long, crc: Int, writeData: (BufferedSink) -> Unit) {
        val nameBytes = name.encodeUtf8()
        // ponytail: no zip64, so 4 GB and 65535 entries in all; years of photos fit well under
        require(offset + 30 + nameBytes.size + size <= 0xFFFFFFFFL) { "Backup is over 4 GB" }
        require(entryCount < 0xFFFF) { "Backup has too many files" }

        sink.writeIntLe(LOCAL_HEADER)
        sink.writeHeaderFields(nameBytes.size, size, crc)
        sink.write(nameBytes)
        writeData(sink)

        centralDirectory.writeIntLe(CENTRAL_HEADER)
        centralDirectory.writeShortLe(VERSION)
        centralDirectory.writeHeaderFields(nameBytes.size, size, crc)
        centralDirectory.writeShortLe(0) // comment length
        centralDirectory.writeShortLe(0) // disk number
        centralDirectory.writeShortLe(0) // internal attributes
        centralDirectory.writeIntLe(0) // external attributes
        centralDirectory.writeIntLe(offset.toInt())
        centralDirectory.write(nameBytes)

        offset += 30 + nameBytes.size + size
        entryCount++
    }

    /** The fields local and central headers share, from "version needed" to "extra length". */
    private fun BufferedSink.writeHeaderFields(nameLength: Int, size: Long, crc: Int) {
        writeShortLe(VERSION)
        writeShortLe(UTF8_NAMES)
        writeShortLe(0) // STORE
        writeShortLe(0) // time: midnight
        writeShortLe(DOS_EPOCH)
        writeIntLe(crc)
        writeIntLe(size.toInt()) // compressed
        writeIntLe(size.toInt()) // uncompressed
        writeShortLe(nameLength)
        writeShortLe(0) // extra length
    }

    override fun close() {
        val centralDirectorySize = centralDirectory.size
        sink.writeAll(centralDirectory)
        sink.writeIntLe(END_OF_CENTRAL_DIRECTORY)
        sink.writeShortLe(0) // this disk
        sink.writeShortLe(0) // disk with the central directory
        sink.writeShortLe(entryCount)
        sink.writeShortLe(entryCount)
        sink.writeIntLe(centralDirectorySize.toInt())
        sink.writeIntLe(offset.toInt())
        sink.writeShortLe(0) // comment length
        sink.close()
    }

    private companion object {
        const val LOCAL_HEADER = 0x04034b50
        const val CENTRAL_HEADER = 0x02014b50
        const val END_OF_CENTRAL_DIRECTORY = 0x06054b50
        const val VERSION = 20
        const val UTF8_NAMES = 1 shl 11
        const val DOS_EPOCH = (1 shl 5) or 1 // 1980-01-01; a zero date is invalid to some readers
    }
}

private class Crc32 {
    private var crc = 0.inv()
    val value get() = crc.inv()

    fun update(bytes: ByteArray, count: Int) {
        for (i in 0 until count) {
            crc = (crc ushr 8) xor table[(crc xor bytes[i].toInt()) and 0xFF]
        }
    }

    private companion object {
        val table = IntArray(256) { n ->
            var c = n
            repeat(8) { c = if (c and 1 != 0) (c ushr 1) xor 0xEDB88320.toInt() else c ushr 1 }
            c
        }
    }
}
