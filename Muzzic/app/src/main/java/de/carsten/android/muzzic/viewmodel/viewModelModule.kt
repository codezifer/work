package de.carsten.android.muzzic.viewmodel

import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule =
    module {
        viewModel {
            PlayerViewModel(
                repository = get(),
                mediaLibraryManager = get(),
                application = get()
            )
        }
        viewModel {
            LibraryViewModel(
                musicRepository = get(),
                artistRepository = get(),
                albumRepository = get(),
                genreRepository = get(),
                playlistRepository = get(),
                playingQueueRepository = get(),
                mediaLibraryManager = get()
            )
        }
        viewModelOf(::StatisticsViewModel)
        viewModel {
            PlayingQueueViewModel(
                repository = get(),
                playlistRepository = get(),
                mediaLibraryManager = get()
            )
        }
        viewModel {
            SelectionViewModel(
                artistRepository = get(),
                albumRepository = get(),
                songRepository = get(),
                playingQueueRepository = get()
            )
        }
        viewModel {
            ArtistAlbumsViewModel(
                savedStateHandle = get(),
                albumRepository = get(),
                mediaLibraryManager = get()
            )
        }
        viewModel {
            AlbumSongsViewModel(
                savedStateHandle = get(),
                albumRepository = get(),
                mediaLibraryManager = get()
            )
        }
        viewModel {
            PlaylistViewModel(
                repository = get(),
                playingQueueRepository = get()
            )
        }
        viewModel {
            GenresViewModel(
                savedStateHandle = get(),
                genreRepository = get(),
                artistRepository = get(),
            )
        }
    }
