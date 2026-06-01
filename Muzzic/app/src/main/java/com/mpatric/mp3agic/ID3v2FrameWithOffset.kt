package com.mpatric.mp3agic

data class ID3v2FrameWithOffset
@Throws(InvalidDataException::class)
constructor(val buffer: ByteArray?, val offsetInTag: Int) : ID3v2Frame(buffer, offsetInTag) {
    override fun equals(p0: Any?): Boolean {
        if (this === p0) return true
        if (javaClass != p0?.javaClass) return false
        if (!super.equals(p0)) return false

        p0 as ID3v2FrameWithOffset

        if (offsetInTag != p0.offsetInTag) return false
        if (!buffer.contentEquals(p0.buffer)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + offsetInTag
        result = 31 * result + buffer.contentHashCode()
        return result
    }
}
