package com.otistran.flash_trade.data.remote.dto.etherscan

import com.squareup.moshi.JsonClass

/**
 * Normal transaction from txlist endpoint.
 * Does not include token metadata.
 */
@JsonClass(generateAdapter = true)
data class TxDto(
    val hash: String,
    val blockNumber: String,
    val timeStamp: String,
    val from: String,
    val to: String,
    val value: String,
    val gas: String,
    val gasPrice: String,
    val gasUsed: String,
    val isError: String,            // "0" = success, "1" = error
    val nonce: String? = null,
    val blockHash: String? = null,
    val transactionIndex: String? = null,
    val input: String? = null,
    val confirmations: String? = null,
    val methodId: String? = null,
    val functionName: String? = null
)

/**
 * Response for txlist endpoint.
 * Returns list of normal transactions.
 */
typealias TxListResponseDto = EtherscanBaseResponse<List<TxDto>>
