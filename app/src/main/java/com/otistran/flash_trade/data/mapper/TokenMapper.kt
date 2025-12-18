package com.otistran.flash_trade.data.mapper

import com.otistran.flash_trade.data.remote.dto.kyber.TokenDto
import com.otistran.flash_trade.data.remote.dto.kyber.TokenListResponse
import com.otistran.flash_trade.domain.model.Token
import com.otistran.flash_trade.domain.model.TokenListResult

/**
 * Maps Token DTOs to domain models.
 */
object TokenMapper {

    fun TokenDto.toDomain(): Token {
        return Token(
            address = address,
            name = name?.takeIf { it.isNotBlank() } ?: "Unknown",
            symbol = symbol?.takeIf { it.isNotBlank() } ?: "???",
            decimals = decimals,
            logoUrl = logoUrl,
            isVerified = isVerified,
            isWhitelisted = isWhitelisted,
            isStable = isStable,
            isHoneypot = isHoneypot ?: false,
            totalTvl = totalTvlAllPools?.toDoubleOrNull() ?: 0.0,
            poolCount = poolCount,
            cgkRank = cgkRank,
            cmcRank = cmcRank
        )
    }

    fun TokenListResponse.toDomain(): TokenListResult {
        return TokenListResult(
            tokens = data.map { it.toDomain() },
            page = page,
            totalPages = totalPages,
            total = total,
            hasMore = page < totalPages
        )
    }

    fun List<TokenDto>.toDomainList(): List<Token> {
        return map { it.toDomain() }
    }
}