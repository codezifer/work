package de.carsten.android.muzzic.utils

import java.util.Locale

/**
 * Utility for genre name normalization and canonicalization.
 */
object GenreUtils {
    /**
     * Normalizes a genre name to a key used for grouping.
     *
     * Removes spaces, hyphens and converts to lowercase to ensure variants
     * like "Death Core", "Death-Core", and "Deathcore" produce the same key.
     *
     * @param genre The raw genre name.
     * @return A normalized key for grouping.
     */
    fun normalizeKey(genre: String): String = genre.lowercase(Locale.ROOT)
        .replace(" ", "")
        .replace("-", "")

    /**
     * Groups genre variants by their normalized key.
     *
     * Variants like "Death Core", "Death-Core" and "Deathcore" share the key
     * "deathcore" and can therefore be merged into one canonical genre.
     *
     * @param variants a map of stored genre names to their song count.
     * @return a map of normalized key to the variant-to-count map of that group.
     */
    fun groupByNormalizedKey(variants: Map<String, Int>): Map<String, Map<String, Int>> {
        val groups = mutableMapOf<String, MutableMap<String, Int>>()
        variants.forEach { (variant, count) ->
            groups.getOrPut(normalizeKey(variant)) { mutableMapOf() }[variant] = count
        }
        return groups
    }

    /**
     * Picks a canonical name for a group of genre variants.
     *
     * Prefers variants that contain spaces (likely multi-word) and picks the
     * most frequent one among them. Fallback to the most frequent variant.
     *
     * @param variants A map of variant names to their occurrence count.
     * @return The canonical name for the genre group.
     */
    fun getCanonicalName(variants: Map<String, Int>): String {
        if (variants.isEmpty()) return "Unknown"

        val entries = variants.entries.toList()

        // 1. Try to find variants with spaces, and pick the most frequent one among them
        val withSpaces = entries.filter { it.key.contains(" ") && !it.key.contains("-") }
            .maxByOrNull { it.value }

        if (withSpaces != null) return withSpaces.key

        // 2. Otherwise just pick the most frequent one
        return entries.maxByOrNull { it.value }?.key ?: "Unknown"
    }
}
