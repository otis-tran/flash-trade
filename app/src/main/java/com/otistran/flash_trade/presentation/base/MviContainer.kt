package com.otistran.flash_trade.presentation.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel for MVI pattern.
 * Manages state and side effects with unidirectional data flow.
 */
abstract class MviContainer<S : MviState, I : MviIntent, E : MviSideEffect>(
    initialState: S
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _sideEffect = Channel<E>(capacity = Channel.CONFLATED)
    val sideEffect = _sideEffect.receiveAsFlow()

    protected val currentState: S get() = _state.value

    /**
     * Process user intent and update state accordingly.
     */
    abstract fun onIntent(intent: I)

    /**
     * Update state with reducer function.
     */
    protected fun reduce(reducer: S.() -> S) {
        _state.value = currentState.reducer()
    }

    /**
     * Emit a side effect (one-time event).
     */
    protected fun emitSideEffect(effect: E) {
        viewModelScope.launch {
            _sideEffect.send(effect)
        }
    }
}
