package de.carsten.android.muzzic.id3.mapper

import de.carsten.android.muzzic.id3.model.AlbumArtMetadata
import de.carsten.android.muzzic.id3.model.Id3Metadata
import de.carsten.android.muzzic.id3.model.frame.Id3Frame
import de.carsten.android.muzzic.id3.model.frame.binary.ApicFrame
import de.carsten.android.muzzic.id3.model.frame.binary.PcntFrame
import de.carsten.android.muzzic.id3.model.frame.binary.PopmFrame
import de.carsten.android.muzzic.id3.model.frame.language.CommFrame
import de.carsten.android.muzzic.id3.model.frame.text.TalbFrame
import de.carsten.android.muzzic.id3.model.frame.text.TconFrame
import de.carsten.android.muzzic.id3.model.frame.text.TdrcFrame
import de.carsten.android.muzzic.id3.model.frame.text.Tit2Frame
import de.carsten.android.muzzic.id3.model.frame.text.TlenFrame
import de.carsten.android.muzzic.id3.model.frame.text.Tpe1Frame
import de.carsten.android.muzzic.id3.model.frame.text.TrckFrame
import de.carsten.android.muzzic.id3.model.frame.text.TyerFrame
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Example visitor mapping raw frames onto the app-facing [Id3Metadata].
 *
 * Demonstrates hierarchical dispatch: only the interesting specifics are
 * overridden, everything else falls through the group defaults into
 * [visitGeneric]. First frame wins for single-value fields.
 */
class Id3MetadataMapper : Id3FrameVisitor<Unit> {
    private var title: String? = null
    private var track: Int? = null
    private var totalTracks: Int? = null
    private var artist: String? = null
    private var album: String? = null
    private var albumYear: Int? = null
    private var albumArt: AlbumArtMetadata? = null
    private var genre: String? = null
    private var rating: Int? = null
    private var playCount: Int? = null
    private var duration: Long? = null
    private var comment: String? = null

    override fun visitGeneric(frame: Id3Frame) = Unit

    override fun visitTit2(frame: Tit2Frame) {
        if (title == null) title = frame.values.firstOrNull()
    }

    override fun visitTpe1(frame: Tpe1Frame) {
        if (artist == null) artist = frame.values.firstOrNull()
    }

    override fun visitTalb(frame: TalbFrame) {
        if (album == null) album = frame.values.firstOrNull()
    }

    override fun visitTrck(frame: TrckFrame) {
        if (track != null) return
        val parts = (frame.values.firstOrNull() ?: return).split("/")
        track = parts.getOrNull(0)?.toIntOrNull()
        totalTracks = parts.getOrNull(1)?.toIntOrNull()
    }

    override fun visitTdrc(frame: TdrcFrame) {
        if (albumYear == null) albumYear = parseYear(frame.values.firstOrNull())
    }

    override fun visitTyer(frame: TyerFrame) {
        if (albumYear == null) albumYear = parseYear(frame.values.firstOrNull())
    }

    override fun visitApic(frame: ApicFrame) {
        if (albumArt == null) {
            albumArt = AlbumArtMetadata(
                offset = frame.pictureData.offset,
                length = frame.pictureData.length,
                mimeType = frame.mimeType,
                hashCode = frame.pictureData.bytes.contentHashCode(),
            )
        }
    }

    override fun visitTcon(frame: TconFrame) {
        if (genre == null) genre = frame.values.firstOrNull()
    }

    override fun visitTlen(frame: TlenFrame) {
        if (duration == null) duration = frame.values.firstOrNull()?.toLongOrNull()
    }

    override fun visitComm(frame: CommFrame) {
        if (comment == null) comment = frame.text
    }

    override fun visitPcnt(frame: PcntFrame) {
        if (playCount == null) playCount = parseCounter(frame.counter.bytes)
    }

    override fun visitPopm(frame: PopmFrame) {
        if (rating == null) rating = frame.rating.toInt()
        if (playCount == null) {
            frame.counter?.let { playCount = parseCounter(it.bytes) }
        }
    }

    /**
     * Builds the collected metadata snapshot.
     *
     * @return mapped [Id3Metadata].
     */
    fun build(): Id3Metadata = Id3Metadata(
        title = title,
        track = track,
        totalTracks = totalTracks,
        artist = artist,
        album = album,
        albumYear = albumYear,
        albumArt = albumArt,
        genre = genre,
        rating = rating,
        playCount = playCount,
        duration = duration,
        comment = comment,
    )

    private fun parseYear(value: String?): Int? = value?.take(4)?.toIntOrNull()

    /**
     * Folds big-endian counter bytes into an [Int].
     *
     * Play counters start at 32 bits and grow by one byte on overflow;
     * values beyond [Int.MAX_VALUE] are coerced since no playlist can
     * hold more plays.
     *
     * @param bytes big-endian counter bytes.
     * @return counter value within [Int] range.
     */
    private fun parseCounter(bytes: ByteArray): Int {
        var value = 0L
        for (byte in bytes) value = value shl 8 or (byte.toLong() and 0xFF)
        return value.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
    }

    companion object {
        /**
         * Maps a frame list onto [Id3Metadata] in one pass.
         *
         * @param frames frames to map.
         * @return mapped [Id3Metadata].
         */
        fun map(frames: List<Id3Frame>): Id3Metadata {
            val mapper = Id3MetadataMapper()
            frames.forEach { it.accept(mapper) }
            return mapper.build()
        }
    }
}
