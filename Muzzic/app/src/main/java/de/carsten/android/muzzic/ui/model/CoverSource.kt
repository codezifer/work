package de.carsten.android.muzzic.ui.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents the source for an album cover, allowing for type-safe handling of different
 * image sources like file paths or placeholder vector drawables.
 */
@Immutable
sealed class CoverSource {
    /** A cover loaded from a file path. */
    @Immutable
    data class FromPath(val path: String) : CoverSource()

    /** A placeholder cover represented by an ImageVector. */
    @Immutable
    data class FromVector(val imageVector: ImageVector) : CoverSource()
}
