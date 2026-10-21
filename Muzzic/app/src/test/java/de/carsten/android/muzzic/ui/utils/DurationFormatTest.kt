package de.carsten.android.muzzic.ui.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DurationFormatTest {
    private val fiftyMinutesMs = 50L * 60 * 1000
    private val oneHourThirtyMs = 90L * 60 * 1000

    @Test
    fun `formats durations below one hour with zero hours`() {
        assertThat(formatDurationLong(0L)).isEqualTo("00h:00min")
        assertThat(formatDurationLong(fiftyMinutesMs)).isEqualTo("00h:50min")
    }

    @Test
    fun `formats durations from one hour on`() {
        assertThat(formatDurationLong(oneHourThirtyMs)).isEqualTo("01h:30min")
        assertThat(formatDurationLong(10L * 60 * 60 * 1000)).isEqualTo("10h:00min")
    }

    @Test
    fun `truncates seconds to full minutes`() {
        assertThat(formatDurationLong(fiftyMinutesMs + 59_000L)).isEqualTo("00h:50min")
    }
}
