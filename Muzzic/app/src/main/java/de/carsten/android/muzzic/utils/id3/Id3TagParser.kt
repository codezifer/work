package de.carsten.android.muzzic.utils.id3

import com.mpatric.mp3agic.BufferTools
import de.carsten.android.muzzic.logging.logger
import java.io.File
import java.io.InputStream

/**
 * Utility for parsing ID3 tags and extracting metadata like album art offsets.
 */
object Id3TagParser {
    private val logger = logger()

    /**
     * Extracts the album art offset and size from an MP3 file.
     *
     * @param file The MP3 file to parse.
     * @return An [AlbumArtOffset] containing the location and size, or (0, 0) if not found.
     */
    fun getAlbumArtOffsetAndSize(file: File): AlbumArtOffset {
        return try {
            file.inputStream().use { input ->
                parseFromStream(input)
            }
        } catch (e: Exception) {
            logger.error("Failed to extract album art offset for ${file.absolutePath}", e)
            AlbumArtOffset(0L, 0L)
        }
    }

    private fun parseFromStream(input: InputStream): AlbumArtOffset {
        val header = ByteArray(10)
        if (input.read(header) != 10 || String(header, 0, 3) != "ID3") {
            return AlbumArtOffset(0L, 0L)
        }

        val majorVersion = header[3].toInt()
        val tagFlags = header[5].toInt()
        val extendedHeader = (tagFlags and 0x40) != 0

        val tagLength = BufferTools.unpackSynchsafeInteger(header[6], header[7], header[8], header[9])
        val tagBytes = ByteArray(tagLength)
        if (input.read(tagBytes) != tagLength) {
            return AlbumArtOffset(0L, 0L)
        }

        var offset = 0
        if (extendedHeader) {
            val extHeaderSize = if (majorVersion == 4) {
                BufferTools.unpackSynchsafeInteger(tagBytes[0], tagBytes[1], tagBytes[2], tagBytes[3])
            } else {
                BufferTools.unpackInteger(tagBytes[0], tagBytes[1], tagBytes[2], tagBytes[3]) + 4
            }
            offset += extHeaderSize
        }

        while (offset + 10 < tagLength) {
            val frameId: String
            val frameSize: Int
            val frameHeaderSize: Int

            if (majorVersion == 2) {
                frameId = String(tagBytes, offset, 3)
                frameSize = BufferTools.unpackInteger(0.toByte(), tagBytes[offset + 3], tagBytes[offset + 4], tagBytes[offset + 5])
                frameHeaderSize = 6
            } else {
                frameId = String(tagBytes, offset, 4)
                frameSize = if (majorVersion == 4) {
                    BufferTools.unpackSynchsafeInteger(tagBytes[offset + 4], tagBytes[offset + 5], tagBytes[offset + 6], tagBytes[offset + 7])
                } else {
                    BufferTools.unpackInteger(tagBytes[offset + 4], tagBytes[offset + 5], tagBytes[offset + 6], tagBytes[offset + 7])
                }
                frameHeaderSize = 10
            }

            if (frameId == "APIC" || frameId == "PIC") {
                val frameData = ByteArray(frameSize)
                System.arraycopy(tagBytes, offset + frameHeaderSize, frameData, 0, frameSize)

                // Calculate internal offset within APIC/PIC frame
                var internalOffset = 1 // Skip Text Encoding byte

                if (majorVersion == 2) {
                    internalOffset += 3 // Skip 3-byte image format
                } else {
                    // Skip MIME type
                    while (internalOffset < frameSize && frameData[internalOffset] != 0.toByte()) {
                        internalOffset++
                    }
                    internalOffset++ // Skip null terminator
                }

                internalOffset++ // Skip Picture Type byte

                // Skip Description
                val encoding = frameData[0].toInt()
                if (encoding == 1 || encoding == 2) { // UTF-16
                    while (internalOffset + 1 < frameSize && (frameData[internalOffset] != 0.toByte() || frameData[internalOffset + 1] != 0.toByte())) {
                        internalOffset += 2
                    }
                    internalOffset += 2
                } else { // ISO-8859-1 or UTF-8
                    while (internalOffset < frameSize && frameData[internalOffset] != 0.toByte()) {
                        internalOffset++
                    }
                    internalOffset++
                }

                val finalOffset = 10L + offset + frameHeaderSize + internalOffset
                val finalSize = frameSize - internalOffset.toLong()

                return AlbumArtOffset(finalOffset, finalSize)
            }

            if (frameSize <= 0) break
            offset += frameHeaderSize + frameSize
        }

        return AlbumArtOffset(0L, 0L)
    }
}
