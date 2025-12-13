package com.otistran.flash_trade.presentation.navigation

/**
 * Navigation routes for the app.
 */
sealed class Screen(val route: String) {
    // Onboarding flow
    data object Welcome : Screen("welcome")
    data object Login : Screen("login")

    // Main flow
    data object Trading : Screen("trading")
    data object Portfolio : Screen("portfolio")
    data object Settings : Screen("settings")

    // Detail screens
    data class TradeDetails(val tradeId: String = "{tradeId}") : Screen("trade/$tradeId") {
        companion object {
            const val TRADE_ID_ARG = "tradeId"
            fun createRoute(tradeId: String) = "trade/$tradeId"
        }
    }

    companion object {
        // Start destination
        val START = Login
    }
}
