package de.carsten.android.muzzic.logging

import android.util.Log

/**
 * Android Logger that implements Java Logger interface with automatic caller detection
 */
class AndroidLogger private constructor(private val tag: String) {
    companion object {
        private const val CALL_STACK_INDEX = 4 // Index to get the actual caller

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
    private fun getCallerInfo(): String =
        try {
            val stackTrace = Thread.currentThread().stackTrace
            if (stackTrace.size > CALL_STACK_INDEX) {
                val element = stackTrace[CALL_STACK_INDEX]
                val className = element.className.substringAfterLast('.')
                val methodName = element.methodName
                "[$className::$methodName]"
            } else {
                "[Unknown::unknown]"
            }
        } catch (e: Exception) {
            "[Error::getCallerInfo]"
        }

    /**
     * Format message with caller information
     */
    private fun formatMessage(message: String): String = "${getCallerInfo()} $message"

    /**
     * Debug level logging
     */
    fun debug(message: String) {
        if (Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, formatMessage(message))
        }
    }

    fun debug(
        message: String,
        throwable: Throwable,
    ) {
        if (Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, formatMessage(message), throwable)
        }
    }

    /**
     * Info level logging
     */
    fun info(message: String) {
        if (Log.isLoggable(tag, Log.INFO)) {
            Log.i(tag, formatMessage(message))
        }
    }

    fun info(
        message: String,
        throwable: Throwable,
    ) {
        if (Log.isLoggable(tag, Log.INFO)) {
            Log.i(tag, formatMessage(message), throwable)
        }
    }

    /**
     * Warning level logging
     */
    fun warning(message: String) {
        if (Log.isLoggable(tag, Log.WARN)) {
            Log.w(tag, formatMessage(message))
        }
    }

    fun warning(
        message: String,
        throwable: Throwable,
    ) {
        if (Log.isLoggable(tag, Log.WARN)) {
            Log.w(tag, formatMessage(message), throwable)
        }
    }

    /**
     * Error level logging
     */
    fun error(message: String) {
        if (Log.isLoggable(tag, Log.ERROR)) {
            Log.e(tag, formatMessage(message))
        }
    }

    fun error(
        message: String,
        throwable: Throwable,
    ) {
        if (Log.isLoggable(tag, Log.ERROR)) {
            Log.e(tag, formatMessage(message), throwable)
        }
    }

    // Additional convenience methods

    /**
     * Check if debug logging is enabled
     */
    fun isDebugEnabled(): Boolean = Log.isLoggable(tag, Log.DEBUG)

    /**
     * Check if info logging is enabled
     */
    fun isInfoEnabled(): Boolean = Log.isLoggable(tag, Log.INFO)

    /**
     * Check if warning logging is enabled
     */
    fun isWarningEnabled(): Boolean = Log.isLoggable(tag, Log.WARN)

    /**
     * Check if error logging is enabled
     */
    fun isErrorEnabled(): Boolean = Log.isLoggable(tag, Log.ERROR)
}

/**
 * Extension functions for easier usage
 */

/**
 * Get logger for any class
 */
inline fun <reified T> T.logger(): AndroidLogger = AndroidLogger.getLogger(T::class.java)
