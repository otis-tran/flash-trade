package com.otistran.flash_trade.core.util

import com.otistran.flash_trade.domain.model.NetworkMode

enum class StablecoinType(val symbol: String, val decimals: Int) {
    USDC("USDC", 6),
    USDT("USDT", 6)
}

object StablecoinConstants {
    private const val USDC_ETHEREUM = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48"
    private const val USDT_ETHEREUM = "0xdAC17F958D2ee523a2206206994597C13D831ec7"

    fun getAddress(type: StablecoinType, chain: NetworkMode): String {
        return when (type) {
            StablecoinType.USDC -> USDC_ETHEREUM
            StablecoinType.USDT -> USDT_ETHEREUM
        }
    }
}

/**
 * Common token addresses per network.
 */
object TokenConstants {
    private const val WETH_ETHEREUM = "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2"

    fun getWethAddress(chain: NetworkMode): String = WETH_ETHEREUM
}
