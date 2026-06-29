package de.carsten.android.muzzic.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.ui.FONT_SIZE_HUGE_TITLE
import de.carsten.android.muzzic.ui.GLASS_CONTAINER_ALPHA
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_LARGE
import de.carsten.android.muzzic.ui.SPACING_MEDIUM
import de.carsten.android.muzzic.ui.navigation.MusicAppState
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.viewmodel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, appState: MusicAppState, onBackClick: () -> Unit, settingsViewModel: SettingsViewModel = koinViewModel()) {
    val musicDirectory by settingsViewModel.musicDirectory.collectAsStateWithLifecycle()
    val playlistDirectory by settingsViewModel.playlistDirectory.collectAsStateWithLifecycle()

    SettingsScreenContent(
        modifier = modifier,
        musicDirectory = musicDirectory,
        playlistDirectory = playlistDirectory,
        settingsViewModel = settingsViewModel,
        onBackClick = onBackClick,
    )
}

@Composable
private fun SettingsScreenContent(
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current,
    musicDirectory: String? = null,
    playlistDirectory: String? = null,
    settingsViewModel: SettingsViewModel? = null,
    onBackClick: () -> Unit = {},
) {
    val musicDirLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri?.let {
            // We need to persist permissions and get the actual path if possible
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            // For now, we store the URI string. A more robust implementation
            // would resolve this to a File path or use DocumentFile.
            settingsViewModel?.updateMusicDirectory(it.toString())
        }
    }

    val playlistDirLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            settingsViewModel?.updatePlaylistDirectory(it.toString())
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = GLASS_CONTAINER_ALPHA))
            .verticalScroll(rememberScrollState())
            .padding(SPACING_LARGE),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SPACING_MEDIUM),
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = stringResource(R.string.settings),
                fontSize = FONT_SIZE_HUGE_TITLE,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(modifier = Modifier.height(SPACING_LARGE))

        SettingsSection(title = stringResource(R.string.library_config)) {
            DirectorySetting(
                label = stringResource(R.string.music_directory),
                path = musicDirectory ?: "Not set",
                onSelect = { musicDirLauncher.launch(null) },
            )
            Spacer(modifier = Modifier.height(SPACING_MEDIUM))
            DirectorySetting(
                label = stringResource(R.string.playlist_directory),
                path = playlistDirectory ?: "Not set",
                onSelect = { playlistDirLauncher.launch(null) },
            )
        }

        Spacer(modifier = Modifier.height(SPACING_LARGE))

        SettingsSection(title = stringResource(R.string.manual_tasks)) {
            Button(
                onClick = { settingsViewModel?.scanMusicLibrary() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.scan_library))
            }
            Spacer(modifier = Modifier.height(SPACING_MEDIUM))
            Button(
                onClick = { settingsViewModel?.importPlaylists() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.scan_playlists))
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = SPACING_MEDIUM))
        content()
    }
}

@Composable
private fun DirectorySetting(label: String, path: String, onSelect: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SPACING_MEDIUM),
        ) {
            OutlinedTextField(
                value = path,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodySmall,
            )
            IconButton(onClick = onSelect) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = stringResource(R.string.select_directory),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE)
fun SettingsScreenPreview() {
    AppTheme {
        SettingsScreenContent(
            musicDirectory = "/storage/emulated/0/Music/Artist",
            playlistDirectory = "/storage/emulated/0/Music/Playlists",
        )
    }
}
