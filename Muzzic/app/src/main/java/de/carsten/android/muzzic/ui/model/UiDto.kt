package de.carsten.android.muzzic.ui.model

/**
 * Interface for an DTO used int the jetpack compose UI
 */
interface UiDto {

    /**
     * Gets key from DTO for lazy composables like LazyGrid
     */
    fun lazyKey(): String?

    /**
     * Gets content type from DTO lazy composables like LazyGrid
     */
    fun lazyContentType(): String? = this::class.simpleName
}
