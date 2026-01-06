package com.otistran.flash_trade.data.repository

import com.otistran.flash_trade.data.service.TransactionService
import com.otistran.flash_trade.domain.repository.Erc20Repository
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton
import com.otistran.flash_trade.util.Result as AppResult

/**
 * Repository for ERC-20 token operations.
 * Handles allowance checks and approvals for token swaps.
 */
@Singleton
class Erc20RepositoryImpl @Inject constructor(
    private val transactionService: TransactionService
) : Erc20Repository {

    override suspend fun getAllowance(
        tokenAddress: String,
        owner: String,
        spender: String,
        chainId: Long
    ): AppResult<BigInteger> {
        // Native tokens don't need approval
        if (transactionService.isNativeToken(tokenAddress)) {
            Timber.d("Native token detected, approval not needed")
            return AppResult.Success(MAX_UINT256)
        }

        return transactionService.getAllowance(
            tokenAddress = tokenAddress,
            owner = owner,
            spender = spender,
            chainId = chainId
        ).fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { e -> AppResult.Error(e.message ?: "Failed to get allowance", e) }
        )
    }

    override suspend fun approve(
        tokenAddress: String,
        spender: String,
        amount: BigInteger,
        chainId: Long
    ): AppResult<String> {
        // Native tokens don't need approval
        if (transactionService.isNativeToken(tokenAddress)) {
            Timber.d("Native token detected, approval not needed")
            return AppResult.Success("NATIVE_TOKEN_NO_APPROVAL_NEEDED")
        }

        Timber.i("Approving token: $tokenAddress for spender: $spender, amount: $amount")

        return transactionService.sendApproval(
            tokenAddress = tokenAddress,
            spenderAddress = spender,
            amount = amount,
            chainId = chainId
        ).fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { e -> AppResult.Error(e.message ?: "Failed to approve", e) }
        )
    }

    override fun isNativeToken(address: String): Boolean {
        return transactionService.isNativeToken(address)
    }

    companion object {
        val MAX_UINT256: BigInteger = BigInteger.valueOf(2).pow(256).subtract(BigInteger.ONE)
    }
}
