package de.carsten.android.muzzic.id3.model.tag

/**
 * Optional extended header carrying non-vital tag insights.
 *
 * The two major versions define incompatible layouts, hence the sealed
 * hierarchy.
 */
sealed interface TagExtendedHeader {
    /**
     * v2.3 extended header: padding size plus optional CRC-32.
     *
     * @property paddingSize total tag size excluding frames and headers.
     * @property crc CRC-32 over the frames before unsynchronisation, null if absent.
     */
    data class V23(val paddingSize: Int = 0, val crc: UInt? = null) : TagExtendedHeader

    /**
     * v2.4 extended header: flag-driven optional sections.
     *
     * @property isUpdate tag updates a tag found earlier in the file or stream.
     * @property crc CRC-32 over header-to-footer data minus the extended header, null if absent.
     * @property restrictions artificial tag restrictions, null if absent.
     */
    data class V24(val isUpdate: Boolean = false, val crc: UInt? = null, val restrictions: TagRestrictions? = null) : TagExtendedHeader
}
