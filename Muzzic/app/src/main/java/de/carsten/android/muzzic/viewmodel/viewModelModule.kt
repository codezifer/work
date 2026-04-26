package de.carsten.android.muzzic.viewmodel

import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule =
    module {
        viewModelOf(::PlayerViewModel)
        viewModel {
            LibraryViewModel(
                musicRepository = get(),
                artistRepository = get(),
                albumRepository = get(),
                genreRepository = get(),
                playlistRepository = get(),
                mediaLibraryManager = get()
            )
        }
        viewModelOf(::StatisticsViewModel)
        viewModelOf(::PlayingQueueViewModel)
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
    }
