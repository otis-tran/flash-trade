package com.otistran.flash_trade.presentation.feature.swap

import android.util.Log
import timber.log.Timber
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.otistran.flash_trade.core.base.BaseViewModel
import com.otistran.flash_trade.core.event.AppEventBus
import com.otistran.flash_trade.data.service.PrivyAuthService
import com.otistran.flash_trade.core.util.StablecoinConstants
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.model.SwapQuote
import com.otistran.flash_trade.domain.model.Token
import com.otistran.flash_trade.domain.repository.SettingsRepository
import com.otistran.flash_trade.domain.usecase.swap.GetQuoteUseCase
import com.otistran.flash_trade.domain.usecase.swap.GetTokenBalancesUseCase
import com.otistran.flash_trade.domain.usecase.swap.GetTokenPricesUseCase
import com.otistran.flash_trade.domain.usecase.swap.SavePurchaseUseCase
import com.otistran.flash_trade.domain.usecase.swap.ScheduleAutoSellUseCase
import com.otistran.flash_trade.domain.usecase.swap.SearchTokensUseCase
import com.otistran.flash_trade.domain.usecase.swap.SwapTokenUseCase
import com.otistran.flash_trade.presentation.feature.swap.manager.QuoteCountdownManager
import com.otistran.flash_trade.presentation.feature.swap.manager.TokenDataManager
import com.otistran.flash_trade.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

private const val QUOTE_REFRESH_INTERVAL_SECONDS = 15
private const val AMOUNT_INPUT_DEBOUNCE_MS = 500L

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SwapViewModel @Inject constructor(
    private val getTokenBalancesUseCase: GetTokenBalancesUseCase,
    private val getTokenPricesUseCase: GetTokenPricesUseCase,
    private val getQuoteUseCase: GetQuoteUseCase,
    private val searchTokensUseCase: SearchTokensUseCase,
    private val swapTokenUseCase: SwapTokenUseCase,
    private val savePurchaseUseCase: SavePurchaseUseCase,
    private val scheduleAutoSellUseCase: ScheduleAutoSellUseCase,
    private val settingsRepository: SettingsRepository,
    private val privyAuthService: PrivyAuthService,
    private val appEventBus: AppEventBus
) : BaseViewModel<SwapState, SwapEvent, SwapEffect>(
    initialState = SwapState()
) {
    private var currentNetwork: NetworkMode = NetworkMode.DEFAULT
    private var quoteJob: Job? = null
    private val searchQueryFlow = MutableStateFlow("")

    // Managers
    private val countdownManager = QuoteCountdownManager(viewModelScope)
    private val tokenDataManager = TokenDataManager(
        getTokenBalancesUseCase,
        getTokenPricesUseCase,
        viewModelScope
    )

    val tokensFlow: Flow<PagingData<Token>> = combine(
        searchQueryFlow,
        state.map { it.showSafeTokensOnly }.distinctUntilChanged()
    ) { query, safeOnly -> 
        Timber.d("tokensFlow: combine triggered - query='$query', safeOnly=$safeOnly")
        query to safeOnly 
    }
        .debounce(300)
        .flatMapLatest { (query, safeOnly) ->
            Timber.d("tokensFlow: flatMapLatest - query='$query', safeOnly=$safeOnly, network=$currentNetwork")
            searchTokensUseCase(query, safeOnly, currentNetwork)
        }
        .cachedIn(viewModelScope)

    init {
        observeNetworkMode(settingsRepository) { network ->
            currentNetwork = network
            setState { copy(network = network) }
        }
        loadSlippagePreference()
        observeCountdown()
    }

    private fun observeCountdown() {
        viewModelScope.launch {
            countdownManager.secondsRemaining.collect { seconds ->
                setState { copy(quoteExpiresInSeconds = seconds) }
            }
        }
    }

    // ==================== Event Handler ====================

    override fun onEvent(event: SwapEvent) = when (event) {
        // Token Selection
        SwapEvent.OpenSellTokenSelector -> openTokenSelector(isSellToken = true)
        SwapEvent.OpenBuyTokenSelector -> openTokenSelector(isSellToken = false)
        SwapEvent.CloseTokenSelector -> closeTokenSelector()
        is SwapEvent.SelectSellToken -> selectToken(event.token, isSellToken = true)
        is SwapEvent.SelectBuyToken -> selectToken(event.token, isSellToken = false)
        is SwapEvent.SearchTokens -> searchTokens(event.query)
        SwapEvent.SwapTokens -> swapTokenDirection()
        SwapEvent.ToggleTokenFilter -> toggleTokenFilter()

        // Amount & Quote
        is SwapEvent.SetSellAmount -> setSellAmount(event.amount)
        SwapEvent.RefreshBalances -> refreshBalances()
        SwapEvent.RefreshQuote -> refreshQuote()

        // Slippage
        SwapEvent.OpenSlippageDialog -> setState { copy(showSlippageDialog = true) }
        SwapEvent.CloseSlippageDialog -> setState { copy(showSlippageDialog = false) }
        is SwapEvent.SetSlippage -> setSlippage(event.slippage)

        // Execution
        SwapEvent.ExecuteSwap -> executeSwap()

        // Navigation - all back actions go to Home for consistency
        SwapEvent.NavigateBack -> setEffect(SwapEffect.NavigateToHome)
        SwapEvent.Cancel -> setEffect(SwapEffect.NavigateToHome)

        // Error
        SwapEvent.DismissError -> setState { copy(error = null) }
    }

    // ==================== Token Selection ====================

    private fun openTokenSelector(isSellToken: Boolean) {
        Timber.d("openTokenSelector: isSellToken=$isSellToken, currentNetwork=$currentNetwork")
        searchQueryFlow.value = ""
        setState {
            copy(
                showTokenSelector = true,
                isSelectingSellToken = isSellToken,
                tokenSearchQuery = ""
            )
        }
    }

    private fun closeTokenSelector() {
        setState { copy(showTokenSelector = false, tokenSearchQuery = "") }
    }

    private fun selectToken(token: SwapToken, isSellToken: Boolean) {
        Timber.d("selectToken: ${if (isSellToken) "SELL" else "BUY"} token=${token.symbol} address=${token.address} decimals=${token.decimals}")
        countdownManager.stop()
        setState {
            if (isSellToken) {
                copy(
                    sellToken = token,
                    showTokenSelector = false,
                    tokenSearchQuery = "",
                    sellAmount = "",
                    buyAmount = "",
                    quote = null
                )
            } else {
                copy(
                    buyToken = token,
                    showTokenSelector = false,
                    tokenSearchQuery = "",
                    quote = null
                )
            }
        }
        refreshTokenData()
        if (!isSellToken && currentState.hasValidSellAmount) {
            refreshQuote()
        }
    }

    private fun searchTokens(query: String) {
        setState { copy(tokenSearchQuery = query) }
        searchQueryFlow.value = query
    }

    private fun swapTokenDirection() {
        countdownManager.stop()
        val (currentSell, currentBuy) = currentState.sellToken to currentState.buyToken
        setState {
            copy(
                sellToken = currentBuy,
                buyToken = currentSell,
                sellAmount = "",
                buyAmount = "",
                quote = null
            )
        }
        refreshTokenData()
    }

    private fun toggleTokenFilter() {
        setState { copy(showSafeTokensOnly = !showSafeTokensOnly) }
    }

    // ==================== Amount & Quote ====================

    private fun setSellAmount(amount: String) {
        if (!isValidAmountInput(amount)) return

        setState { copy(sellAmount = amount) }
        quoteJob?.cancel()
        countdownManager.stop()

        quoteJob = viewModelScope.launch {
            delay(AMOUNT_INPUT_DEBOUNCE_MS)
            if (amount.isNotEmpty() && amount.toBigDecimalOrNull() != null) {
                refreshQuote()
            } else {
                setState { copy(buyAmount = "", quote = null) }
            }
        }
    }

    private fun isValidAmountInput(amount: String): Boolean {
        if (amount.isEmpty()) return true
        val decimal = amount.toBigDecimalOrNull()
        return decimal != null || amount == "."
    }

    private fun refreshQuote() {
        val sellToken = currentState.sellToken ?: return
        val buyToken = currentState.buyToken ?: return
        val sellAmount = currentState.sellAmount.toBigDecimalOrNull() ?: return

        viewModelScope.launch {
            setState { copy(isLoadingQuote = true) }

            when (val result = getQuoteUseCase(sellToken, buyToken, sellAmount, currentNetwork)) {
                is Result.Success -> {
                    applyQuote(result.data)
                    countdownManager.start(QUOTE_REFRESH_INTERVAL_SECONDS) {
                        setState { copy(isQuoteStale = true) }
                        refreshQuote()
                    }
                }
                is Result.Error -> {
                    Timber.w("Quote error: ${result.message}")
                    setState { copy(isLoadingQuote = false, buyAmount = "", quote = null) }
                }
                Result.Loading -> {}
            }
        }
    }

    private fun applyQuote(quote: SwapQuote) {
        setState {
            copy(
                isLoadingQuote = false,
                quote = quote,
                quoteTimestamp = System.currentTimeMillis(),
                buyAmount = quote.formattedAmountOut,
                sellAmountUsd = "$${quote.amountInUsd.setScale(2, RoundingMode.HALF_UP)}",
                buyAmountUsd = "$${quote.amountOutUsd.setScale(2, RoundingMode.HALF_UP)}",
                quoteExpiresInSeconds = QUOTE_REFRESH_INTERVAL_SECONDS,
                isQuoteStale = false
            )
        }
    }

    // ==================== Token Data (Balance & Prices) ====================

    private fun refreshTokenData() {
        Timber.d("refreshTokenData() called")
        setState { copy(isPricesLoading = true) }
        tokenDataManager.fetchTokenData(
            sellToken = currentState.sellToken,
            buyToken = currentState.buyToken,
            network = currentNetwork
        ) { result ->
            Timber.d("refreshTokenData() called with: result = $result")
            setState {
                copy(
                    isPricesLoading = false,
                    sellToken = sellToken?.copy(
                        balance = result.sellBalance ?: sellToken.balance,
                        priceUsd = result.sellPrice ?: sellToken.priceUsd
                    ),
                    buyToken = buyToken?.copy(
                        balance = result.buyBalance ?: buyToken.balance,
                        priceUsd = result.buyPrice ?: buyToken.priceUsd
                    ),
                    sellTokenPrice = result.sellPrice ?: sellTokenPrice,
                    buyTokenPrice = result.buyPrice ?: buyTokenPrice
                )
            }
        }
    }

    private fun refreshBalances() {
        tokenDataManager.fetchBalancesOnly(
            sellToken = currentState.sellToken,
            buyToken = currentState.buyToken,
            network = currentNetwork
        ) { sellBalance, buyBalance ->
            setState {
                copy(
                    sellToken = sellToken?.copy(balance = sellBalance ?: sellToken.balance),
                    buyToken = buyToken?.copy(balance = buyBalance ?: buyToken.balance)
                )
            }
        }
    }

    // ==================== Slippage ====================

    private fun setSlippage(slippage: Double) {
        setState { copy(slippage = slippage, showSlippageDialog = false) }
        viewModelScope.launch {
            settingsRepository.setDefaultSlippage(slippage)
        }
    }

    private fun loadSlippagePreference() {
        viewModelScope.launch {
            val savedSlippage = settingsRepository.getDefaultSlippage()
            setState { copy(slippage = savedSlippage) }
        }
    }

    // ==================== Swap Execution ====================

    private fun executeSwap() {
        if (!currentState.canSwap) return

        Timber.d("Starting swap execution")

        viewModelScope.launch {
            setState { copy(isExecuting = true, error = null) }

            // Check user authentication first
            val user = privyAuthService.getUser()
            if (user == null) {
                Timber.e("executeSwap: User not authenticated")
                setState { copy(isExecuting = false, error = "Please sign in to continue") }
                return@launch
            }

            // Then check for wallet
            val wallet = user.embeddedEthereumWallets.firstOrNull()
            if (wallet == null) {
                Timber.e("executeSwap: No wallet found for user ${user.id}")
                setState { copy(isExecuting = false, error = "Wallet not ready. Please wait and retry.") }
                return@launch
            }
            Timber.d("executeSwap: Using wallet ${wallet.address}")

            val sellToken = currentState.sellToken!!
            val buyToken = currentState.buyToken!!
            val amountInWei = currentState.sellAmount.toBigDecimalOrNull()
                ?.multiply(BigDecimal.TEN.pow(sellToken.decimals))
                ?.toBigInteger() ?: return@launch

            // Convert slippage percentage to basis points (0.5% → 50 bps)
            val slippageBps = (currentState.slippage * 100).toInt()

            // Use NonCancellable to ensure swap completes even if user leaves screen
            // This is critical for auto-sell scheduling to work properly
            val result = withContext(NonCancellable) {
                swapTokenUseCase(
                    tokenIn = sellToken,
                    tokenOut = buyToken,
                    amountIn = amountInWei,
                    userAddress = wallet.address,
                    chainId = currentNetwork.chainId,
                    chainName = currentNetwork.chainName,
                    wallet = wallet,
                    slippageBps = slippageBps
                )
            }

            when (result) {
                is Result.Success -> {
                    val txHash = result.data
                    
                    // Stop countdown and clear quote to prevent continuous polling
                    countdownManager.stop()
                    quoteJob?.cancel()
                    setState { copy(isExecuting = false, quote = null, sellAmount = "", buyAmount = "") }
                    
                    // Schedule auto-sell if enabled and not a stablecoin
                    val isStablecoin = StablecoinConstants.isStablecoin(buyToken.address)
                    val isAutoSellEnabled = settingsRepository.isAutoSellEnabled()
                    
                    if (!isStablecoin && isAutoSellEnabled) {
                        try {
                            // Calculate amountOut in raw (wei) from quote
                            val amountOutRaw = currentState.quote?.amountOut
                                ?.multiply(BigDecimal.TEN.pow(buyToken.decimals))
                                ?.toBigInteger() ?: java.math.BigInteger.ZERO
                            
                            // Save purchase record
                            savePurchaseUseCase(
                                txHash = txHash,
                                tokenAddress = buyToken.address,
                                tokenSymbol = buyToken.symbol,
                                tokenName = buyToken.name,
                                tokenDecimals = buyToken.decimals,
                                stablecoinAddress = sellToken.address,
                                stablecoinSymbol = sellToken.symbol,
                                amountIn = amountInWei,
                                amountOut = amountOutRaw,
                                chainId = currentNetwork.chainId,
                                walletAddress = wallet.address
                            )
                            
                            // Schedule auto-sell worker
                            scheduleAutoSellUseCase(txHash)
                            
                            val duration = settingsRepository.getAutoSellDurationMinutes()
                            appEventBus.showToast("Swap successful! Auto-sell in ${duration}m")
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to schedule auto-sell")
                            appEventBus.showToast("Swap successful!")
                        }
                    } else {
                        appEventBus.showToast("Swap successful!")
                    }
                    
                    // Trigger portfolio refresh (user can navigate manually)
                    appEventBus.triggerRefreshPortfolio()
                    
                    // Navigate back to home after successful swap
                    setEffect(SwapEffect.NavigateToHome)
                    
                    // Delay 500ms then refresh balances to show updated portfolio
                    delay(500)
                    appEventBus.triggerRefreshPortfolio()
                }
                is Result.Error -> {
                    Timber.e("Swap failed: ${result.message}", result.cause)
                    setState { copy(isExecuting = false, error = result.message) }
                }
                Result.Loading -> {}
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownManager.cancel()
        quoteJob?.cancel()
    }
}
