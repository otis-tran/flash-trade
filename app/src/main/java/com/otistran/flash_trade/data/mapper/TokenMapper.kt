package com.otistran.flash_trade.data.mapper

import com.otistran.flash_trade.data.local.entity.TokenEntity
import com.otistran.flash_trade.data.remote.dto.kyber.TokenDto
import com.otistran.flash_trade.data.remote.dto.kyber.TokenListResponse
import com.otistran.flash_trade.domain.model.Token
import com.otistran.flash_trade.domain.model.TokenListResult

/**
 * Maps Token DTOs, Entities, and domain models.
 */
object TokenMapper {

    // ==================== DTO → Domain ====================

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

    // ==================== DTO → Entity ====================

    fun TokenDto.toEntity(): TokenEntity {
        return TokenEntity(
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
            cmcRank = cmcRank,
            cachedAt = System.currentTimeMillis()
        )
    }

    fun List<TokenDto>.toEntityList(): List<TokenEntity> {
        return map { it.toEntity() }
    }

    // ==================== Entity → Domain ====================

    fun TokenEntity.toDomain(): Token {
        return Token(
            address = address,
            name = name,
            symbol = symbol,
            decimals = decimals,
            logoUrl = logoUrl,
            isVerified = isVerified,
            isWhitelisted = isWhitelisted,
            isStable = isStable,
            isHoneypot = isHoneypot,
            totalTvl = totalTvl,
            poolCount = poolCount,
            cgkRank = cgkRank,
            cmcRank = cmcRank
        )
    }

    @JvmName("entityListToDomain")
    fun List<TokenEntity>.toDomainList(): List<Token> {
        return map { it.toDomain() }
    }

    // ==================== Domain → Entity ====================

    fun Token.toEntity(): TokenEntity {
        return TokenEntity(
            address = address,
            name = name,
            symbol = symbol,
            decimals = decimals,
            logoUrl = logoUrl,
            isVerified = isVerified,
            isWhitelisted = isWhitelisted,
            isStable = isStable,
            isHoneypot = isHoneypot,
            totalTvl = totalTvl,
            poolCount = poolCount,
            cgkRank = cgkRank,
            cmcRank = cmcRank,
            cachedAt = System.currentTimeMillis()
        )
    }

    @JvmName("domainListToEntity")
    fun List<Token>.toEntityList(): List<TokenEntity> {
        return map { it.toEntity() }
    }
}