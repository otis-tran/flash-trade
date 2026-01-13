package com.otistran.flash_trade.domain.usecase.swap.step

import com.otistran.flash_trade.data.service.PermitSignatureService
import com.otistran.flash_trade.domain.repository.Erc20Repository
import com.otistran.flash_trade.domain.service.TokenCapabilityService
import io.privy.wallet.ethereum.EmbeddedEthereumWallet
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

/** Result of token approval strategy execution. */
sealed class ApprovalResult {
    data object NotRequired : ApprovalResult()
    data class Permit(val calldata: String, val deadline: Long) : ApprovalResult()
    data object TraditionalApprovalSent : ApprovalResult()
    data object AlreadyApproved : ApprovalResult()
    data class Error(val message: String) : ApprovalResult()
}

/**
 * Determines and executes the optimal token approval strategy.
 *
 * Priority order:
 * 1. Skip for native tokens (ETH)
 * 2. Use EIP-2612 permit for supported tokens (gasless)
 * 3. Fall back to traditional approval transaction
 */
class ApprovalStep @Inject constructor(
    private val erc20Repository: Erc20Repository,
    private val tokenCapabilityService: TokenCapabilityService,
    private val permitSignatureService: PermitSignatureService
) {
    companion object {
        private const val PERMIT_VALIDITY_SECONDS = 1800L // 30 minutes
    }

    /**
     * Executes the approval strategy for a token swap.
     *
     * @param tokenAddress Token contract address
     * @param tokenSymbol Token symbol for logging
     * @param userAddress User wallet address
     * @param spenderAddress Router address to approve
     * @param amount Amount to approve
     * @param wallet Privy wallet for signing
     * @param chainId Network chain ID
     * @return Result indicating the approval action taken
     */
    suspend fun execute(
        tokenAddress: String,
        tokenSymbol: String,
        userAddress: String,
        spenderAddress: String,
        amount: BigInteger,
        wallet: EmbeddedEthereumWallet,
        chainId: Long
    ): ApprovalResult {
        if (erc20Repository.isNativeToken(tokenAddress)) {
            Timber.d("Native token, no approval needed")
            return ApprovalResult.NotRequired
        }

        val supportsPermit = tokenCapabilityService.supportsEIP2612(tokenAddress, chainId)
        Timber.d("Token $tokenSymbol supportsPermit: $supportsPermit")

        if (supportsPermit) {
            trySignPermit(tokenAddress, userAddress, spenderAddress, amount, wallet, chainId)
                ?.let { return it }
            Timber.w("Permit failed, falling back to traditional approval")
        }

        return executeTraditionalApproval(tokenAddress, userAddress, spenderAddress, amount, chainId)
    }

    private suspend fun trySignPermit(
        tokenAddress: String,
        userAddress: String,
        spenderAddress: String,
        amount: BigInteger,
        wallet: EmbeddedEthereumWallet,
        chainId: Long
    ): ApprovalResult.Permit? {
        val deadline = System.currentTimeMillis() / 1000 + PERMIT_VALIDITY_SECONDS

        val result = permitSignatureService.signPermit(
            tokenAddress = tokenAddress,
            owner = userAddress,
            spender = spenderAddress,
            value = amount,
            deadline = deadline,
            wallet = wallet,
            chainId = chainId
        )

        return if (result.isSuccess) {
            Timber.i("EIP-2612 permit signed successfully")
            ApprovalResult.Permit(result.getOrNull()!!, deadline)
        } else {
            Timber.w("Permit signing failed: ${result.exceptionOrNull()?.message}")
            null
        }
    }

    private suspend fun executeTraditionalApproval(
        tokenAddress: String,
        userAddress: String,
        spenderAddress: String,
        amount: BigInteger,
        chainId: Long
    ): ApprovalResult {
        val allowanceResult = erc20Repository.getAllowance(
            tokenAddress = tokenAddress,
            owner = userAddress,
            spender = spenderAddress,
            chainId = chainId
        )

        if (allowanceResult is com.otistran.flash_trade.util.Result.Error) {
            Timber.e("Failed to get allowance: ${allowanceResult.message}")
            return ApprovalResult.Error("Failed to check allowance: ${allowanceResult.message}")
        }

        val currentAllowance = (allowanceResult as com.otistran.flash_trade.util.Result.Success).data
        Timber.d("Current allowance: $currentAllowance, required: $amount")

        if (currentAllowance >= amount) {
            Timber.d("Sufficient allowance exists")
            return ApprovalResult.AlreadyApproved
        }

        Timber.i("Requesting approval...")
        val approveResult = erc20Repository.approve(
            tokenAddress = tokenAddress,
            spender = spenderAddress,
            amount = com.otistran.flash_trade.data.repository.Erc20RepositoryImpl.MAX_UINT256,
            chainId = chainId
        )

        if (approveResult is com.otistran.flash_trade.util.Result.Error) {
            Timber.e("Failed to approve: ${approveResult.message}")
            return ApprovalResult.Error("Failed to approve token: ${approveResult.message}")
        }

        Timber.i("Approval transaction sent")
        return ApprovalResult.TraditionalApprovalSent
    }
}

