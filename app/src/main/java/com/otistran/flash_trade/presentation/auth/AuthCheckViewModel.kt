package com.otistran.flash_trade.presentation.auth

import androidx.lifecycle.ViewModel
import com.otistran.flash_trade.domain.usecase.CheckLoginStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthCheckViewModel @Inject constructor(
    val checkLoginStatusUseCase: CheckLoginStatusUseCase
) : ViewModel()
