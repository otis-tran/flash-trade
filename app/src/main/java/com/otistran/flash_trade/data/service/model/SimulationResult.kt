package com.otistran.flash_trade.data.service.model

import java.math.BigInteger

/**
 * Result of transaction simulation via eth_call.
 * Used to detect reverts before sending actual transactions.
 */
data class SimulationResult(
    /** Whether the simulation succeeded (no revert) */
    val success: Boolean,

    /** Human-readable revert reason if simulation failed */
    val revertReason: String? = null,

    /** Raw return data from eth_call (hex string) */
    val returnData: String? = null,

    /** Estimated gas used (if available) */
    val gasUsed: BigInteger? = null
)
