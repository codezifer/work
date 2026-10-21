package de.carsten.android.muzzic.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * How long the UI state keeps collecting after the last subscriber leaves.
 *
 * The grace period covers configuration changes, so a rotation does not restart
 * the underlying database queries.
 */
const val UI_STATE_SUBSCRIPTION_TIMEOUT_MS = 5_000L

interface UiStateViewModel<UISTATE> {
    val uiState: StateFlow<UISTATE>
}

/**
 * Converts a screen state stream into a lifecycle-aware [StateFlow].
 *
 * Collecting starts with the first UI subscriber and stops
 * [UI_STATE_SUBSCRIPTION_TIMEOUT_MS] after the last one leaves.
 *
 * @param scope scope that hosts the upstream collection, usually `viewModelScope`.
 * @param initialValue state emitted before the first upstream value arrives.
 * @return the hot, replaying UI state flow.
 */
fun <UISTATE> Flow<UISTATE>.asUiState(scope: CoroutineScope, initialValue: UISTATE): StateFlow<UISTATE> =
    stateIn(scope, SharingStarted.WhileSubscribed(UI_STATE_SUBSCRIPTION_TIMEOUT_MS), initialValue)
