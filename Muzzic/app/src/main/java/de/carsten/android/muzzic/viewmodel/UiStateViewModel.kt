package de.carsten.android.muzzic.viewmodel

import kotlinx.coroutines.flow.StateFlow

interface UiStateViewModel<UISTATE> {
    val uiState: StateFlow<UISTATE>
}
