package de.carsten.android.muzzic.id3.model.tag

/**
 * ISO-639-2 three-letter language code as used by language-bearing frames.
 *
 * Should be lower case; "XXX" marks an unknown language.
 *
 * @property code three-letter code.
 */
@JvmInline
value class LanguageCode(val code: String) {
    companion object {
        /** Marker for an unknown language. */
        val UNKNOWN = LanguageCode("XXX")
    }
}
