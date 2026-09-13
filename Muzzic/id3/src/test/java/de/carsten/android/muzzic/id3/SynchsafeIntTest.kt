package de.carsten.android.muzzic.id3

import de.carsten.android.muzzic.id3.exceptions.Id3Exception
import de.carsten.android.muzzic.id3.model.tag.SynchsafeInt
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

class SynchsafeIntTest {
    @Test
    fun `decode masks high bits tolerantly`() {
        assertThat(SynchsafeInt.decode(byteArrayOf(0x00, 0x00, 0x02, 0x01))).isEqualTo(257)
    }

    @Test
    fun `encode decode roundtrip`() {
        listOf(0, 1, 127, 128, 257, 1024, SynchsafeInt.MAX_VALUE).forEach { value ->
            assertThat(SynchsafeInt.decode(SynchsafeInt.encode(value))).isEqualTo(value)
        }
    }

    @Test
    fun `encode keeps bit seven cleared`() {
        SynchsafeInt.encode(SynchsafeInt.MAX_VALUE).forEach { byte ->
            assertThat(byte.toInt() and 0x80).isEqualTo(0)
        }
    }

    @Test
    fun `encode rejects out of range values`() {
        assertThatThrownBy { SynchsafeInt.encode(-1) }.isInstanceOf(Id3Exception::class.java)
        assertThatThrownBy { SynchsafeInt.encode(SynchsafeInt.MAX_VALUE + 1) }.isInstanceOf(Id3Exception::class.java)
    }

    @Test
    fun `decode rejects truncated input`() {
        assertThatThrownBy { SynchsafeInt.decode(byteArrayOf(0x00, 0x00, 0x01)) }
            .isInstanceOf(Id3Exception::class.java)
    }
}
