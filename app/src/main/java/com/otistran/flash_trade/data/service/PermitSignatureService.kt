package com.otistran.flash_trade.data.service

import com.otistran.flash_trade.domain.service.TokenCapabilityService
import io.privy.wallet.ethereum.EmbeddedEthereumWallet
import io.privy.wallet.ethereum.EthereumRpcRequest
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Signs EIP-2612 permit messages for gasless token approvals.
 *
 * Enables single-transaction swaps by bundling approval signature with swap calldata,
 * eliminating the need for a separate approval transaction.
 *
 * @see <a href="https://eips.ethereum.org/EIPS/eip-2612">EIP-2612</a>
 */
@Singleton
class PermitSignatureService @Inject constructor(
    private val tokenCapabilityService: TokenCapabilityService
) {
    /**
     * Signs an EIP-2612 permit for gasless token approval.
     *
     * @param tokenAddress Token contract address
     * @param owner Wallet address signing the permit
     * @param spender Router address authorized to spend tokens
     * @param value Amount to approve
     * @param deadline Unix timestamp when permit expires
     * @param wallet Privy wallet for signing
     * @param chainId Network chain ID
     * @return ABI-encoded permit calldata for KyberSwap API
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

            val tokenName = tokenCapabilityService.getTokenName(tokenAddress, chainId)
                ?: return Result.failure(Exception("Failed to get token name"))

            val nonce = tokenCapabilityService.getPermitNonce(tokenAddress, owner, chainId)
                ?: return Result.failure(Exception("Failed to get permit nonce"))

            Timber.d("Token: $tokenName, nonce: $nonce")

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

            val request = EthereumRpcRequest.ethSignTypedDataV4(owner, typedData)
            val signResult = wallet.provider.request(request)

            val signature = signResult.getOrNull()?.data as? String
                ?: return Result.failure(
                    signResult.exceptionOrNull() ?: Exception("Permit signing failed")
                )

            Timber.d("Permit signed: $signature")

            val sigBytes = signature.removePrefix("0x")
            if (sigBytes.length != 130) {
                return Result.failure(Exception("Invalid signature length: ${sigBytes.length}"))
            }

            val r = sigBytes.substring(0, 64)
            val s = sigBytes.substring(64, 128)
            val v = sigBytes.substring(128, 130).toInt(16)

            val permitCalldata = encodePermitCalldata(owner, spender, value, deadline, v, r, s)

            Timber.i("Permit calldata encoded successfully")
            Result.success(permitCalldata)

        } catch (e: Exception) {
            Timber.e("Error signing permit", e)
            Result.failure(e)
        }
    }

    /**
     * Builds EIP-712 typed data JSON for permit signing.
     *
     * Note: Privy SDK requires snake_case keys (primary_type, chain_id, verifying_contract).
     * Default version "1" works for most tokens; DAI uses version "2".
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
    ): String = """
        {
            "types": {
                "EIP712Domain": [
                    {"name": "name", "type": "string"},
                    {"name": "version", "type": "string"},
                    {"name": "chain_id", "type": "uint256"},
                    {"name": "verifying_contract", "type": "address"}
                ],
                "Permit": [
                    {"name": "owner", "type": "address"},
                    {"name": "spender", "type": "address"},
                    {"name": "value", "type": "uint256"},
                    {"name": "nonce", "type": "uint256"},
                    {"name": "deadline", "type": "uint256"}
                ]
            },
            "primary_type": "Permit",
            "domain": {
                "name": "$tokenName",
                "version": "1",
                "chain_id": $chainId,
                "verifying_contract": "$tokenAddress"
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

    /**
     * ABI-encodes permit parameters for KyberSwap API (without function selector).
     *
     * Format: encode([address, address, uint256, uint256, uint8, bytes32, bytes32],
     *                [owner, spender, value, deadline, v, r, s])
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

        return "0x$ownerPadded$spenderPadded$valuePadded$deadlinePadded$vPadded$rPadded$sPadded"
    }
}

