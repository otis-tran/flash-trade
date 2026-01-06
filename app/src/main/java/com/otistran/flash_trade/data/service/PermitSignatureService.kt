package com.otistran.flash_trade.data.service

import com.otistran.flash_trade.domain.service.TokenCapabilityService
import io.privy.wallet.ethereum.EmbeddedEthereumWallet
import io.privy.wallet.ethereum.EthereumRpcRequest
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service to sign EIP-2612 permit messages for gasless token approvals.
 * Creates EIP-712 typed data and signs using Privy wallet.
 */
@Singleton
class PermitSignatureService @Inject constructor(
    private val tokenCapabilityService: TokenCapabilityService
) {
    /**
     * Sign an EIP-2612 permit for gasless token approval.
     *
     * @param tokenAddress Token contract address
     * @param owner User wallet address (permit signer)
     * @param spender Router address (permit beneficiary)
     * @param value Amount to approve
     * @param deadline Unix timestamp for permit expiration
     * @param wallet Privy wallet for signing
     * @param chainId Chain ID
     * @return Encoded permit calldata for KyberSwap API, or error
     */
    suspend fun signPermit(
        tokenAddress: String,
        owner: String,
        spender: String,
        value: BigInteger,
        deadline: Long,
        wallet: EmbeddedEthereumWallet,
        chainId: Long
    ): Result<String> {
        return try {
            Timber.d("Signing permit for token $tokenAddress, spender $spender")

            // Get token name for EIP-712 domain
            val tokenName = tokenCapabilityService.getTokenName(tokenAddress, chainId)
                ?: return Result.failure(Exception("Failed to get token name"))

            // Get permit nonce for owner
            val nonce = tokenCapabilityService.getPermitNonce(tokenAddress, owner, chainId)
                ?: return Result.failure(Exception("Failed to get permit nonce"))

            Timber.d("Token: $tokenName, nonce: $nonce")

            // Build EIP-712 typed data
            val typedData = buildTypedData(
                tokenName = tokenName,
                tokenAddress = tokenAddress,
                chainId = chainId,
                owner = owner,
                spender = spender,
                value = value,
                nonce = nonce,
                deadline = deadline
            )

            Timber.d("Typed data: $typedData")

            // Sign using Privy wallet (eth_signTypedData_v4)
            val request = EthereumRpcRequest.ethSignTypedDataV4(owner, typedData)
            val signResult = wallet.provider.request(request)

            val signature = signResult.getOrNull()?.data as? String
                ?: return Result.failure(
                    signResult.exceptionOrNull() ?: Exception("Permit signing failed")
                )

            Timber.d("Permit signed: $signature")

            // Parse r, s, v from signature
            val sigBytes = signature.removePrefix("0x")
            if (sigBytes.length != 130) {
                return Result.failure(Exception("Invalid signature length: ${sigBytes.length}"))
            }

            val r = sigBytes.substring(0, 64)
            val s = sigBytes.substring(64, 128)
            val v = sigBytes.substring(128, 130).toInt(16)

            // Encode permit calldata for KyberSwap
            val permitCalldata = encodePermitCalldata(
                owner = owner,
                spender = spender,
                value = value,
                deadline = deadline,
                v = v,
                r = r,
                s = s
            )

            Timber.i("Permit calldata encoded successfully")
            Result.success(permitCalldata)

        } catch (e: Exception) {
            Timber.e("Error signing permit", e)
            Result.failure(e)
        }
    }

    /**
     * Build EIP-712 typed data JSON for permit.
     */
    private fun buildTypedData(
        tokenName: String,
        tokenAddress: String,
        chainId: Long,
        owner: String,
        spender: String,
        value: BigInteger,
        nonce: BigInteger,
        deadline: Long
    ): String {
        // Note: Some tokens use version "2" (like DAI on mainnet)
        // We default to "1" which works for most tokens (USDC, etc.)
        return """
        {
            "types": {
                "EIP712Domain": [
                    {"name": "name", "type": "string"},
                    {"name": "version", "type": "string"},
                    {"name": "chainId", "type": "uint256"},
                    {"name": "verifyingContract", "type": "address"}
                ],
                "Permit": [
                    {"name": "owner", "type": "address"},
                    {"name": "spender", "type": "address"},
                    {"name": "value", "type": "uint256"},
                    {"name": "nonce", "type": "uint256"},
                    {"name": "deadline", "type": "uint256"}
                ]
            },
            "primaryType": "Permit",
            "domain": {
                "name": "$tokenName",
                "version": "1",
                "chainId": $chainId,
                "verifyingContract": "$tokenAddress"
            },
            "message": {
                "owner": "$owner",
                "spender": "$spender",
                "value": "$value",
                "nonce": "$nonce",
                "deadline": "$deadline"
            }
        }
        """.trimIndent()
    }

    /**
     * Encode permit parameters for KyberSwap API.
     * Format: owner + spender + value + deadline + v + r + s (all padded to 32 bytes)
     */
    private fun encodePermitCalldata(
        owner: String,
        spender: String,
        value: BigInteger,
        deadline: Long,
        v: Int,
        r: String,
        s: String
    ): String {
        val ownerPadded = owner.removePrefix("0x").lowercase().padStart(64, '0')
        val spenderPadded = spender.removePrefix("0x").lowercase().padStart(64, '0')
        val valuePadded = value.toString(16).padStart(64, '0')
        val deadlinePadded = deadline.toString(16).padStart(64, '0')
        val vPadded = v.toString(16).padStart(64, '0')
        val rPadded = r.lowercase().padStart(64, '0')
        val sPadded = s.lowercase().padStart(64, '0')

        return "$ownerPadded$spenderPadded$valuePadded$deadlinePadded$vPadded$rPadded$sPadded"
    }
}
