package com.otistran.flash_trade.domain.service.model

import java.math.BigInteger

/**
 * Result of swap pre-validation check.
 * Contains balance and approval state before executing swap.
 */
data class SwapPreValidationResult(
    /** Whether user has enough balance for the swap */
    val hasEnoughBalance: Boolean,

    /** Current balance of the input token */
    val currentBalance: BigInteger,

    /** Amount required for the swap */
    val requiredAmount: BigInteger,

    /** Whether the token supports EIP-2612 permit */
    val supportsPermit: Boolean,

    /** Current allowance for the router */
    val currentAllowance: BigInteger,

    /** Whether approval transaction is needed */
    val needsApproval: Boolean
)
