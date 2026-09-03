package de.carsten.android.muzzic.logging

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import de.carsten.android.muzzic.BuildConfig

/**
 * Android Logger that implements Java Logger interface with automatic caller detection
 */
class AndroidLogger private constructor(private val tag: String) {
    companion object {

        /**
         * Get logger instance for a specific class
         */
        fun getLogger(clazz: Class<*>): AndroidLogger = AndroidLogger(clazz.simpleName)

        /**
         * Get logger instance with custom tag
         */
        fun getLogger(tag: String): AndroidLogger = AndroidLogger(tag)
    }

    /**
     * Get caller information from stack trace
     */
    private fun getCallerInfo(): String = try {
        val stackTrace = Throwable().stackTrace
        val loggerClassName = AndroidLogger::class.java.name
        val element = stackTrace.firstOrNull { it.className != loggerClassName }
        val className = element?.className?.substringAfterLast('.') ?: "Unknown"
        val methodName = element?.methodName ?: "unknown"
        "[$className::$methodName]"
    } catch (e: Exception) {
        "[Error::getCallerInfo]"
    }

    /**
     * Format message with caller information
     */
    private fun formatMessage(message: String): String = "${getCallerInfo()} $message"

    /**
     * Check if logging is enabled
     *
     * @param level log level
     * @return [Boolean]
     */
    private fun loggable(level: Int): Boolean = BuildConfig.DEBUG || Log.isLoggable(tag, level)

    /**
     * Debug level logging
     */
    fun debug(message: String) {
        if (isDebugEnabled()) {
            Log.d(tag, formatMessage(message))
        }
    }

    fun d(message: String) = this.debug(message)

    fun debug(message: String, throwable: Throwable) {
        if (isDebugEnabled()) {
            Log.d(tag, formatMessage(message), throwable)
        }
    }

    fun d(message: String, throwable: Throwable) = this.debug(message, throwable)

    /**
     * Info level logging
     */
    fun info(message: String) {
        if (isInfoEnabled()) {
            Log.i(tag, formatMessage(message))
        }
    }

    fun i(message: String) = this.info(message)

    fun info(message: String, throwable: Throwable) {
        if (isInfoEnabled()) {
            Log.i(tag, formatMessage(message), throwable)
        }
    }

    fun i(message: String, throwable: Throwable) = this.info(message, throwable)

    /**
     * Warning level logging
     */
    fun warning(message: String) {
        if (isWarningEnabled()) {
            Log.w(tag, formatMessage(message))
        }
    }

    fun w(message: String) = this.warning(message)

    fun warning(message: String, throwable: Throwable) {
        if (isWarningEnabled()) {
            Log.w(tag, formatMessage(message), throwable)
        }
    }

    fun w(message: String, throwable: Throwable) = this.warning(message, throwable)

    /**
     * Error level logging
     */
    fun error(message: String) {
        if (isErrorEnabled()) {
            Log.e(tag, formatMessage(message))
        }
    }

    fun e(message: String) = this.error(message)

    fun error(message: String, throwable: Throwable) {
        if (isErrorEnabled()) {
            Log.e(tag, formatMessage(message), throwable)
        }
    }

    fun e(message: String, throwable: Throwable) = this.error(message, throwable)

    // Additional convenience methods

    /**
     * Check if debug logging is enabled
     */
    fun isDebugEnabled(): Boolean = loggable(Log.DEBUG)

    /**
     * Check if info logging is enabled
     */
    fun isInfoEnabled(): Boolean = loggable(Log.INFO)

    /**
     * Check if warning logging is enabled
     */
    fun isWarningEnabled(): Boolean = loggable(Log.WARN)

    /**
     * Check if error logging is enabled
     */
    fun isErrorEnabled(): Boolean = loggable(Log.ERROR)
}

/**
 * Extension functions for easier usage.
 * Get logger for any class.
 */
inline fun <reified T> T.logger(): AndroidLogger = AndroidLogger.getLogger(T::class.java)

/**
 * Get logger for any tag
 *
 * @param tag logger tag as [String]
 */
fun logger(tag: String): AndroidLogger = AndroidLogger.getLogger(tag)

/**
 * Get composable logger for any tag
 *
 * @param tag logger tag [String]
 */
@Composable
fun rememberLogger(tag: String): AndroidLogger = remember { logger(tag) }
