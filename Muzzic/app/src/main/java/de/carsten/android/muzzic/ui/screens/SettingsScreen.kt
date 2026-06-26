package de.carsten.android.muzzic.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.carsten.android.muzzic.ui.FONT_SIZE_HUGE_TITLE
import de.carsten.android.muzzic.ui.GLASS_CONTAINER_ALPHA
import de.carsten.android.muzzic.ui.SPACING_LARGE
import de.carsten.android.muzzic.ui.SPACING_MEDIUM
import de.carsten.android.muzzic.ui.navigation.MusicAppState
import de.carsten.android.muzzic.viewmodel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    appState: MusicAppState,
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val musicDirectory by viewModel.musicDirectory.collectAsStateWithLifecycle()
    val playlistDirectory by viewModel.playlistDirectory.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val musicDirLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            // We need to persist permissions and get the actual path if possible
            context.contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            // For now, we store the URI string. A more robust implementation
            // would resolve this to a File path or use DocumentFile.
            viewModel.updateMusicDirectory(it.toString())
        }
    }

    val playlistDirLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.updatePlaylistDirectory(it.toString())
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = GLASS_CONTAINER_ALPHA))
            .verticalScroll(rememberScrollState())
            .padding(SPACING_LARGE)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SPACING_MEDIUM)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Settings",
                fontSize = FONT_SIZE_HUGE_TITLE,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(SPACING_LARGE))

        SettingsSection(title = "Library Configuration") {
            DirectorySetting(
                label = "Music Directory",
                path = musicDirectory ?: "Not set",
                onSelect = { musicDirLauncher.launch(null) }
            )
            Spacer(modifier = Modifier.height(SPACING_MEDIUM))
            DirectorySetting(
                label = "Playlist Directory",
                path = playlistDirectory ?: "Not set",
                onSelect = { playlistDirLauncher.launch(null) }
            )
        }

        Spacer(modifier = Modifier.height(SPACING_LARGE))

        SettingsSection(title = "Manual Tasks") {
            Button(
                onClick = { viewModel.scanMusicLibrary() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Scan Music Library")
            }
            Spacer(modifier = Modifier.height(SPACING_MEDIUM))
            Button(
                onClick = { viewModel.importPlaylists() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Import Playlists")
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = SPACING_MEDIUM))
        content()
    }
}

@Composable
fun DirectorySetting(label: String, path: String, onSelect: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SPACING_MEDIUM)
        ) {
            OutlinedTextField(
                value = path,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodySmall
            )
            IconButton(onClick = onSelect) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Select Directory",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
