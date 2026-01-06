package com.otistran.flash_trade.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.repository.SettingsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base ViewModel for MVI pattern.
 * Manages state and side effects with unidirectional data flow.
 *
 * @param S State type extending [UiState]
 * @param I Event/Intent type extending [UiEvent]
 * @param E Effect type extending [UiEffect]
 */
abstract class BaseViewModel<S : UiState, I : UiEvent, E : UiEffect>(
    initialState: S
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effect = Channel<E>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    /**
     * Current state snapshot.
     */
    protected val currentState: S get() = _state.value

    /**
     * Process user event/intent and update state accordingly.
     * This is the single entry point for all UI events.
     */
    abstract fun onEvent(event: I)

    /**
     * Update state using a reducer function.
     * Thread-safe state update.
     *
     * @param reducer Lambda that receives current state and returns new state
     */
    protected fun setState(reducer: S.() -> S) {
        _state.update { it.reducer() }
    }

    /**
     * Emit a one-time side effect (navigation, toast, dialog, etc.)
     * Effects are consumed only once and won't survive configuration changes.
     *
     * @param effect The effect to emit
     */
    protected fun setEffect(effect: E) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }

    /**
     * Emit a side effect with a builder pattern.
     */
    protected inline fun setEffect(builder: () -> E) {
        setEffect(builder())
    }

    /**
     * Observe network mode changes from settings.
     * Provides a common pattern for ViewModels that need to react to network changes.
     *
     * Uses distinctUntilChanged at Settings level to prevent duplicate emissions
     * when other settings (theme, autoSell) change but network stays the same.
     *
     * @param settingsRepository Repository to observe settings from
     * @param onNetworkChanged Callback invoked when network mode changes
     */
    protected fun observeNetworkMode(
        settingsRepository: SettingsRepository,
        onNetworkChanged: suspend (NetworkMode) -> Unit
    ) {
        viewModelScope.launch {
            settingsRepository.observeSettings()
                .distinctUntilChanged { old, new -> old.networkMode == new.networkMode }
                .map { it.networkMode }
                .collect { onNetworkChanged(it) }
        }
    }
}