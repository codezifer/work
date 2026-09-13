package de.carsten.android.muzzic.id3

import de.carsten.android.muzzic.id3.exceptions.Id3Exception
import de.carsten.android.muzzic.id3.mapper.Id3MetadataMapper
import de.carsten.android.muzzic.id3.model.Id3Metadata
import de.carsten.android.muzzic.id3.model.tag.Id3Tag
import de.carsten.android.muzzic.id3.parser.Id3Parser
import de.carsten.android.muzzic.logging.MuzzicLogger
import java.io.InputStream

object Id3 {
    /**
     * Parses Id3 metadata from input stream.
     *
     * @param input source [InputStream] positioned at the tag header.
     * @param logger parent [MuzzicLogger].
     * @param fileId optional file identifier like filename [String].
     * @return [Id3Metadata].
     */
    fun parse(input: InputStream, logger: MuzzicLogger, fileId: String? = null): Id3Metadata = Id3MetadataMapper.map(parseTag(input, logger, fileId).frames)

    /**
     * Parses the full typed tag from input stream.
     *
     * @param input source [InputStream] positioned at the tag header.
     * @param logger parent [MuzzicLogger].
     * @param fileId optional file identifier like filename [String].
     * @return typed [Id3Tag].
     */
    fun parseTag(input: InputStream, logger: MuzzicLogger, fileId: String? = null): Id3Tag {
        val logFiledId = { prefix: String -> if (fileId == null) "" else "$prefix $fileId" }
        try {
            logger.info("Starting to parse Id3 metadata from input stream ${logFiledId("for")} ...")
            return Id3Parser.parse(input)
        } catch (e: Exception) {
            logger.error("An error occurred while parsing id3 metadata ${logFiledId("for")}!", e)
            if (e is Id3Exception) throw e
            throw Id3Exception(e)
        } finally {
            logger.info("... Finished to parse Id3 metadata from input stream ${logFiledId("for")}.")
        }
    }
}
