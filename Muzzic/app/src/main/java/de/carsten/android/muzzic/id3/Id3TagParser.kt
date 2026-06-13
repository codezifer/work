package de.carsten.android.muzzic.id3

import de.carsten.android.muzzic.logging.logger
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.id3.AbstractID3v2Frame
import org.jaudiotagger.tag.id3.ID3v24Frames
import org.jaudiotagger.tag.id3.framebody.FrameBodyPCNT
import org.jaudiotagger.tag.id3.framebody.FrameBodyPOPM
import java.io.File
import java.io.InputStream

/**
 * Utility for parsing ID3 tags and extracting metadata like album art offsets.
 */
object Id3TagParser {
    private val logger = logger()

    private const val ID_ID3 = "ID3"
    private const val ID_RATING = ID3v24Frames.FRAME_ID_POPULARIMETER
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
     * Extracts all relevant metadata from an [AudioFile].
     *
     * @param audioFile The audio file to extract from.
     * @return An [ExtendedMetadata] object containing the extracted values.
     */
    fun extractMetadata(audioFile: AudioFile): ExtendedMetadata {
        val tag = audioFile.tag
        if (tag == null) return ExtendedMetadata()

        val yearString = tag.getFirst(FieldKey.YEAR).ifBlank { tag.getFirst(FieldKey.ORIGINAL_YEAR) }
        val year = parseId3Year(yearString)

        val trackString = tag.getFirst(FieldKey.TRACK)
        val (trackNumber, totalTracksFromTrack) = parseTrackString(trackString)
        val totalTracksField = tag.getFirst(FieldKey.TRACK_TOTAL).trim().toIntOrNull() ?: -1
        val totalTracks = if (totalTracksField != -1) totalTracksField else totalTracksFromTrack

        val rating = extractRating(tag)
        val playCount = extractPlayCount(tag)

        return ExtendedMetadata(
            year = year,
            rating = rating,
            playCount = playCount,
            trackNumber = trackNumber,
            totalTracks = totalTracks,
        )
    }

    /**
     * Extracts extended metadata (Year, Rating, Play Count) from an ID3v2 tag.
     *
     * @param tag The tag to extract from.
     * @return An [ExtendedMetadata] object containing the extracted values.
     */
    fun extractExtendedMetadata(tag: Tag?): ExtendedMetadata {
        if (tag == null) return ExtendedMetadata()

        val yearString = tag.getFirst(FieldKey.YEAR).ifBlank { tag.getFirst(FieldKey.ORIGINAL_YEAR) }
        val year = parseId3Year(yearString)

        val trackString = tag.getFirst(FieldKey.TRACK)
        val (trackNumber, totalTracksFromTrack) = parseTrackString(trackString)
        val totalTracksField = tag.getFirst(FieldKey.TRACK_TOTAL).trim().toIntOrNull() ?: -1
        val totalTracks = if (totalTracksField != -1) totalTracksField else totalTracksFromTrack

        val rating = extractRating(tag)
        val playCount = extractPlayCount(tag)

        return ExtendedMetadata(
            year = year,
            rating = rating,
            playCount = playCount,
            trackNumber = trackNumber,
            totalTracks = totalTracks,
        )
    }

    private fun extractRating(tag: Tag): Int {
        // Try POPM frame first (ID3v2 standard for rating)
        val frame = tag.getFirstField(ID_RATING)
        if (frame is AbstractID3v2Frame) {
            val body = frame.body
            if (body is FrameBodyPOPM) {
                return body.rating.toInt() and 0xFF
            }
        }

        // Fallback to general FieldKey.RATING (works for many formats in JAudioTagger)
        val ratingStr = tag.getFirst(FieldKey.RATING)
        return ratingStr.toIntOrNull() ?: 0
    }

    private fun extractPlayCount(tag: Tag): Int {
        // Try POPM counter first
        val popmFrame = tag.getFirstField(ID_RATING)
        if (popmFrame is AbstractID3v2Frame) {
            val body = popmFrame.body
            if (body is FrameBodyPOPM) {
                val counter = body.counter
                if (counter > 0) return counter.toInt()
            }
        }

        // Fallback to PCNT frame
        val pcntFrame = tag.getFirstField(ID_PLAY_COUNT)
        if (pcntFrame is AbstractID3v2Frame) {
            val body = pcntFrame.body
            if (body is FrameBodyPCNT) {
                return body.counter.toInt()
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

        val separators = listOf("/", "\\", "-", ":")
        return try {
            val separator = separators.find { track.contains(it) }
            if (separator != null) {
                val parts = track.split(separator)
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
        val tagLength = unpackSynchsafeInteger(header[6], header[7], header[8], header[9])
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
                    unpackSynchsafeInteger(tagBytes[0], tagBytes[1], tagBytes[2], tagBytes[3])
                } else {
                    // ID3v2.3 extended header size is a regular integer
                    unpackInteger(tagBytes[0], tagBytes[1], tagBytes[2], tagBytes[3]) + 4
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
                    frameSize = unpackInteger(0.toByte(), tagBytes[offset + 3], tagBytes[offset + 4], tagBytes[offset + 5])
                    frameHeaderSize = 6
                } else {
                    // ID3v2.3/4 frame header: 4-byte ID, 4-byte size, 2-byte flags
                    frameId = String(tagBytes, offset, 4)
                    frameSize = if (majorVersion == 4) {
                        // ID3v2.4 uses synchsafe integers for frame sizes
                        unpackSynchsafeInteger(tagBytes[offset + 4], tagBytes[offset + 5], tagBytes[offset + 6], tagBytes[offset + 7])
                    } else {
                        // ID3v2.3 uses regular integers for frame sizes
                        unpackInteger(tagBytes[offset + 4], tagBytes[offset + 5], tagBytes[offset + 6], tagBytes[offset + 7])
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

    private fun unpackSynchsafeInteger(b1: Byte, b2: Byte, b3: Byte, b4: Byte): Int {
        return (b1.toInt() and 0x7F shl 21) or
                (b2.toInt() and 0x7F shl 14) or
                (b3.toInt() and 0x7F shl 7) or
                (b4.toInt() and 0x7F)
    }

    private fun unpackInteger(b1: Byte, b2: Byte, b3: Byte, b4: Byte): Int {
        return (b1.toInt() and 0xFF shl 24) or
                (b2.toInt() and 0xFF shl 16) or
                (b3.toInt() and 0xFF shl 8) or
                (b4.toInt() and 0xFF)
    }
}
