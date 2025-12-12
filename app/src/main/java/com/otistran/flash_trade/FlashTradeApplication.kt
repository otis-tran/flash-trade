package com.otistran.flash_trade

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FlashTradeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize any app-wide components here
    }
}
