package de.carsten.android.muzzic.ui.model

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents the source for an album cover, allowing for type-safe handling of different
 * image sources like file paths or placeholder vector drawables.
 */
sealed class CoverSource {
    /** A cover loaded from a file path. */
    data class FromPath(
        val path: String,
    ) : CoverSource()

    /** A placeholder cover represented by an ImageVector. */
    data class FromVector(
        val imageVector: ImageVector,
    ) : CoverSource()
}
