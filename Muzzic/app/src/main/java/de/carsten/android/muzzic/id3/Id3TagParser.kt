package de.carsten.android.muzzic.id3

import com.mpatric.mp3agic.BufferTools
import com.mpatric.mp3agic.ID3v2
import de.carsten.android.muzzic.logging.logger
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * Utility for parsing ID3 tags and extracting metadata like album art offsets.
 */
object Id3TagParser {
    private val logger = logger()

    private const val ID_ID3 = "ID3"
    private const val ID_YEAR = "TYER"
    private const val ID_RECORDING_TIME = "TDRC"
    private const val ID_RATING = "POPM"
    private const val ID_PLAY_COUNT = "PCNT"
    private const val ID_APIC = "APIC"
    private const val ID_PIC = "PIC"

    /**
     * Extracts the album art offset and size from an MP3 file.
     *
     * @param file The MP3 file to parse.
     * @return An [AlbumArtOffset] containing the location and size, or (0, 0) if not found.
     */
    fun getAlbumArtOffsetAndSize(file: File): AlbumArtOffset = try {
        file.inputStream().use { input ->
            parseFromStream(input)
        }
    } catch (e: Exception) {
        logger.error("Failed to extract album art offset for ${file.absolutePath}", e)
        AlbumArtOffset(0L, 0L)
    }

    /**
     * Extracts extended metadata (Year, Rating, Play Count) from an ID3v2 tag.
     *
     * @param id3v2Tag The ID3v2 tag to extract from.
     * @return An [ExtendedMetadata] object containing the extracted values.
     */
    fun extractExtendedMetadata(id3v2Tag: ID3v2): ExtendedMetadata {
        val yearString = extractYear(id3v2Tag)
        val year = parseId3Year(yearString)
        val rating = extractRating(id3v2Tag)
        val playCount = extractPlayCount(id3v2Tag)

        return ExtendedMetadata(
            year = year,
            rating = rating,
            playCount = playCount,
        )
    }

    private fun extractYear(id3v2Tag: ID3v2): String? {
        // Try TDRC (v2.4)
        val tdrcFrame = id3v2Tag.frameSets[ID_RECORDING_TIME]?.frames?.firstOrNull()
        if (tdrcFrame != null) {
            val data = tdrcFrame.data
            if (data != null && data.size > 1) {
                // Text frames start with encoding byte
                return BufferTools.byteBufferToStringIgnoringEncodingIssues(data, 1, data.size - 1)
            }
        }

        // Try TYER (v2.3)
        val tyerFrame = id3v2Tag.frameSets[ID_YEAR]?.frames?.firstOrNull()
        if (tyerFrame != null) {
            val data = tyerFrame.data
            if (data != null && data.size > 1) {
                return BufferTools.byteBufferToStringIgnoringEncodingIssues(data, 1, data.size - 1)
            }
        }

        // Fallback to mp3agic convenience method
        return id3v2Tag.year
    }

    private fun extractRating(id3v2Tag: ID3v2): Int {
        // Try WMP rating first as it's a common convenience method in mp3agic
        val wmpRating = id3v2Tag.wmpRating
        if (wmpRating > 0) {
            // Map 1-5 stars to WMP raw values (approximate)
            return when (wmpRating) {
                1 -> 13
                2 -> 64
                3 -> 128
                4 -> 196
                5 -> 255
                else -> 0
            }
        }

        // Fallback: manually parse POPM frame if available
        val popmFrame = id3v2Tag.frameSets[ID_RATING]?.frames?.firstOrNull()
        if (popmFrame != null) {
            val data = popmFrame.data
            if (data != null && data.isNotEmpty()) {
                // POPM frame format: <email> <00> <rating> <optional counter>
                // Find the first null byte
                val nullIndex = data.indexOf(0.toByte())
                if (nullIndex >= 0 && nullIndex + 1 < data.size) {
                    return data[nullIndex + 1].toInt() and 0xFF
                }
            }
        }
        return 0
    }

    private fun extractPlayCount(id3v2Tag: ID3v2): Int {
        val pcntFrame = id3v2Tag.frameSets[ID_PLAY_COUNT]?.frames?.firstOrNull()
        if (pcntFrame != null) {
            val data = pcntFrame.data
            if (data != null && data.isNotEmpty()) {
                return when (data.size) {
                    1 -> data[0].toInt() and 0xFF
                    2 -> ByteBuffer.wrap(data).short.toInt() and 0xFFFF
                    4 -> ByteBuffer.wrap(data).int
                    else -> 0
                }
            }
        }
        return 0
    }

    /**
     * Parses raw ID3 year as string which could have a format like "2025" or "2025-01-02"
     *
     * @param year raw ID3 year (TYER) as [String]
     * @return parsed year as [Int] (-1 if source string is null or empty)
     */
    fun parseId3Year(year: String?): Int {
        if (year.isNullOrBlank()) return -1

        val regex = Regex("^(\\d{4})")
        val match = regex.find(year)
        return match
            ?.groups
            ?.get(1)
            ?.value
            ?.toIntOrNull() ?: -1
    }

    /**
     * Parses a track string (e.g., "1", "1/10", "01/10") into track number and total tracks.
     *
     * @param track The raw track string.
     * @return A Pair of (trackNumber, totalTracks). Defaults to (-1, -1) if unparseable.
     */
    fun parseTrackString(track: String?): Pair<Int, Int> {
        if (track.isNullOrBlank()) return Pair(-1, -1)

        return try {
            if (track.contains("/")) {
                val parts = track.split("/")
                val trackNum = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: -1
                val totalTracks = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: -1
                Pair(trackNum, totalTracks)
            } else {
                val trackNum = track.trim().toIntOrNull() ?: -1
                Pair(trackNum, -1)
            }
        } catch (e: Exception) {
            Pair(-1, -1)
        }
    }

    private fun parseFromStream(input: InputStream): AlbumArtOffset {
        // ID3 header: "ID3" (3 bytes), version (2 bytes), flags (1 byte), size (4 bytes)
        val header = ByteArray(10)
        val bytesReadHeader = input.read(header)
        if (bytesReadHeader != 10) {
            logger.debug("Could not read ID3 header (read $bytesReadHeader bytes)")
            return AlbumArtOffset(0L, 0L)
        }
        // Validate ID3 identifier
        if (String(header, 0, 3) != ID_ID3) {
            logger.debug("No ID3 tag found in file")
            return AlbumArtOffset(0L, 0L)
        }

        val majorVersion = header[3].toInt()
        val tagFlags = header[5].toInt()
        val extendedHeader = (tagFlags and 0x40) != 0

        // The size is a 32-bit synchsafe integer (7 bits per byte)
        val tagLength = BufferTools.unpackSynchsafeInteger(header[6], header[7], header[8], header[9])
        val tagBytes = ByteArray(tagLength)
        var totalBytesRead = 0
        while (totalBytesRead < tagLength) {
            val read = input.read(tagBytes, totalBytesRead, tagLength - totalBytesRead)
            if (read == -1) break
            totalBytesRead += read
        }

        if (totalBytesRead != tagLength) {
            logger.error("Failed to read full ID3 tag (expected $tagLength, read $totalBytesRead)")
            return AlbumArtOffset(0L, 0L)
        }

        var offset = 0
        if (extendedHeader) {
            try {
                val extHeaderSize = if (majorVersion == 4) {
                    // ID3v2.4 extended header size is a synchsafe integer
                    BufferTools.unpackSynchsafeInteger(tagBytes[0], tagBytes[1], tagBytes[2], tagBytes[3])
                } else {
                    // ID3v2.3 extended header size is a regular integer
                    BufferTools.unpackInteger(tagBytes[0], tagBytes[1], tagBytes[2], tagBytes[3]) + 4
                }
                offset += extHeaderSize
            } catch (e: Exception) {
                logger.error("Error parsing extended header", e)
                return AlbumArtOffset(0L, 0L)
            }
        }

        while (offset + 10 < tagLength) {
            val frameId: String
            val frameSize: Int
            val frameHeaderSize: Int

            try {
                if (majorVersion == 2) {
                    // ID3v2.2 frame header: 3-byte ID, 3-byte size
                    frameId = String(tagBytes, offset, 3)
                    frameSize = BufferTools.unpackInteger(0.toByte(), tagBytes[offset + 3], tagBytes[offset + 4], tagBytes[offset + 5])
                    frameHeaderSize = 6
                } else {
                    // ID3v2.3/4 frame header: 4-byte ID, 4-byte size, 2-byte flags
                    frameId = String(tagBytes, offset, 4)
                    frameSize = if (majorVersion == 4) {
                        // ID3v2.4 uses synchsafe integers for frame sizes
                        BufferTools.unpackSynchsafeInteger(tagBytes[offset + 4], tagBytes[offset + 5], tagBytes[offset + 6], tagBytes[offset + 7])
                    } else {
                        // ID3v2.3 uses regular integers for frame sizes
                        BufferTools.unpackInteger(tagBytes[offset + 4], tagBytes[offset + 5], tagBytes[offset + 6], tagBytes[offset + 7])
                    }
                    frameHeaderSize = 10
                }
            } catch (e: Exception) {
                logger.error("Error parsing frame header at offset $offset", e)
                break
            }

            if (frameId == ID_APIC || frameId == ID_PIC) {
                try {
                    if (offset + frameHeaderSize + frameSize > tagLength) {
                        logger.error("Frame $frameId exceeds tag length (offset: $offset, size: $frameSize, tagLength: $tagLength)")
                        return AlbumArtOffset(0L, 0L)
                    }

                    val frameData = ByteArray(frameSize)
                    System.arraycopy(tagBytes, offset + frameHeaderSize, frameData, 0, frameSize)

                    // Calculate internal offset within APIC/PIC frame
                    var internalOffset = 1 // Skip Text Encoding byte

                    if (majorVersion == 2) {
                        internalOffset += 3 // Skip 3-byte image format (e.g. "JPG")
                    } else {
                        // Skip MIME type (null-terminated string)
                        while (internalOffset < frameSize && frameData[internalOffset] != 0.toByte()) {
                            internalOffset++
                        }
                        internalOffset++ // Skip null terminator
                    }

                    if (internalOffset >= frameSize) {
                        logger.error("Malformed APIC/PIC frame: internal offset $internalOffset exceeds frame size $frameSize")
                        return AlbumArtOffset(0L, 0L)
                    }

                    internalOffset++ // Skip Picture Type byte (e.g. 0x03 for Front Cover)

                    // Skip Description (null-terminated string)
                    val encoding = frameData[0].toInt()
                    if (encoding == 1 || encoding == 2) { // UTF-16 (two null bytes terminator)
                        while (internalOffset + 1 < frameSize && (frameData[internalOffset] != 0.toByte() || frameData[internalOffset + 1] != 0.toByte())) {
                            internalOffset += 2
                        }
                        internalOffset += 2
                    } else { // ISO-8859-1 or UTF-8 (single null byte terminator)
                        while (internalOffset < frameSize && frameData[internalOffset] != 0.toByte()) {
                            internalOffset++
                        }
                        internalOffset++
                    }

                    if (internalOffset >= frameSize) {
                        logger.error("Malformed APIC/PIC frame: internal offset after description $internalOffset exceeds frame size $frameSize")
                        return AlbumArtOffset(0L, 0L)
                    }

                    val finalOffset = 10L + offset + frameHeaderSize + internalOffset
                    val finalSize = frameSize - internalOffset.toLong()

                    if (finalSize <= 0) {
                        logger.error("Invalid final image size calculated: $finalSize")
                        return AlbumArtOffset(0L, 0L)
                    }

                    return AlbumArtOffset(finalOffset, finalSize)
                } catch (e: Exception) {
                    logger.error("Error extracting album art from $frameId frame", e)
                    return AlbumArtOffset(0L, 0L)
                }
            }

            if (frameSize <= 0) break
            offset += frameHeaderSize + frameSize
        }

        logger.debug("No APIC/PIC frame found in ID3 tag")
        return AlbumArtOffset(0L, 0L)
    }
}
