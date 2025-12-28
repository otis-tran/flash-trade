package com.otistran.flash_trade.data.remote.dto.etherscan

import com.squareup.moshi.JsonClass

/**
 * ERC-20 token transfer event from tokentx endpoint.
 * Includes token metadata for display.
 */
@JsonClass(generateAdapter = true)
data class TokenTxDto(
    val hash: String,
    val blockNumber: String,
    val timeStamp: String,
    val from: String,
    val to: String,
    val value: String,
    val tokenName: String,
    val tokenSymbol: String,
    val tokenDecimal: String,
    val contractAddress: String,
    val gas: String,
    val gasPrice: String,
    val gasUsed: String,
    val nonce: String? = null,
    val blockHash: String? = null,
    val transactionIndex: String? = null,
    val input: String? = null,
    val confirmations: String? = null
)

/**
 * Response for tokentx endpoint.
 * Returns list of token transfers.
 */
typealias TokenTxResponseDto = EtherscanBaseResponse<List<TokenTxDto>>
