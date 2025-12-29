package com.otistran.flash_trade

import android.app.Application
import com.otistran.flash_trade.domain.sync.TokenSyncManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FlashTradeApplication : Application() {

    @Inject
    lateinit var tokenSyncManager: TokenSyncManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Start background sync on app launch
        applicationScope.launch {
            //tokenSyncManager.checkAndStartSync()
        }
    }
}