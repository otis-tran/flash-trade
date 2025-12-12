# Phase 01: Core Utils & Base MVI

## Context
- **Parent Plan:** [plan.md](plan.md)
- **Dependencies:** None (first phase)
- **Docs:** [system-architecture.md](../../docs/system-architecture.md), [code-standards.md](../../docs/code-standards.md)

## Overview
| Field | Value |
|-------|-------|
| Date | 2024-12-12 |
| Priority | High |
| Implementation Status | Complete |
| Review Status | Complete |

**Description:** Create foundational utility classes and base MVI pattern components.

## Key Insights
- MVI requires: Intent (user action) → State (immutable) → UI
- Result wrapper needed for domain operations
- MviContainer manages state + side effects

## Requirements
1. Result sealed class for success/error handling
2. Base MVI interfaces (Intent, State)
3. MviContainer for ViewModel state management
4. Side effects channel for one-time events

## Architecture

```
util/
└── Result.kt              # Success/Error wrapper

presentation/base/
├── MviIntent.kt           # Marker interface for intents
├── MviState.kt            # Marker interface for states
├── MviSideEffect.kt       # Marker interface for side effects
└── MviContainer.kt        # State container (ViewModel base)
```

## Related Code Files
- `app/src/main/java/com/otistran/flash_trade/MainActivity.kt` (existing)

## Implementation Steps

### Step 1: Create Result.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/util/Result.kt`

```kotlin
package com.otistran.flash_trade.util

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()
    data object Loading : Result<Nothing>()

    val isSuccess get() = this is Success
    val isError get() = this is Error
    val isLoading get() = this is Loading

    fun getOrNull(): T? = (this as? Success)?.data
    fun errorOrNull(): String? = (this as? Error)?.message
}

inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
    is Result.Loading -> this
}

inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onError(action: (String, Throwable?) -> Unit): Result<T> {
    if (this is Result.Error) action(message, cause)
    return this
}
```

### Step 2: Create MviIntent.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/presentation/base/MviIntent.kt`

```kotlin
package com.otistran.flash_trade.presentation.base

/**
 * Marker interface for MVI intents.
 * All feature intents should extend this.
 */
interface MviIntent
```

### Step 3: Create MviState.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/presentation/base/MviState.kt`

```kotlin
package com.otistran.flash_trade.presentation.base

/**
 * Marker interface for MVI states.
 * All feature states should extend this.
 */
interface MviState
```

### Step 4: Create MviSideEffect.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/presentation/base/MviSideEffect.kt`

```kotlin
package com.otistran.flash_trade.presentation.base

/**
 * Marker interface for MVI side effects.
 * One-time events like navigation, toasts, etc.
 */
interface MviSideEffect
```

### Step 5: Create MviContainer.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/presentation/base/MviContainer.kt`

```kotlin
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

    private val _sideEffect = Channel<E>(Channel.BUFFERED)
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
```

## Todo List
- [x] Create util/ directory
- [x] Create Result.kt
- [x] Create presentation/base/ directory
- [x] Create MviIntent.kt
- [x] Create MviState.kt
- [x] Create MviSideEffect.kt
- [x] Create MviContainer.kt
- [x] Verify compilation
- [x] Unit tests created and passing
- [x] Phase 01 marked complete

## Success Criteria
- [x] All 5 files created
- [x] Each file <200 lines
- [x] No compilation errors
- [x] Result class has map, onSuccess, onError extensions
- [x] Unit tests passing (12 Result tests + 4 MviContainer tests)

## Risk Assessment
| Risk | Impact | Mitigation |
|------|--------|------------|
| None | - | Simple foundational classes |

## Security Considerations
- None for this phase (utility classes only)

## Review Report
See: [251212-code-reviewer-to-dev-phase01-review.md](../reports/251212-code-reviewer-to-dev-phase01-review.md)

**Status**: ✅ APPROVED - No critical issues, ready for Phase 02

## Next Steps
→ Phase 02: Domain Layer (depends on Result.kt)
