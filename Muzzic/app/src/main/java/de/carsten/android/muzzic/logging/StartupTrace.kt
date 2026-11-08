package de.carsten.android.muzzic.logging

import android.os.Process
import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap

/**
 * Cold-start timeline for the startup cover latency measurement.
 *
 * All markers are relative to the process start, so logcat lines from different runs are
 * directly comparable and can be cross-checked against `am start -W`.
 */
object StartupTrace {
    private val logger = logger("StartupTrace")
    private val processStartUptimeMs = Process.getStartUptimeMillis()
    private val markedOnce = ConcurrentHashMap.newKeySet<String>()

    /**
     * Milliseconds since the process was forked.
     */
    fun elapsedMs(): Long = SystemClock.uptimeMillis() - processStartUptimeMs

    /**
     * Logs [name] with the milliseconds elapsed since process start.
     */
    fun mark(name: String) {
        logger.info("$name @ +${elapsedMs()}ms")
    }

    /**
     * Logs [name] only on its first call; later calls are ignored.
     */
    fun markOnce(name: String) {
        if (markedOnce.add(name)) mark(name)
    }
}
