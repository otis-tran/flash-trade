package com.otistran.flash_trade.presentation.feature.swap

import androidx.lifecycle.viewModelScope
import com.otistran.flash_trade.core.base.BaseViewModel
import com.otistran.flash_trade.data.service.PrivyAuthService
import com.otistran.flash_trade.domain.model.Token
import com.otistran.flash_trade.domain.repository.TokenRepository
import com.otistran.flash_trade.domain.usecase.swap.ExecuteSwapUseCase
import com.otistran.flash_trade.domain.usecase.swap.GetSwapQuoteUseCase
import com.otistran.flash_trade.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.BigInteger
import javax.inject.Inject

private const val CHAIN = "base"
private const val DEBOUNCE_MS = 500L
private const val AUTO_REFRESH_MS = 5000L

@HiltViewModel
class SwapViewModel @Inject constructor(
    private val getSwapQuoteUseCase: GetSwapQuoteUseCase,
    private val executeSwapUseCase: ExecuteSwapUseCase,
    private val privyAuthService: PrivyAuthService,
    private val tokenRepository: TokenRepository
) : BaseViewModel<SwapState, SwapEvent, SwapEffect>(
    initialState = SwapState()
) {

    private var quoteRefreshJob: Job? = null
    private var debounceJob: Job? = null

    override fun onEvent(event: SwapEvent) {
        when (event) {
            is SwapEvent.LoadToken -> loadToken(event.address)
            is SwapEvent.SelectTokenFrom -> selectTokenFrom(event.token)
            is SwapEvent.SelectTokenTo -> selectTokenTo(event.token)
            is SwapEvent.SetAmount -> setAmount(event.amount)
            SwapEvent.FetchQuote -> fetchQuote()
            SwapEvent.ExecuteSwap -> executeSwap()
            SwapEvent.SwapTokens -> swapTokens()
            SwapEvent.SetMaxAmount -> setMaxAmount()
            SwapEvent.DismissError -> setState { copy(error = null) }
            SwapEvent.RefreshQuote -> fetchQuote(forceRefresh = true)
        }
    }

    private fun loadToken(address: String) {
        if (address.isBlank()) return
        viewModelScope.launch {
            when (val result = tokenRepository.getTokenByAddress(address)) {
                is Result.Success -> {
                    result.data?.let { token ->
                        setState { copy(tokenFrom = token) }
                    }
                }
                is Result.Error -> {
                    setState { copy(error = "Failed to load token") }
                }
                Result.Loading -> { /* Loading */ }
            }
        }
    }

    private fun selectTokenFrom(token: Token) {
        setState { copy(tokenFrom = token, quote = null) }
        fetchQuoteDebounced()
    }

    private fun selectTokenTo(token: Token) {
        setState { copy(tokenTo = token, quote = null) }
        fetchQuoteDebounced()
    }

    private fun setAmount(amount: String) {
        // Only allow valid numeric input
        if (amount.isNotEmpty() && amount.toDoubleOrNull() == null) return
        setState { copy(amount = amount, quote = null) }
        fetchQuoteDebounced()
    }

    private fun swapTokens() {
        val currentState = state.value
        setState {
            copy(
                tokenFrom = currentState.tokenTo,
                tokenTo = currentState.tokenFrom,
                quote = null
            )
        }
        fetchQuoteDebounced()
    }

    private fun setMaxAmount() {
        val balance = state.value.userBalance
        setState { copy(amount = balance) }
        fetchQuoteDebounced()
    }

    private fun fetchQuoteDebounced() {
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            fetchQuote()
        }
    }

    private fun fetchQuote(forceRefresh: Boolean = false) {
        val currentState = state.value
        if (currentState.tokenFrom == null || currentState.tokenTo == null) return
        if (currentState.amount.isEmpty()) return

        val amountDouble = currentState.amount.toDoubleOrNull()
        if (amountDouble == null || amountDouble <= 0) return

        viewModelScope.launch {
            setState { copy(isLoadingQuote = true, error = null, quoteExpired = false) }

            val amountWei = parseAmountToWei(
                currentState.amount,
                currentState.tokenFrom.decimals
            )

            val user = privyAuthService.getUser()
            val walletAddress = user?.embeddedEthereumWallets?.firstOrNull()?.address

            when (val result = getSwapQuoteUseCase(
                chain = CHAIN,
                tokenIn = currentState.tokenFrom.address,
                tokenOut = currentState.tokenTo.address,
                amountIn = amountWei,
                userAddress = walletAddress
            )) {
                is Result.Success -> {
                    setState {
                        copy(
                            isLoadingQuote = false,
                            quote = result.data,
                            quoteExpired = false
                        )
                    }
                    startAutoRefresh()
                }
                is Result.Error -> {
                    setState {
                        copy(
                            isLoadingQuote = false,
                            error = result.message
                        )
                    }
                }
                Result.Loading -> { /* Already loading */ }
            }
        }
    }

    private fun startAutoRefresh() {
        quoteRefreshJob?.cancel()
        quoteRefreshJob = viewModelScope.launch {
            delay(AUTO_REFRESH_MS)
            setState { copy(quoteExpired = true) }
            fetchQuote(forceRefresh = true)
        }
    }

    private fun executeSwap() {
        val currentState = state.value
        if (!currentState.canSwap) return

        viewModelScope.launch {
            setState { copy(isExecuting = true, error = null) }

            val user = privyAuthService.getUser()
            val walletAddress = user?.embeddedEthereumWallets?.firstOrNull()?.address
            if (walletAddress == null) {
                setState { copy(isExecuting = false, error = "Wallet not found") }
                return@launch
            }

            when (val result = executeSwapUseCase(
                chain = CHAIN,
                quote = currentState.quote!!,
                senderAddress = walletAddress
            )) {
                is Result.Success -> {
                    setState {
                        copy(
                            isExecuting = false,
                            txHash = result.data.txHash,
                            quote = null
                        )
                    }
                    setEffect(SwapEffect.ShowToast("Swap submitted!"))
                    setEffect(SwapEffect.NavigateToTxDetails(result.data.txHash))
                }
                is Result.Error -> {
                    setState { copy(isExecuting = false, error = result.message) }
                }
                Result.Loading -> { /* Already executing */ }
            }
        }
    }

    private fun parseAmountToWei(amount: String, decimals: Int): BigInteger {
        val value = BigDecimal(amount)
        val multiplier = BigDecimal.TEN.pow(decimals)
        return value.multiply(multiplier).toBigInteger()
    }

    override fun onCleared() {
        super.onCleared()
        quoteRefreshJob?.cancel()
        debounceJob?.cancel()
    }
}
