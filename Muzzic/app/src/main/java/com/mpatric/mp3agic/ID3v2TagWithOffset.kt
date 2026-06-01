package com.mpatric.mp3agic

class ID3v2TagWithOffset(bytes: ByteArray) : AbstractID3v2Tag(bytes) {
    override fun unpackFlags(bytes: ByteArray?) {
        // Implementation depends on ID3 version, but for reading frames,
        // the base AbstractID3v2Tag handles the common logic.
    }

    override fun packFlags(bytes: ByteArray?, offset: Int) {
        // Not needed for reading
    }

    override fun createFrame(bytes: ByteArray?, offset: Int): ID3v2Frame? = ID3v2FrameWithOffset(bytes, offset)

    fun getApicFrame(): ID3v2FrameWithOffset? {
        val frameSet = frameSets["APIC"] ?: frameSets["PIC"] ?: return null
        return frameSet.frames.firstOrNull() as? ID3v2FrameWithOffset
    }
}
