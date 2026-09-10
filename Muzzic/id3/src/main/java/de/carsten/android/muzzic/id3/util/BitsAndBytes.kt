package de.carsten.android.muzzic.id3.util

import java.nio.ByteBuffer

object BitsAndBytes {
    fun grab(buf: ByteBuffer, offset: Int, lenght: Int): ByteArray {
        try {
            buf.rewind()
            val bytes = ByteArray(lenght)
            buf.position(offset)
            buf.get(bytes)
            return bytes
        } catch (e: OutOfMemoryError) {

        }
    }
}
