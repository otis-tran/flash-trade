package com.otistran.flash_trade.data.local.mapper

import com.otistran.flash_trade.data.local.entity.PurchaseEntity
import com.otistran.flash_trade.domain.model.Purchase

fun PurchaseEntity.toDomain(): Purchase = Purchase(
    txHash = txHash,
    tokenAddress = tokenAddress,
    tokenSymbol = tokenSymbol,
    tokenName = tokenName,
    tokenDecimals = tokenDecimals,
    stablecoinAddress = stablecoinAddress,
    stablecoinSymbol = stablecoinSymbol,
    amountIn = amountIn,
    amountOut = amountOut,
    chainId = chainId,
    purchaseTime = purchaseTime,
    autoSellTime = autoSellTime,
    status = status,
    sellTxHash = sellTxHash,
    workerId = workerId,
    walletAddress = walletAddress
)

fun Purchase.toEntity(): PurchaseEntity = PurchaseEntity(
    txHash = txHash,
    tokenAddress = tokenAddress,
    tokenSymbol = tokenSymbol,
    tokenName = tokenName,
    tokenDecimals = tokenDecimals,
    stablecoinAddress = stablecoinAddress,
    stablecoinSymbol = stablecoinSymbol,
    amountIn = amountIn,
    amountOut = amountOut,
    chainId = chainId,
    purchaseTime = purchaseTime,
    autoSellTime = autoSellTime,
    status = status,
    sellTxHash = sellTxHash,
    workerId = workerId,
    walletAddress = walletAddress
)
