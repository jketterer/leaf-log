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
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class ZipArchiver actual constructor() {

    actual suspend fun createZip(zipPath: String, entries: List<ZipEntry>): Unit = withContext(Dispatchers.IO) {
        val zipBytes = buildZipBytes(entries)
        val nsData = memScoped {
            NSData.create(bytes = allocArrayOf(zipBytes), length = zipBytes.size.toULong())
        }
        nsData.writeToFile(zipPath, atomically = true)
    }

    actual suspend fun extractZip(zipPath: String): List<ZipEntry> = withContext(Dispatchers.IO) {
        val nsData = NSData.create(contentsOfFile = zipPath)
            ?: throw IllegalStateException("Cannot read zip file: $zipPath")
        val length = nsData.length.toInt()
        val bytes = ByteArray(length)
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), nsData.bytes, length.toULong())
        }
        parseZipBytes(bytes)
    }

    actual suspend fun readFileHeader(filePath: String, byteCount: Int): ByteArray? = withContext(Dispatchers.IO) {
        val nsData = NSData.create(contentsOfFile = filePath) ?: return@withContext null
        val length = minOf(nsData.length.toInt(), byteCount)
        if (length == 0) return@withContext null
        val bytes = ByteArray(length)
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), nsData.bytes, length.toULong())
        }
        bytes
    }

    actual suspend fun readFile(filePath: String): ByteArray? = withContext(Dispatchers.IO) {
        val nsData = NSData.create(contentsOfFile = filePath) ?: return@withContext null
        val length = nsData.length.toInt()
        if (length == 0) return@withContext null
        val bytes = ByteArray(length)
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), nsData.bytes, length.toULong())
        }
        bytes
    }
}

// ---- Minimal ZIP format writer/reader using STORE method (no compression) ----

private fun buildZipBytes(entries: List<ZipEntry>): ByteArray {
    val localHeaders = mutableListOf<ByteArray>()
    val offsets = mutableListOf<Int>()
    var offset = 0

    // Build local file headers + data
    for (entry in entries) {
        val nameBytes = entry.name.encodeToByteArray()
        val header = buildLocalFileHeader(nameBytes, entry.data)
        localHeaders.add(header)
        offsets.add(offset)
        offset += header.size
    }

    // Build central directory
    val centralDir = ByteArray(0).toMutableList()
    for (i in entries.indices) {
        val nameBytes = entries[i].name.encodeToByteArray()
        val cdr = buildCentralDirectoryRecord(nameBytes, entries[i].data, offsets[i])
        centralDir.addAll(cdr.toList())
    }
    val centralDirBytes = centralDir.toByteArray()
    val centralDirOffset = offset

    // End of central directory record
    val eocd = buildEndOfCentralDirectory(
        entryCount = entries.size,
        centralDirSize = centralDirBytes.size,
        centralDirOffset = centralDirOffset,
    )

    // Combine all parts
    val result = ByteArray(offset + centralDirBytes.size + eocd.size)
    var pos = 0
    for (header in localHeaders) {
        header.copyInto(result, pos)
        pos += header.size
    }
    centralDirBytes.copyInto(result, pos)
    pos += centralDirBytes.size
    eocd.copyInto(result, pos)

    return result
}

private fun buildLocalFileHeader(nameBytes: ByteArray, data: ByteArray): ByteArray {
    val crc = crc32(data)
    val size = 30 + nameBytes.size + data.size
    val header = ByteArray(size)
    var pos = 0

    // Local file header signature
    writeInt32LE(header, pos, 0x04034b50); pos += 4
    // Version needed to extract (2.0)
    writeInt16LE(header, pos, 20); pos += 2
    // General purpose bit flag
    writeInt16LE(header, pos, 0); pos += 2
    // Compression method (0 = STORE)
    writeInt16LE(header, pos, 0); pos += 2
    // Last mod file time
    writeInt16LE(header, pos, 0); pos += 2
    // Last mod file date
    writeInt16LE(header, pos, 0); pos += 2
    // CRC-32
    writeInt32LE(header, pos, crc); pos += 4
    // Compressed size
    writeInt32LE(header, pos, data.size); pos += 4
    // Uncompressed size
    writeInt32LE(header, pos, data.size); pos += 4
    // File name length
    writeInt16LE(header, pos, nameBytes.size); pos += 2
    // Extra field length
    writeInt16LE(header, pos, 0); pos += 2
    // File name
    nameBytes.copyInto(header, pos); pos += nameBytes.size
    // File data
    data.copyInto(header, pos)

    return header
}

private fun buildCentralDirectoryRecord(nameBytes: ByteArray, data: ByteArray, localHeaderOffset: Int): ByteArray {
    val crc = crc32(data)
    val record = ByteArray(46 + nameBytes.size)
    var pos = 0

    // Central directory file header signature
    writeInt32LE(record, pos, 0x02014b50); pos += 4
    // Version made by
    writeInt16LE(record, pos, 20); pos += 2
    // Version needed to extract
    writeInt16LE(record, pos, 20); pos += 2
    // General purpose bit flag
    writeInt16LE(record, pos, 0); pos += 2
    // Compression method (STORE)
    writeInt16LE(record, pos, 0); pos += 2
    // Last mod file time
    writeInt16LE(record, pos, 0); pos += 2
    // Last mod file date
    writeInt16LE(record, pos, 0); pos += 2
    // CRC-32
    writeInt32LE(record, pos, crc); pos += 4
    // Compressed size
    writeInt32LE(record, pos, data.size); pos += 4
    // Uncompressed size
    writeInt32LE(record, pos, data.size); pos += 4
    // File name length
    writeInt16LE(record, pos, nameBytes.size); pos += 2
    // Extra field length
    writeInt16LE(record, pos, 0); pos += 2
    // File comment length
    writeInt16LE(record, pos, 0); pos += 2
    // Disk number start
    writeInt16LE(record, pos, 0); pos += 2
    // Internal file attributes
    writeInt16LE(record, pos, 0); pos += 2
    // External file attributes
    writeInt32LE(record, pos, 0); pos += 4
    // Relative offset of local header
    writeInt32LE(record, pos, localHeaderOffset); pos += 4
    // File name
    nameBytes.copyInto(record, pos)

    return record
}

private fun buildEndOfCentralDirectory(entryCount: Int, centralDirSize: Int, centralDirOffset: Int): ByteArray {
    val eocd = ByteArray(22)
    var pos = 0

    // End of central directory signature
    writeInt32LE(eocd, pos, 0x06054b50); pos += 4
    // Number of this disk
    writeInt16LE(eocd, pos, 0); pos += 2
    // Disk where central directory starts
    writeInt16LE(eocd, pos, 0); pos += 2
    // Number of central directory records on this disk
    writeInt16LE(eocd, pos, entryCount); pos += 2
    // Total number of central directory records
    writeInt16LE(eocd, pos, entryCount); pos += 2
    // Size of central directory
    writeInt32LE(eocd, pos, centralDirSize); pos += 4
    // Offset of start of central directory
    writeInt32LE(eocd, pos, centralDirOffset); pos += 4
    // Comment length
    writeInt16LE(eocd, pos, 0)

    return eocd
}

private fun parseZipBytes(bytes: ByteArray): List<ZipEntry> {
    val entries = mutableListOf<ZipEntry>()
    var pos = 0

    while (pos + 4 <= bytes.size) {
        val signature = readInt32LE(bytes, pos)
        if (signature != 0x04034b50) break // Not a local file header

        val compressionMethod = readInt16LE(bytes, pos + 8)
        val compressedSize = readInt32LE(bytes, pos + 18)
        val fileNameLength = readInt16LE(bytes, pos + 26)
        val extraFieldLength = readInt16LE(bytes, pos + 28)

        val nameStart = pos + 30
        val name = bytes.decodeToString(nameStart, nameStart + fileNameLength)

        val dataStart = nameStart + fileNameLength + extraFieldLength
        val data = bytes.copyOfRange(dataStart, dataStart + compressedSize)

        if (compressionMethod != 0) {
            throw IllegalStateException("Only STORE method is supported, got method $compressionMethod")
        }

        entries.add(ZipEntry(name = name, data = data))
        pos = dataStart + compressedSize
    }

    return entries
}

// ---- CRC-32 ----

private val crc32Table = IntArray(256) { n ->
    var c = n
    repeat(8) {
        c = if (c and 1 != 0) (c ushr 1) xor 0xEDB88320.toInt() else c ushr 1
    }
    c
}

private fun crc32(data: ByteArray): Int {
    var crc = 0.inv()
    for (b in data) {
        crc = (crc ushr 8) xor crc32Table[(crc xor b.toInt()) and 0xFF]
    }
    return crc.inv()
}

// ---- Little-endian helpers ----

private fun writeInt16LE(buf: ByteArray, offset: Int, value: Int) {
    buf[offset] = (value and 0xFF).toByte()
    buf[offset + 1] = ((value shr 8) and 0xFF).toByte()
}

private fun writeInt32LE(buf: ByteArray, offset: Int, value: Int) {
    buf[offset] = (value and 0xFF).toByte()
    buf[offset + 1] = ((value shr 8) and 0xFF).toByte()
    buf[offset + 2] = ((value shr 16) and 0xFF).toByte()
    buf[offset + 3] = ((value shr 24) and 0xFF).toByte()
}

private fun readInt16LE(buf: ByteArray, offset: Int): Int =
    (buf[offset].toInt() and 0xFF) or
        ((buf[offset + 1].toInt() and 0xFF) shl 8)

private fun readInt32LE(buf: ByteArray, offset: Int): Int =
    (buf[offset].toInt() and 0xFF) or
        ((buf[offset + 1].toInt() and 0xFF) shl 8) or
        ((buf[offset + 2].toInt() and 0xFF) shl 16) or
        ((buf[offset + 3].toInt() and 0xFF) shl 24)
