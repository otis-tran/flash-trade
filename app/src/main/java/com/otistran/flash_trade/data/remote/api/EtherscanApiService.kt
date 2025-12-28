package com.otistran.flash_trade.data.remote.api

import com.otistran.flash_trade.data.remote.dto.etherscan.BalanceResponseDto
import com.otistran.flash_trade.data.remote.dto.etherscan.TokenTxResponseDto
import com.otistran.flash_trade.data.remote.dto.etherscan.TxListResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service for Etherscan V2 API.
 * Single endpoint with chainid parameter for multi-chain support.
 *
 * API Docs: https://docs.etherscan.io/v2-migration
 * Rate Limit: 5 calls/sec (free tier)
 */
interface EtherscanApiService {

    /**
     * Get native ETH balance for address.
     * Returns balance in Wei as string.
     */
    @GET("v2/api")
    suspend fun getBalance(
        @Query("chainid") chainId: Long,
        @Query("module") module: String = "account",
        @Query("action") action: String = "balance",
        @Query("address") address: String,
        @Query("tag") tag: String = "latest",
        @Query("apikey") apiKey: String
    ): BalanceResponseDto

    /**
     * Get ERC-20 token transfer events for address.
     * Used to calculate token holdings.
     */
    @GET("v2/api")
    suspend fun getTokenTx(
        @Query("chainid") chainId: Long,
        @Query("module") module: String = "account",
        @Query("action") action: String = "tokentx",
        @Query("address") address: String,
        @Query("contractaddress") contractAddress: String? = null,
        @Query("page") page: Int = 1,
        @Query("offset") offset: Int = 100,
        @Query("sort") sort: String = "desc",
        @Query("apikey") apiKey: String
    ): TokenTxResponseDto

    /**
     * Get normal transactions for address.
     */
    @GET("v2/api")
    suspend fun getTxList(
        @Query("chainid") chainId: Long,
        @Query("module") module: String = "account",
        @Query("action") action: String = "txlist",
        @Query("address") address: String,
        @Query("startblock") startBlock: Long = 0,
        @Query("endblock") endBlock: Long = 99999999,
        @Query("page") page: Int = 1,
        @Query("offset") offset: Int = 100,
        @Query("sort") sort: String = "desc",
        @Query("apikey") apiKey: String
    ): TxListResponseDto
}
