package com.otistran.flash_trade.domain.usecase

import com.otistran.flash_trade.domain.repository.AuthRepository
import com.otistran.flash_trade.util.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : NoParamsUseCase<Unit> {

    override suspend fun invoke(): Result<Unit> {
        return authRepository.logout()
    }
}
