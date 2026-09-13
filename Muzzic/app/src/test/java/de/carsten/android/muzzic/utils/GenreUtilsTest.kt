package de.carsten.android.muzzic.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Unit tests for [GenreUtils].
 */
class GenreUtilsTest {

    @Test
    fun `normalizeKey maps death core variants to the same key`() {
        val keys = listOf("Death Core", "Death-Core", "Deathcore", "death core", "DEATHCORE")
            .map { GenreUtils.normalizeKey(it) }

        assertThat(keys.toSet()).containsExactly("deathcore")
    }

    @Test
    fun `getCanonicalName prefers spaced variant`() {
        val canonical = GenreUtils.getCanonicalName(mapOf("Death Core" to 2, "Deathcore" to 5, "Death-Core" to 1))

        assertThat(canonical).isEqualTo("Death Core")
    }

    @Test
    fun `getCanonicalName falls back to most frequent variant`() {
        val canonical = GenreUtils.getCanonicalName(mapOf("Deathcore" to 3, "Death-Core" to 7))

        assertThat(canonical).isEqualTo("Death-Core")
    }

    @Test
    fun `groupByNormalizedKey merges spelling variants`() {
        val groups = GenreUtils.groupByNormalizedKey(
            mapOf("Death Core" to 2, "Death-Core" to 1, "Deathcore" to 4, "Rock" to 3),
        )

        assertThat(groups).containsOnlyKeys("deathcore", "rock")
        assertThat(groups.getValue("deathcore"))
            .containsOnlyKeys("Death Core", "Death-Core", "Deathcore")
        assertThat(groups.getValue("rock")).containsOnlyKeys("Rock")
    }
}
