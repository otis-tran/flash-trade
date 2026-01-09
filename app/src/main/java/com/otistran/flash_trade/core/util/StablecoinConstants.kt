package com.otistran.flash_trade.core.util

import com.otistran.flash_trade.domain.model.NetworkMode

enum class StablecoinType(val symbol: String, val decimals: Int) {
    USDC("USDC", 6),
    USDT("USDT", 6)
}

object StablecoinConstants {
    private const val USDC_ETHEREUM = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48"
    private const val USDT_ETHEREUM = "0xdAC17F958D2ee523a2206206994597C13D831ec7"

    private val ALL_STABLECOINS = setOf(
        USDC_ETHEREUM.lowercase(),
        USDT_ETHEREUM.lowercase()
    )

    fun getAddress(type: StablecoinType, chain: NetworkMode): String {
        return when (type) {
            StablecoinType.USDC -> USDC_ETHEREUM
            StablecoinType.USDT -> USDT_ETHEREUM
        }
    }

    /**
     * Check if a token address is a stablecoin.
     * Used to skip auto-sell for stablecoins.
     */
    fun isStablecoin(address: String): Boolean {
        return address.lowercase() in ALL_STABLECOINS
    }

    /**
     * Get preferred stablecoin address for auto-sell target.
     * Priority: USDT > USDC
     */
    fun getPreferredStablecoin(chain: NetworkMode): String {
        return USDT_ETHEREUM
    }
}

/**
 * Common token addresses per network.
 */
object TokenConstants {
    private const val WETH_ETHEREUM = "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2"

    /**
     * ETH native token addresses used by various DEX APIs.
     * - Kyber/1inch: 0xeeee...
     * - Some APIs: 0x0000...
     */
    private val ETH_NATIVE_ADDRESSES = setOf(
        "0x0000000000000000000000000000000000000000",
        "0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"
    )

    /**
     * Maximum percentage of native token balance that can be swapped.
     * Reserve 20% for gas fees.
     */
    const val NATIVE_TOKEN_MAX_SWAP_PERCENTAGE = 0.8

    fun getWethAddress(chain: NetworkMode): String = WETH_ETHEREUM

    /**
     * Check if a token address represents a native token (ETH/BNB/MATIC/etc).
     * Uses case-insensitive comparison.
     */
    fun isNativeToken(address: String): Boolean =
        address.lowercase() in ETH_NATIVE_ADDRESSES
}
