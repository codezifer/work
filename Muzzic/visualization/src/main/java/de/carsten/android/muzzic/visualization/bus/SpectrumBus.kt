package de.carsten.android.muzzic.visualization.bus

import de.carsten.android.muzzic.visualization.MAX_SPECTRUM_BANDS
import de.carsten.android.muzzic.visualization.SPECTRUM_BUS_CAPACITY_FRAMES
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * A lock-free, zero-allocation Single-Producer Single-Consumer (SPSC) ring buffer using Seqlocks.
 *
 * Decouples the audio processing thread (Producer) from the OpenGL rendering thread (Consumer).
 *
 * @param capacity Number of pre-allocated frame slots (default: 64 slots, ~740ms buffer at 86Hz).
 * @param maxBands Maximum supported spectrum band count per slot (default: 64).
 */
class SpectrumBus(val capacity: Int = SPECTRUM_BUS_CAPACITY_FRAMES, val maxBands: Int = MAX_SPECTRUM_BANDS) {
    class Slot(maxBands: Int) {
        val seq = AtomicInteger(0)

        @Volatile var timestampNanos: Long = 0L

        @Volatile var bandCount: Int = 0
        val values = FloatArray(maxBands)
    }

    private val slots = Array(capacity) { Slot(maxBands) }
    private val writeIndex = AtomicLong(0L)

    /**
     * Writes a new spectrum frame to the ring buffer (called from the audio thread).
     *
     * @param timestampNanos Frame timestamp from `System.nanoTime()`.
     * @param values Normalized band amplitudes 0.0..1.0.
     * @param bandCount Number of active bands.
     */
    fun write(timestampNanos: Long, values: FloatArray, bandCount: Int) {
        val idx = writeIndex.get()
        val slot = slots[(idx % capacity).toInt()]

        // 1. Begin write by setting sequence to odd
        val curSeq = slot.seq.get()
        val writingSeq = if (curSeq % 2 == 0) curSeq + 1 else curSeq + 2
        slot.seq.set(writingSeq)

        // 2. Write frame data
        slot.timestampNanos = timestampNanos
        val count = bandCount.coerceAtMost(maxBands)
        slot.bandCount = count
        System.arraycopy(values, 0, slot.values, 0, count)

        // 3. Complete write by setting sequence to even
        slot.seq.lazySet(writingSeq + 1)

        // 4. Advance write index
        writeIndex.lazySet(idx + 1)
    }

    /**
     * Reads the newest spectrum frame with `timestampNanos <= targetNanos` (called from the GL thread).
     *
     * @param targetNanos Target timestamp (`now - visualLatencyNanos`).
     * @param outValues Destination array for copied band values (length >= maxBands).
     * @return Number of copied bands, or 0 if no valid frame was found.
     */
    fun readAtOrBefore(targetNanos: Long, outValues: FloatArray): Int {
        val currentWrite = writeIndex.get()
        if (currentWrite == 0L) return 0

        val maxSearch = capacity.toLong().coerceAtMost(currentWrite)

        for (i in 0L until maxSearch) {
            val idx = currentWrite - 1L - i
            val slot = slots[(idx % capacity).toInt()]

            val seqBefore = slot.seq.get()
            if (seqBefore % 2 != 0) continue // Slot is currently being written

            val ts = slot.timestampNanos
            if (ts <= targetNanos) {
                val count = slot.bandCount
                if (outValues.size < count) return 0

                System.arraycopy(slot.values, 0, outValues, 0, count)

                val seqAfter = slot.seq.get()
                if (seqBefore == seqAfter) {
                    // Valid lock-free read
                    return count
                }
            }
        }
        return 0
    }

    /**
     * Returns the timestamp of the newest written frame, or 0L if empty.
     */
    fun newestTimestampNanos(): Long {
        val currentWrite = writeIndex.get()
        if (currentWrite == 0L) return 0L
        val slot = slots[((currentWrite - 1L) % capacity).toInt()]
        return slot.timestampNanos
    }
}
