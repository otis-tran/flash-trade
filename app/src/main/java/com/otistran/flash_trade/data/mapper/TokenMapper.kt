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
            name = name,
            symbol = symbol,
            decimals = decimals,
            logoUrl = logoUrl,
            isVerified = isVerified,
            isWhitelisted = isWhitelisted,
            isStable = isStable,
            isHoneypot = isHoneypot ?: false,
            isFot = isFot ?: false,
            tax = tax ?: 0.0,
            totalTvl = totalTvlAllPools?.toDoubleOrNull() ?: 0.0,
            poolCount = poolCount,
            maxPoolTvl = maxPoolTvl?.toDoubleOrNull(),
            maxPoolVolume = maxPoolVolume?.toDoubleOrNull(),
            avgPoolTvl = avgPoolTvl?.toDoubleOrNull(),
            cgkRank = cgkRank,
            cmcRank = cmcRank,
            websites = websites,
            earliestPoolCreatedAt = earliestPoolCreatedAt
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

    fun List<TokenDto>.toDomainList(): List<Token> = map { it.toDomain() }

    // ==================== DTO → Entity ====================

    fun TokenDto.toEntity(): TokenEntity {
        return TokenEntity(
            address = address,
            name = name,
            symbol = symbol,
            decimals = decimals,
            logoUrl = logoUrl,
            isVerified = isVerified,
            isWhitelisted = isWhitelisted,
            isStable = isStable,
            isHoneypot = isHoneypot ?: false,
            isFot = isFot ?: false,
            tax = tax ?: 0.0,
            totalTvl = totalTvlAllPools?.toDoubleOrNull() ?: 0.0,
            poolCount = poolCount,
            maxPoolTvl = maxPoolTvl?.toDoubleOrNull(),
            maxPoolVolume = maxPoolVolume?.toDoubleOrNull(),
            avgPoolTvl = avgPoolTvl?.toDoubleOrNull(),
            cgkRank = cgkRank,
            cmcRank = cmcRank,
            websites = websites,
            earliestPoolCreatedAt = earliestPoolCreatedAt,
            cachedAt = System.currentTimeMillis()
        )
    }

    fun List<TokenDto>.toEntityList(): List<TokenEntity> = map { it.toEntity() }

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
            isFot = isFot,
            tax = tax,
            totalTvl = totalTvl,
            poolCount = poolCount,
            maxPoolTvl = maxPoolTvl,
            maxPoolVolume = maxPoolVolume,
            avgPoolTvl = avgPoolTvl,
            cgkRank = cgkRank,
            cmcRank = cmcRank,
            websites = websites,
            earliestPoolCreatedAt = earliestPoolCreatedAt
        )
    }

    @JvmName("entityListToDomain")
    fun List<TokenEntity>.toDomainList(): List<Token> = map { it.toDomain() }
}