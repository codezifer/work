package de.carsten.android.muzzic.ui.model

import android.net.Uri
import java.io.InputStream

sealed class AlbumArtInput {
    data class FromPath(val audioFilePath: String) : AlbumArtInput()
    data class FromUri(val audioFileUri: Uri) : AlbumArtInput()
    data class FromInputStream(val inputStream: InputStream) : AlbumArtInput()
    data object None : AlbumArtInput()
}
