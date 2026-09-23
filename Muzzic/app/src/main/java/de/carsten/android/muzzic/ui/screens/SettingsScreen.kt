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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import de.carsten.android.muzzic.ui.SPACING_TINY
import de.carsten.android.muzzic.ui.navigation.MusicAppState
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.viewmodel.SettingsViewModel
import de.carsten.android.muzzic.visualization.component.VisualizerEngine
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, appState: MusicAppState, onBackClick: () -> Unit, settingsViewModel: SettingsViewModel = koinViewModel()) {
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreenContent(
        modifier = modifier,
        musicDirectory = uiState.musicDirectory,
        playlistDirectory = uiState.playlistDirectory,
        visualizerEngine = uiState.visualizerEngine,
        projectMPreset = uiState.projectMPreset,
        availablePresets = uiState.availablePresets,
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
    visualizerEngine: VisualizerEngine = VisualizerEngine.BARS,
    projectMPreset: String? = null,
    availablePresets: List<String> = emptyList(),
    settingsViewModel: SettingsViewModel? = null,
    onBackClick: () -> Unit = {},
) {
    val musicDirLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
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

    var showAboutDialog by remember { mutableStateOf(false) }
    var showClearDatabaseDialog by remember { mutableStateOf(false) }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    if (showClearDatabaseDialog) {
        ClearDatabaseDialog(
            onConfirm = {
                settingsViewModel?.clearLibrary()
                showClearDatabaseDialog = false
            },
            onDismiss = { showClearDatabaseDialog = false },
        )
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
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { showAboutDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = stringResource(R.string.about_muzzic),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(modifier = Modifier.height(SPACING_LARGE))

        SettingsSection(title = stringResource(R.string.library_config)) {
            DirectorySetting(
                label = stringResource(R.string.music_directory),
                path = musicDirectory ?: stringResource(R.string.not_set),
                onSelect = { musicDirLauncher.launch(null) },
            )
            Spacer(modifier = Modifier.height(SPACING_MEDIUM))
            DirectorySetting(
                label = stringResource(R.string.playlist_directory),
                path = playlistDirectory ?: stringResource(R.string.not_set),
                onSelect = { playlistDirLauncher.launch(null) },
            )
        }

        Spacer(modifier = Modifier.height(SPACING_LARGE))

        SettingsSection(title = stringResource(R.string.visualization_setting)) {
            VisualizerSetting(
                currentEngine = visualizerEngine,
                currentPreset = projectMPreset,
                availablePresets = availablePresets,
                onEngineSelected = { engine ->
                    settingsViewModel?.updateVisualizerEngine(engine)
                },
                onPresetSelected = { presetName ->
                    settingsViewModel?.updateProjectMPreset(presetName)
                },
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
            Spacer(modifier = Modifier.height(SPACING_MEDIUM))
            Button(
                onClick = { showClearDatabaseDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.clear_database))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VisualizerSetting(
    currentEngine: VisualizerEngine,
    currentPreset: String?,
    availablePresets: List<String>,
    onEngineSelected: (VisualizerEngine) -> Unit,
    onPresetSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM)) {
        Text(
            text = stringResource(R.string.visualizer_engine),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        var engineExpanded by remember { mutableStateOf(false) }
        val engineLabel = when (currentEngine) {
            VisualizerEngine.BARS -> stringResource(R.string.visualizer_engine_bars)
            VisualizerEngine.PROJECT_M -> stringResource(R.string.visualizer_engine_projectm)
        }

        ExposedDropdownMenuBox(
            expanded = engineExpanded,
            onExpandedChange = { engineExpanded = !engineExpanded },
        ) {
            OutlinedTextField(
                value = engineLabel,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = engineExpanded) },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium,
            )
            ExposedDropdownMenu(
                expanded = engineExpanded,
                onDismissRequest = { engineExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.visualizer_engine_bars)) },
                    onClick = {
                        onEngineSelected(VisualizerEngine.BARS)
                        engineExpanded = false
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.visualizer_engine_projectm)) },
                    onClick = {
                        onEngineSelected(VisualizerEngine.PROJECT_M)
                        engineExpanded = false
                    },
                )
            }
        }

        if (currentEngine == VisualizerEngine.PROJECT_M && availablePresets.isNotEmpty()) {
            Spacer(modifier = Modifier.height(SPACING_TINY))
            Text(
                text = stringResource(R.string.projectm_preset),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            var presetExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = presetExpanded,
                onExpandedChange = { presetExpanded = !presetExpanded },
            ) {
                OutlinedTextField(
                    value = currentPreset ?: availablePresets.first(),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = presetExpanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
                ExposedDropdownMenu(
                    expanded = presetExpanded,
                    onDismissRequest = { presetExpanded = false },
                ) {
                    availablePresets.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text(preset) },
                            onClick = {
                                onPresetSelected(preset)
                                presetExpanded = false
                            },
                        )
                    }
                }
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
private fun ClearDatabaseDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.clear_database_confirm_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Text(text = stringResource(R.string.clear_database_confirm_message))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
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
