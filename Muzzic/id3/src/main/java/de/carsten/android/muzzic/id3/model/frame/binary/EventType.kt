package de.carsten.android.muzzic.id3.model.frame.binary

/**
 * Event timing code types (ETCO).
 *
 * @property code raw event byte.
 */
enum class EventType(val code: Byte) {

    /** Padding, has no meaning. */
    PADDING(code = 0x00),

    /** End of initial silence. */
    END_OF_INITIAL_SILENCE(code = 0x01),

    /** Intro start. */
    INTRO_START(code = 0x02),

    /** Main part start. */
    MAIN_PART_START(code = 0x03),

    /** Outro start. */
    OUTRO_START(code = 0x04),

    /** Outro end. */
    OUTRO_END(code = 0x05),

    /** Verse start. */
    VERSE_START(code = 0x06),

    /** Refrain start. */
    REFRAIN_START(code = 0x07),

    /** Interlude start. */
    INTERLUDE_START(code = 0x08),

    /** Theme start. */
    THEME_START(code = 0x09),

    /** Variation start. */
    VARIATION_START(code = 0x0A),

    /** Key change. */
    KEY_CHANGE(code = 0x0B),

    /** Time change. */
    TIME_CHANGE(code = 0x0C),

    /** Momentary unwanted noise. */
    MOMENTARY_NOISE(code = 0x0D),

    /** Sustained noise. */
    SUSTAINED_NOISE(code = 0x0E),

    /** Sustained noise end. */
    SUSTAINED_NOISE_END(code = 0x0F),

    /** Intro end. */
    INTRO_END(code = 0x10),

    /** Main part end. */
    MAIN_PART_END(code = 0x11),

    /** Verse end. */
    VERSE_END(code = 0x12),

    /** Refrain end. */
    REFRAIN_END(code = 0x13),

    /** Theme end. */
    THEME_END(code = 0x14),

    /** Profanity. */
    PROFANITY(code = 0x15),

    /** Profanity end. */
    PROFANITY_END(code = 0x16),

    /** Audio end (start of silence). */
    AUDIO_END(code = 0xFD.toByte()),

    /** Audio file ends. */
    AUDIO_FILE_ENDS(code = 0xFE.toByte()),
    ;

    companion object {
        /**
         * Resolves an event type from its raw byte.
         *
         * @param code raw event byte.
         * @return matching type or null for reserved and user-defined ranges.
         */
        fun fromCode(code: Byte): EventType? = entries.firstOrNull { it.code == code }
    }
}
