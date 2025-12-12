package com.otistran.flash_trade.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.otistran.flash_trade.domain.model.Wallet

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey
    val address: String,
    val chainId: Int,
    val balance: Double,
    val createdAt: Long
) {
    fun toDomain(): Wallet = Wallet(
        address = address,
        chainId = chainId,
        balance = balance,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(wallet: Wallet): WalletEntity = WalletEntity(
            address = wallet.address,
            chainId = wallet.chainId,
            balance = wallet.balance,
            createdAt = wallet.createdAt
        )
    }
}
