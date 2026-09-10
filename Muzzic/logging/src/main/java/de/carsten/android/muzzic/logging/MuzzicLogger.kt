package de.carsten.android.muzzic.logging

interface MuzzicLogger {
    fun d(message: String)
    fun debug(message: String)
    fun d(message: String, throwable: Throwable)
    fun debug(message: String, throwable: Throwable)

    fun i(message: String)
    fun info(message: String)
    fun i(message: String, throwable: Throwable)
    fun info(message: String, throwable: Throwable)

    fun w(message: String)
    fun warning(message: String)
    fun w(message: String, throwable: Throwable)
    fun warning(message: String, throwable: Throwable)

    fun e(message: String)
    fun error(message: String)
    fun e(message: String, throwable: Throwable)
    fun error(message: String, throwable: Throwable)

    fun isDebugEnabled(): Boolean
    fun isInfoEnabled(): Boolean
    fun isWarningEnabled(): Boolean
    fun isErrorEnabled(): Boolean
}
