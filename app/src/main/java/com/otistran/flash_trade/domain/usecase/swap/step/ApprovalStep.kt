package com.otistran.flash_trade.domain.usecase.swap.step

import com.otistran.flash_trade.data.service.PermitSignatureService
import com.otistran.flash_trade.domain.repository.Erc20Repository
import com.otistran.flash_trade.domain.service.TokenCapabilityService
import io.privy.wallet.ethereum.EmbeddedEthereumWallet
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

/**
 * Result of approval determination.
 */
sealed class ApprovalResult {
    /** No approval needed (native token) */
    data object NotRequired : ApprovalResult()

    /** EIP-2612 permit available */
    data class Permit(val calldata: String, val deadline: Long) : ApprovalResult()

    /** Traditional approval required, and it was sent */
    data object TraditionalApprovalSent : ApprovalResult()

    /** Already has sufficient allowance */
    data object AlreadyApproved : ApprovalResult()

    /** Error occurred */
    data class Error(val message: String) : ApprovalResult()
}

/**
 * Step 2: Token approval strategy
 * - Skip for native tokens
 * - Try EIP-2612 permit for supported tokens
 * - Fall back to traditional approval
 */
class ApprovalStep @Inject constructor(
    private val erc20Repository: Erc20Repository,
    private val tokenCapabilityService: TokenCapabilityService,
    private val permitSignatureService: PermitSignatureService
) {
    /**
     * Determine and execute approval strategy.
     *
     * @param tokenAddress Token to approve
     * @param tokenSymbol Token symbol (for logging)
     * @param userAddress User wallet address
     * @param spenderAddress Router address
     * @param amount Amount to approve
     * @param wallet Privy wallet for signing
     * @param chainId Chain ID
     * @return ApprovalResult indicating what action was taken
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
        // Check if native token
        if (erc20Repository.isNativeToken(tokenAddress)) {
            Timber.d("Native token, no approval needed")
            return ApprovalResult.NotRequired
        }

        // Try EIP-2612 permit first
        val supportsPermit = tokenCapabilityService.supportsEIP2612(tokenAddress, chainId)
        Timber.d("Token $tokenSymbol supportsPermit: $supportsPermit")

        if (supportsPermit) {
            val permit = trySignPermit(
                tokenAddress = tokenAddress,
                userAddress = userAddress,
                spenderAddress = spenderAddress,
                amount = amount,
                wallet = wallet,
                chainId = chainId
            )
            if (permit != null) {
                return permit
            }
            Timber.w("Permit failed, falling back to traditional approval")
        }

        // Traditional approval flow
        return executeTraditionalApproval(
            tokenAddress = tokenAddress,
            userAddress = userAddress,
            spenderAddress = spenderAddress,
            amount = amount,
            chainId = chainId
        )
    }

    private suspend fun trySignPermit(
        tokenAddress: String,
        userAddress: String,
        spenderAddress: String,
        amount: BigInteger,
        wallet: EmbeddedEthereumWallet,
        chainId: Long
    ): ApprovalResult.Permit? {
        val deadline = System.currentTimeMillis() / 1000 + 1800 // 30 min

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
            val calldata = result.getOrNull()!!
            Timber.i("EIP-2612 permit signed successfully")
            ApprovalResult.Permit(calldata, deadline)
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
        // Check current allowance
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

        // Send approval
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
