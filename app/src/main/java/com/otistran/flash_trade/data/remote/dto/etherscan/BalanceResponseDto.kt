package com.otistran.flash_trade.data.remote.dto.etherscan

/**
 * Response for Etherscan balance API.
 * Result is Wei balance as string (to avoid precision loss).
 */
typealias BalanceResponseDto = EtherscanBaseResponse<String>
