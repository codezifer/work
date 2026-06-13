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
        val header = ByteArray(10)
        if (input.read(header) != 10 || String(header, 0, 3) != ID_ID3) {
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

            if (frameId == ID_APIC || frameId == ID_PIC) {
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
