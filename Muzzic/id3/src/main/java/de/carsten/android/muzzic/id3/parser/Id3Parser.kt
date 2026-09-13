package de.carsten.android.muzzic.id3.parser

import de.carsten.android.muzzic.id3.exceptions.Id3Exception
import de.carsten.android.muzzic.id3.model.frame.FrameFormatFlags
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.frame.FrameId
import de.carsten.android.muzzic.id3.model.frame.FrameStatusFlags
import de.carsten.android.muzzic.id3.model.frame.Id3Frame
import de.carsten.android.muzzic.id3.model.frame.UnknownFrame
import de.carsten.android.muzzic.id3.model.tag.DataBytes
import de.carsten.android.muzzic.id3.model.tag.Id3Tag
import de.carsten.android.muzzic.id3.model.tag.Id3Version
import de.carsten.android.muzzic.id3.model.tag.SynchsafeInt
import de.carsten.android.muzzic.id3.model.tag.TagExtendedHeader
import de.carsten.android.muzzic.id3.model.tag.TagFooter
import de.carsten.android.muzzic.id3.model.tag.TagHeader
import de.carsten.android.muzzic.id3.model.tag.TagHeaderFlags
import de.carsten.android.muzzic.id3.model.tag.TagRestrictions
import java.io.EOFException
import java.io.InputStream

/**
 * Skeleton parser turning raw tag bytes into the typed [Id3Tag] model.
 *
 * Already functional for plain tags: header, both extended header
 * variants, frame headers with format-flag extras, unsynchronisation
 * reversal and footer validation. Bodies decode through
 * [FrameDecoderRegistry]; complex layouts without a registered decoder
 * surface as [UnknownFrame].
 *
 * Known skeleton limitations, documented for the follow-up parser task:
 * compressed or encrypted bodies are preserved as [UnknownFrame] instead
 * of being transformed. A missing footer identifier throws; only a
 * deviating footer version falls back to the header version.
 */
object Id3Parser {
    /**
     * Parses a complete tag from the stream positioned at its header.
     *
     * @param input source positioned at the first "ID3" byte.
     * @return parsed tag.
     * @throws Id3Exception on malformed data or I/O errors.
     */
    fun parse(input: InputStream): Id3Tag {
        val header = parseHeader(input)
        val body = readFully(input, header.tagSize, "tag body")
        var index = 0
        val (extendedHeader, headerConsumed) = parseExtendedHeader(header, body, index)
        index += headerConsumed
        val frames = mutableListOf<Id3Frame>()
        var paddingSize = 0
        while (index < body.size) {
            if (body[index] == 0x00.toByte()) {
                paddingSize = body.size - index
                break
            }
            if (body.size - index < FrameHeader.SIZE) {
                paddingSize = body.size - index
                break
            }
            val (frameHeader, headerLength) = parseFrameHeader(header.version, body, index)
            index += headerLength
            if (frameHeader.size < 0 || index + frameHeader.size > body.size) {
                throw Id3Exception("Frame ${frameHeader.frameId} size exceeds tag body")
            }
            val bodyOffset = TagHeader.SIZE + index
            var frameBody = body.copyOfRange(index, index + frameHeader.size)
            index += frameHeader.size
            frameBody = maybeReverseUnsynchronisation(header, frameHeader, frameBody)
            frames.add(decodeBody(frameHeader, frameBody, bodyOffset))
        }
        val footer = parseFooter(header, input)
        return Id3Tag(
            version = header.version,
            header = header,
            extendedHeader = extendedHeader,
            frames = frames,
            paddingSize = paddingSize,
            footer = footer,
        )
    }

    /**
     * Parses the 10 byte tag header.
     *
     * @param input source positioned at the first "ID3" byte.
     * @return parsed header.
     * @throws Id3Exception if the identifier or version is unsupported.
     */
    fun parseHeader(input: InputStream): TagHeader {
        val bytes = readFully(input, TagHeader.SIZE, "tag header")
        if (bytes[0] != 'I'.code.toByte() || bytes[1] != 'D'.code.toByte() || bytes[2] != '3'.code.toByte()) {
            throw Id3Exception("Missing ID3 file identifier")
        }
        val version = Id3Version.from(bytes[3], bytes[4])
            ?: throw Id3Exception("Unsupported ID3 version ${bytes[3]}.${bytes[4]}")
        return TagHeader(
            version = version,
            flags = TagHeaderFlags.fromByte(version, bytes[5]),
            tagSize = SynchsafeInt.decode(bytes, 6),
        )
    }

    /**
     * Reverses the unsynchronisation scheme.
     *
     * Every inserted $00 after $FF is removed again.
     *
     * @param bytes unsynchronised bytes.
     * @return plain bytes.
     */
    fun reverseUnsynchronisation(bytes: ByteArray): ByteArray {
        val plain = ByteArray(bytes.size)
        var write = 0
        var read = 0
        while (read < bytes.size) {
            val current = bytes[read]
            plain[write++] = current
            read += 1
            if (current == 0xFF.toByte() && read < bytes.size && bytes[read] == 0x00.toByte()) read += 1
        }
        return plain.copyOf(write)
    }

    private fun parseExtendedHeader(header: TagHeader, body: ByteArray, offset: Int): Pair<TagExtendedHeader?, Int> {
        if (!header.flags.extendedHeader) return null to 0
        return when (header.version) {
            Id3Version.V2_3 -> parseV23ExtendedHeader(body, offset)
            Id3Version.V2_4 -> parseV24ExtendedHeader(body, offset)
        }
    }

    private fun parseV23ExtendedHeader(body: ByteArray, offset: Int): Pair<TagExtendedHeader, Int> {
        if (body.size - offset < 10) throw Id3Exception("Truncated v2.3 extended header")
        val size = readU32(body, offset)
        val crcPresent = body[offset + 4].toInt() and 0x80 != 0
        val paddingSize = readU32(body, offset + 6)
        val crc = if (crcPresent) readU32(body, offset + 10).toUInt() else null
        return TagExtendedHeader.V23(paddingSize = paddingSize, crc = crc) to (MIN_V23_EXTENDED_HEADER + size)
    }

    private fun parseV24ExtendedHeader(body: ByteArray, offset: Int): Pair<TagExtendedHeader, Int> {
        if (body.size - offset < MIN_V24_EXTENDED_HEADER) throw Id3Exception("Truncated v2.4 extended header")
        val totalSize = SynchsafeInt.decode(body, offset)
        if (totalSize < MIN_V24_EXTENDED_HEADER) throw Id3Exception("Invalid v2.4 extended header size $totalSize")
        var index = offset + 4
        val flagBytes = body[index++].toInt() and 0xFF
        val end = offset + totalSize
        var isUpdate = false
        var crc: UInt? = null
        var restrictions: TagRestrictions? = null
        repeat(flagBytes.coerceAtMost(1)) {
            if (index >= end) return@repeat
            val flags = body[index++].toInt() and 0xFF
            isUpdate = flags and 0x40 != 0
            val crcPresent = flags and 0x20 != 0
            val restrictionsPresent = flags and 0x10 != 0
            if (isUpdate) index += flagDataLength(body, index, end)
            if (crcPresent) {
                val length = flagDataLength(body, index, end)
                crc = readCrc35(body, index + 1, length)
                index += 1 + length
            }
            if (restrictionsPresent) {
                val length = flagDataLength(body, index, end)
                if (length >= 1 && index + 1 < end) restrictions = TagRestrictions.fromByte(body[index + 1])
                index += 1 + length
            }
        }
        return TagExtendedHeader.V24(isUpdate = isUpdate, crc = crc, restrictions = restrictions) to totalSize
    }

    private fun flagDataLength(body: ByteArray, index: Int, end: Int): Int {
        if (index >= end) return 0
        return body[index].toInt() and 0xFF
    }

    private fun readCrc35(body: ByteArray, offset: Int, length: Int): UInt {
        var value = 0L
        for (position in 0 until length.coerceAtMost(5)) {
            if (offset + position >= body.size) break
            value = value shl 7 or (body[offset + position].toLong() and 0x7F)
        }
        return value.toUInt()
    }

    private fun parseFrameHeader(version: Id3Version, body: ByteArray, offset: Int): Pair<FrameHeader, Int> {
        val frameId = FrameId.parse(body, offset)
        val status = FrameStatusFlags.fromByte(version, body[offset + 8])
        return when (version) {
            Id3Version.V2_3 -> {
                val size = readU32(body, offset + 4)
                val format = FrameFormatFlags.V23.fromByte(body[offset + 9])
                var index = offset + FrameHeader.SIZE
                val decompressedSize = if (format.compression) readU32(body, index).also { index += 4 } else null
                val encryptionMethod = if (format.encryption) body[index++].toUByte() else null
                val groupId = if (format.grouping) body[index++].toUByte() else null
                FrameHeader.V23(frameId, size, status, format, decompressedSize, encryptionMethod, groupId) to
                    (index - offset)
            }

            Id3Version.V2_4 -> {
                val size = SynchsafeInt.decode(body, offset + 4)
                val format = FrameFormatFlags.V24.fromByte(body[offset + 9])
                var index = offset + FrameHeader.SIZE
                val groupId = if (format.grouping) body[index++].toUByte() else null
                val dataLength = if (format.dataLengthIndicator) {
                    SynchsafeInt.decode(body, index).also { index += 4 }
                } else {
                    null
                }
                val encryptionMethod = if (format.encryption) body[index++].toUByte() else null
                FrameHeader.V24(frameId, size, status, format, groupId, dataLength, encryptionMethod) to (index - offset)
            }
        }
    }

    private fun maybeReverseUnsynchronisation(header: TagHeader, frameHeader: FrameHeader, body: ByteArray): ByteArray {
        val frameUnsynchronised = (frameHeader as? FrameHeader.V24)?.format?.unsynchronisation == true
        return if (header.flags.unsynchronisation || frameUnsynchronised) reverseUnsynchronisation(body) else body
    }

    private fun decodeBody(header: FrameHeader, body: ByteArray, bodyOffset: Int): Id3Frame {
        if (isTransformed(header)) return UnknownFrame(header, DataBytes(body.copyOf(), offset = bodyOffset))
        return FrameDecoderRegistry.decode(header, body, bodyOffset)
    }

    private fun isTransformed(header: FrameHeader): Boolean = when (header) {
        is FrameHeader.V23 -> header.format.compression || header.format.encryption
        is FrameHeader.V24 -> header.format.compression || header.format.encryption
    }

    private fun parseFooter(header: TagHeader, input: InputStream): TagFooter? {
        if (header.version != Id3Version.V2_4 || !header.flags.footerPresent) return null
        val bytes = readFully(input, TagFooter.SIZE, "tag footer")
        if (bytes[0] != '3'.code.toByte() || bytes[1] != 'D'.code.toByte() || bytes[2] != 'I'.code.toByte()) {
            throw Id3Exception("Missing 3DI footer identifier")
        }
        val version = Id3Version.from(bytes[3], bytes[4]) ?: header.version
        return TagFooter(
            version = version,
            flags = TagHeaderFlags.fromByte(version, bytes[5]),
            tagSize = SynchsafeInt.decode(bytes, 6),
        )
    }

    private fun readU32(bytes: ByteArray, offset: Int): Int {
        if (bytes.size - offset < 4) throw Id3Exception("Truncated 32 bit integer")
        return (bytes[offset].toInt() and 0xFF shl 24) or
            (bytes[offset + 1].toInt() and 0xFF shl 16) or
            (bytes[offset + 2].toInt() and 0xFF shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)
    }

    private fun readFully(input: InputStream, length: Int, what: String): ByteArray {
        if (length < 0) throw Id3Exception("Negative length for $what")
        try {
            val bytes = input.readNBytes(length)
            if (bytes.size < length) throw Id3Exception("Truncated $what: expected $length bytes, got ${bytes.size}")
            return bytes
        } catch (e: EOFException) {
            throw Id3Exception("Truncated $what", e)
        } catch (e: Id3Exception) {
            throw e
        } catch (e: Exception) {
            throw Id3Exception("Failed reading $what", e)
        }
    }

    private const val MIN_V23_EXTENDED_HEADER = 4
    private const val MIN_V24_EXTENDED_HEADER = 6
}
