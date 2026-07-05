package de.carsten.android.muzzic.id3

import de.carsten.android.muzzic.UNKNOWN
import de.carsten.android.muzzic.UNKNOWN_TRACK
import de.carsten.android.muzzic.logging.logger
import java.io.File
import java.io.InputStream
import kotlin.math.roundToLong
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.id3.AbstractID3v2Frame
import org.jaudiotagger.tag.id3.ID3v24Frames
import org.jaudiotagger.tag.id3.framebody.FrameBodyPCNT
import org.jaudiotagger.tag.id3.framebody.FrameBodyPOPM

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

    private const val ID3_HEADER_SIZE = 10
    private const val FRAME_HEADER_SIZE_V22 = 6
    private const val FRAME_HEADER_SIZE_V23V24 = 10

    /**
     * Extracts the album art offset and size from an MP3 file.
     *
     * @param file The MP3 file to parse.
     * @return An [AlbumArtMetadata] containing the location and size, or (0, 0) if not found.
     */
    fun getAlbumArtMetadata(file: File): AlbumArtMetadata = try {
        file.inputStream().use { input ->
            parseFromStream(input)
        }
    } catch (e: Exception) {
        logger.error("Failed to extract album art offset for ${file.absolutePath}", e)
        AlbumArtMetadata(0L, 0L)
    }

    /**
     * Extracts all relevant metadata from an [AudioFile].
     *
     * @param audioFile The audio file to extract from.
     * @return An [ExtendedMetadata] object containing the extracted values.
     */
    fun extractMetadata(audioFile: AudioFile): ExtendedMetadata {
        val tag = audioFile.tag ?: return ExtendedMetadata()
        val header = audioFile.audioHeader ?: return ExtendedMetadata()

        val title = tag.getFirst(FieldKey.TITLE) ?: UNKNOWN
        val album = tag.getFirst(FieldKey.ALBUM) ?: UNKNOWN
        val artist = tag.getFirst(FieldKey.ARTIST).ifBlank { tag.getFirst(FieldKey.ALBUM_ARTIST) ?: UNKNOWN }
        val genre = tag.getFirst(FieldKey.GENRE) ?: UNKNOWN
        val duration = (header.preciseTrackLength * 1000).roundToLong()
        val yearString = tag.getFirst(FieldKey.YEAR).ifBlank { tag.getFirst(FieldKey.ORIGINAL_YEAR) ?: UNKNOWN }
        val year = parseId3Year(yearString)
        val trackString = tag.getFirst(FieldKey.TRACK) ?: UNKNOWN_TRACK
        val (trackNumber, totalTracksFromTrack) = parseTrackString(trackString)
        val totalTracksField = tag.getFirst(FieldKey.TRACK_TOTAL).trim().toIntOrNull() ?: -1
        val totalTracks = if (totalTracksField != -1) totalTracksField else totalTracksFromTrack
        val rating = extractRating(tag)
        val playCount = extractPlayCount(tag)

        return ExtendedMetadata(
            title = title,
            album = album,
            artist = artist,
            genre = genre,
            year = year,
            duration = duration,
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

    private fun parseFromStream(input: InputStream): AlbumArtMetadata {
        val header = ByteArray(ID3_HEADER_SIZE)
        if (input.read(header) != ID3_HEADER_SIZE || String(header, 0, 3) != ID_ID3) {
            return AlbumArtMetadata(0L, 0L)
        }

        val majorVersion = header[3].toInt()
        val extendedHeader = (header[5].toInt() and 0x40) != 0
        val tagLength = unpackSynchsafeInteger(header[6], header[7], header[8], header[9])

        val tagBytes = readFully(input, tagLength) ?: return AlbumArtMetadata(0L, 0L)

        var offset = if (extendedHeader) calculateExtendedHeaderSize(tagBytes, majorVersion) else 0

        while (offset + 10 < tagLength) {
            val (frameId, frameSize, frameHeaderSize) = parseFrameHeader(tagBytes, offset, majorVersion)
                ?: break

            if (frameId == ID_APIC || frameId == ID_PIC) {
                return extractAlbumArtFromFrame(tagBytes, offset, frameHeaderSize, frameSize, majorVersion)
            }

            if (frameSize <= 0) break
            offset += frameHeaderSize + frameSize
        }

        return AlbumArtMetadata(0L, 0L)
    }

    private fun readFully(input: InputStream, length: Int): ByteArray? {
        val bytes = ByteArray(length)
        var totalRead = 0
        while (totalRead < length) {
            val read = input.read(bytes, totalRead, length - totalRead)
            if (read == -1) break
            totalRead += read
        }
        return if (totalRead == length) {
            bytes
        } else {
            logger.error("Failed to read full ID3 tag (expected $length, read $totalRead)")
            null
        }
    }

    private fun calculateExtendedHeaderSize(tagBytes: ByteArray, majorVersion: Int): Int = try {
        if (majorVersion == 4) {
            unpackSynchsafeInteger(tagBytes[0], tagBytes[1], tagBytes[2], tagBytes[3])
        } else {
            unpackInteger(tagBytes[0], tagBytes[1], tagBytes[2], tagBytes[3]) + 4
        }
    } catch (e: Exception) {
        logger.error("Error parsing extended header", e)
        0
    }

    private data class FrameHeaderInfo(val id: String, val size: Int, val headerSize: Int)

    private fun parseFrameHeader(tagBytes: ByteArray, offset: Int, majorVersion: Int): FrameHeaderInfo? = try {
        if (majorVersion == 2) {
            val id = String(tagBytes, offset, 3)
            val size = unpackInteger(0.toByte(), tagBytes[offset + 3], tagBytes[offset + 4], tagBytes[offset + 5])
            FrameHeaderInfo(id, size, FRAME_HEADER_SIZE_V22)
        } else {
            val id = String(tagBytes, offset, 4)
            val size = if (majorVersion == 4) {
                unpackSynchsafeInteger(tagBytes[offset + 4], tagBytes[offset + 5], tagBytes[offset + 6], tagBytes[offset + 7])
            } else {
                unpackInteger(tagBytes[offset + 4], tagBytes[offset + 5], tagBytes[offset + 6], tagBytes[offset + 7])
            }
            FrameHeaderInfo(id, size, FRAME_HEADER_SIZE_V23V24)
        }
    } catch (e: Exception) {
        logger.error("Error parsing frame header at offset $offset", e)
        null
    }

    private fun extractAlbumArtFromFrame(tagBytes: ByteArray, offset: Int, frameHeaderSize: Int, frameSize: Int, majorVersion: Int): AlbumArtMetadata {
        try {
            if (offset + frameHeaderSize + frameSize > tagBytes.size) {
                return AlbumArtMetadata(0L, 0L)
            }

            val frameData = ByteArray(frameSize)
            System.arraycopy(tagBytes, offset + frameHeaderSize, frameData, 0, frameSize)

            val internalOffset = calculateInternalFrameOffset(frameData, frameSize, majorVersion)
            if (internalOffset == -1) return AlbumArtMetadata(0L, 0L)

            val finalOffset = ID3_HEADER_SIZE.toLong() + offset + frameHeaderSize + internalOffset
            val finalSize = frameSize - internalOffset.toLong()

            if (finalSize <= 0) return AlbumArtMetadata(0L, 0L)

            val hashCode = calculateImageHashCode(tagBytes, offset + frameHeaderSize + internalOffset, finalSize.toInt())
            return AlbumArtMetadata(finalOffset, finalSize, hashCode)
        } catch (e: Exception) {
            logger.error("Error extracting album art", e)
            return AlbumArtMetadata(0L, 0L)
        }
    }

    private fun calculateInternalFrameOffset(frameData: ByteArray, frameSize: Int, majorVersion: Int): Int {
        var pos = 1 // Skip Text Encoding byte

        if (majorVersion == 2) {
            pos += 3 // Skip 3-byte image format (e.g. "JPG")
        } else {
            // Skip MIME type (null-terminated string)
            while (pos < frameSize && frameData[pos] != 0.toByte()) pos++
            pos++ // Skip null terminator
        }

        if (pos >= frameSize) return -1
        pos++ // Skip Picture Type byte

        // Skip Description (null-terminated string)
        val encoding = frameData[0].toInt()
        if (encoding == 1 || encoding == 2) { // UTF-16
            while (pos + 1 < frameSize && (frameData[pos] != 0.toByte() || frameData[pos + 1] != 0.toByte())) pos += 2
            pos += 2
        } else { // ISO-8859-1 or UTF-8
            while (pos < frameSize && frameData[pos] != 0.toByte()) pos++
            pos++
        }

        return if (pos < frameSize) pos else -1
    }

    private fun calculateImageHashCode(tagBytes: ByteArray, offset: Int, size: Int): Int = try {
        val imageBytes = ByteArray(size)
        System.arraycopy(tagBytes, offset, imageBytes, 0, size)
        imageBytes.contentHashCode()
    } catch (e: Exception) {
        logger.error("Failed to calculate hash code for album art", e)
        0
    }

    private fun unpackSynchsafeInteger(b1: Byte, b2: Byte, b3: Byte, b4: Byte): Int = (b1.toInt() and 0x7F shl 21) or
        (b2.toInt() and 0x7F shl 14) or
        (b3.toInt() and 0x7F shl 7) or
        (b4.toInt() and 0x7F)

    private fun unpackInteger(b1: Byte, b2: Byte, b3: Byte, b4: Byte): Int = (b1.toInt() and 0xFF shl 24) or
        (b2.toInt() and 0xFF shl 16) or
        (b3.toInt() and 0xFF shl 8) or
        (b4.toInt() and 0xFF)
}
