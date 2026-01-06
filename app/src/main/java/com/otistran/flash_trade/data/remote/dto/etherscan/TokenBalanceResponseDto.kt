package com.otistran.flash_trade.data.remote.dto.etherscan

/**
 * Response for Etherscan token balance API.
 * Result is token balance as string (to avoid precision loss).
 */
typealias TokenBalanceResponseDto = EtherscanBaseResponse<String>
