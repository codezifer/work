package de.carsten.android.muzzic.ui.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class RatingConversionTest {
    /** Canonical WMP9 anchors for the eleven 0.5-star steps from 0 to 5 stars. */
    private val anchors = intArrayOf(0, 25, 51, 75, 102, 128, 153, 178, 204, 225, 255)

    @Test
    fun `getStars maps every half step anchor to its star value`() {
        anchors.forEachIndexed { index, wmp ->
            assertThat(getStars(wmp)).isEqualTo(index / 2f)
        }
    }

    @Test
    fun `getWmpRating maps every half step star value to its anchor`() {
        anchors.forEachIndexed { index, wmp ->
            assertThat(getWmpRating(index / 2f)).isEqualTo(wmp)
        }
    }

    @Test
    fun `round trip preserves the rating`() {
        anchors.forEach { wmp ->
            assertThat(getWmpRating(getStars(wmp))).isEqualTo(wmp)
        }
    }

    @Test
    fun `null rating is unrated`() {
        assertThat(getStars(null)).isEqualTo(0f)
    }

    @Test
    fun `values are clamped to the valid range`() {
        assertThat(getWmpRating(7f)).isEqualTo(255)
        assertThat(getWmpRating(-2f)).isEqualTo(0)
    }

    @Test
    fun `getStarRating exposes the float value and max stars`() {
        val rating = getStarRating(153)
        assertThat(rating.starRating).isEqualTo(3f)
        assertThat(rating.maxStars).isEqualTo(5)
    }

    @Test
    fun `getWmpRating converts a media3 star rating`() {
        assertThat(getWmpRating(androidx.media3.common.StarRating(5, 2.5f))).isEqualTo(128)
    }
}
