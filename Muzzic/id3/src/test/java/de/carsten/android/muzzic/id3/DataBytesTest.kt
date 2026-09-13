package de.carsten.android.muzzic.id3

import de.carsten.android.muzzic.id3.model.tag.DataBytes
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

class DataBytesTest {
    @Test
    fun `defaults to stream start with full length`() {
        val payload = DataBytes(byteArrayOf(1, 2, 3))
        assertThat(payload.offset).isEqualTo(0)
        assertThat(payload.length).isEqualTo(3)
    }

    @Test
    fun `equality ignores offset and length`() {
        val first = DataBytes(byteArrayOf(1, 2), offset = 10, length = 2)
        val second = DataBytes(byteArrayOf(1, 2), offset = 40, length = 2)
        assertThat(first).isEqualTo(second)
        assertThat(first.hashCode()).isEqualTo(second.hashCode())
        assertThat(first).isNotEqualTo(DataBytes(byteArrayOf(1, 3), offset = 10, length = 2))
    }

    @Test
    fun `toString carries position without dumping bytes`() {
        assertThat(DataBytes(byteArrayOf(1, 2, 3), offset = 20).toString())
            .isEqualTo("DataBytes(offset=20, length=3)")
    }

    @Test
    fun `rejects negative length`() {
        assertThatThrownBy { DataBytes(byteArrayOf(1), length = -1) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
