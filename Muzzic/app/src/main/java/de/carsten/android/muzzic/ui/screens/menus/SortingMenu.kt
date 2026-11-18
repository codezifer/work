package de.carsten.android.muzzic.ui.screens.menus

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_MEDIUM
import de.carsten.android.muzzic.ui.theme.AppTheme

/**
 * Entry of a [SortingMenu] with a label, a leading icon and its action.
 */
data class MenuItem(@StringRes val labelRes: Int, val icon: ImageVector, val onClick: () -> Unit)

/**
 * Icon button that opens a dropdown menu and closes it after an entry was clicked.
 *
 * @param icon Trigger icon of the button.
 * @param iconDescriptionRes Accessibility label for the trigger icon.
 * @param menuItems Entries rendered in the dropdown.
 */
@Composable
fun SortingMenu(icon: ImageVector, @StringRes iconDescriptionRes: Int, menuItems: List<MenuItem>, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }

    SortingMenuContent(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        icon = icon,
        iconDescriptionRes = iconDescriptionRes,
        menuItems = menuItems,
        modifier = modifier,
    )
}

@Composable
private fun SortingMenuContent(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    icon: ImageVector,
    @StringRes iconDescriptionRes: Int,
    menuItems: List<MenuItem>,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        SortingMenuTrigger(icon = icon, iconDescriptionRes = iconDescriptionRes) { onExpandedChange(true) }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            menuItems.forEach { item ->
                SortingMenuEntry(item = item) {
                    onExpandedChange(false)
                    item.onClick()
                }
            }
        }
    }
}

@Composable
private fun SortingMenuTrigger(icon: ImageVector, @StringRes iconDescriptionRes: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(iconDescriptionRes),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SortingMenuEntry(item: MenuItem, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(item.labelRes)) },
        leadingIcon = {
            // Label already describes the entry, so the icon stays silent for TalkBack.
            Icon(imageVector = item.icon, contentDescription = null)
        },
        onClick = onClick,
    )
}

private val previewMenuItems =
    listOf(
        MenuItem(R.string.sort_metadata, Icons.AutoMirrored.Filled.QueueMusic) {},
        MenuItem(R.string.shuffle, Icons.Filled.Shuffle) {},
    )

@Composable
@Preview(showBackground = true)
@Preview(uiMode = PREVIEW_DARK_MODE, showBackground = true)
fun SortingMenuPreview() {
    AppTheme {
        SortingMenuContent(
            expanded = false,
            onExpandedChange = {},
            icon = Icons.AutoMirrored.Filled.Sort,
            iconDescriptionRes = R.string.sort,
            menuItems = previewMenuItems,
        )
    }
}

@Composable
@Preview(showBackground = true)
@Preview(uiMode = PREVIEW_DARK_MODE, showBackground = true)
fun SortingMenuExpandedPreview() {
    AppTheme {
        Column(modifier = Modifier.padding(SPACING_MEDIUM)) {
            SortingMenuTrigger(
                icon = Icons.AutoMirrored.Filled.Sort,
                iconDescriptionRes = R.string.sort,
                onClick = {},
            )

            // DropdownMenu lives in a Popup that previews do not render, so its container is rebuilt here.
            Surface(
                shape = MenuDefaults.shape,
                color = MenuDefaults.containerColor,
                tonalElevation = MenuDefaults.TonalElevation,
                shadowElevation = MenuDefaults.ShadowElevation,
            ) {
                Column(
                    modifier =
                    Modifier
                        .width(IntrinsicSize.Max)
                        .padding(vertical = SPACING_MEDIUM),
                ) {
                    previewMenuItems.forEach { item ->
                        SortingMenuEntry(item = item, onClick = {})
                    }
                }
            }
        }
    }
}
