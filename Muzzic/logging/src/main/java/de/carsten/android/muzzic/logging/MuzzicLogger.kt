package de.carsten.android.muzzic.logging

/**
 * Minimal logging facade shared by all Muzzic modules.
 *
 * Implementations live outside this module (the app provides the Android
 * backed logger), which keeps `id3` and `playlist` free of Android APIs.
 */
interface MuzzicLogger {
    fun debug(message: String)

    fun debug(message: String, throwable: Throwable)

    fun info(message: String)

    fun info(message: String, throwable: Throwable)

    fun warning(message: String)

    fun warning(message: String, throwable: Throwable)

    fun error(message: String)

    fun error(message: String, throwable: Throwable)
}
