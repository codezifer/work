package de.carsten.android.muzzic.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.theme.AppTheme

/**
 * Draws the default album artwork used when no cover data is available.
 *
 * The fallback is a plain [Image] instead of an `AsyncImage` with an error painter on purpose:
 * Coil rejects a request whose data is null with a `NullRequestDataException` before any
 * placeholder or error painter is applied, so the "no cover" state must be handled by the
 * composition layer instead of the image loader.
 *
 * @param modifier the modifier to apply to the image.
 * @param contentDescription accessibility description; pass null for purely decorative usage.
 * @param contentScale how the artwork is scaled inside the given bounds.
 */
@Composable
fun DefaultAlbumArt(
    modifier: Modifier = Modifier,
    contentDescription: String? = stringResource(R.string.placeholder_description),
    contentScale: ContentScale = ContentScale.Crop,
) {
    Image(
        painter = painterResource(R.drawable.disc),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
    )
}

@Composable
@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = PREVIEW_DARK_MODE, showBackground = true)
fun DefaultAlbumArtPreview() {
    AppTheme {
        DefaultAlbumArt(modifier = Modifier.size(200.dp))
    }
}
