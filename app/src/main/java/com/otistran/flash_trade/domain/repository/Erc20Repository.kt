package com.otistran.flash_trade.domain.repository

import com.otistran.flash_trade.util.Result
import java.math.BigInteger

/**
 * Repository for ERC-20 token operations.
 * Handles allowance checks and approvals for token swaps.
 */
interface Erc20Repository {

    /**
     * Check the allowance amount for a spender.
     *
     * @param tokenAddress ERC-20 contract address (or native token indicator)
     * @param owner Token owner address (user wallet)
     * @param spender Address to check allowance for (e.g., DEX router)
     * @param chainId Chain ID (1 for Ethereum)
     * @return Allowance amount in token wei units
     */
    suspend fun getAllowance(
        tokenAddress: String,
        owner: String,
        spender: String,
        chainId: Long
    ): Result<BigInteger>

    /**
     * Approve a spender to spend tokens.
     *
     * @param tokenAddress ERC-20 contract address (or native token indicator)
     * @param spender Address to approve (e.g., DEX router)
     * @param amount Amount to approve (use MAX_UINT256 for unlimited approval)
     * @param chainId Chain ID
     * @return Transaction hash of approval transaction
     */
    suspend fun approve(
        tokenAddress: String,
        spender: String,
        amount: BigInteger,
        chainId: Long
    ): Result<String>

    /**
     * Check if the given address is native token (ETH).
     *
     * @param address Token address to check
     * @return true if native token, false otherwise
     */
    fun isNativeToken(address: String): Boolean

    companion object {
        /** Maximum uint256 value for unlimited approvals */
        val MAX_UINT256: BigInteger = BigInteger.valueOf(2).pow(256).subtract(BigInteger.ONE)
    }
}
