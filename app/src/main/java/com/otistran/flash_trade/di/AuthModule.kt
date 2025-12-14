package com.otistran.flash_trade.di

import com.otistran.flash_trade.data.local.datastore.UserPreferences
import com.otistran.flash_trade.data.repository.AuthRepositoryImpl
import com.otistran.flash_trade.data.service.PrivyAuthService
import com.otistran.flash_trade.domain.repository.AuthRepository
import com.otistran.flash_trade.domain.usecase.CheckLoginStatusUseCase
import com.otistran.flash_trade.domain.usecase.LogoutUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for authentication dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        privyAuthService: PrivyAuthService,
        userPreferences: UserPreferences
    ): AuthRepository = AuthRepositoryImpl(privyAuthService, userPreferences)

    @Provides
    @Singleton
    fun provideCheckLoginStatusUseCase(
        authRepository: AuthRepository
    ): CheckLoginStatusUseCase = CheckLoginStatusUseCase(authRepository)

    @Provides
    @Singleton
    fun provideLogoutUseCase(
        authRepository: AuthRepository
    ): LogoutUseCase = LogoutUseCase(authRepository)
}
