package com.otistran.flash_trade.domain.usecase.swap.step

import com.otistran.flash_trade.data.service.TransactionSimulator
import com.otistran.flash_trade.domain.model.EncodedRouteResponse
import com.otistran.flash_trade.util.Result
import timber.log.Timber
import javax.inject.Inject

/**
 * Step 5: Transaction simulation
 * Uses eth_call to verify transaction will succeed before execution.
 */
class SimulationStep @Inject constructor(
    private val transactionSimulator: TransactionSimulator
) {
    /**
     * Simulate transaction to catch reverts before execution.
     *
     * @param userAddress User wallet address
     * @param encodedRoute Encoded route from build step
     * @param chainId Chain ID
     * @return Success or error with revert reason
     */
    suspend fun execute(
        userAddress: String,
        encodedRoute: EncodedRouteResponse,
        chainId: Long
    ): Result<Unit> {
        Timber.d("Simulating transaction")

        val routeData = encodedRoute.data
        if (routeData?.routerAddress == null || routeData.data == null) {
            Timber.e("Missing router address or TX data")
            return Result.Error("Invalid route data")
        }

        val simulationResult = transactionSimulator.simulate(
            from = userAddress,
            to = routeData.routerAddress,
            data = routeData.data,
            value = routeData.transactionValue ?: "0x0",
            chainId = chainId
        )

        return if (simulationResult.success) {
            Timber.i("Simulation passed")
            Result.Success(Unit)
        } else {
            Timber.e("Simulation failed: ${simulationResult.revertReason}")
            Result.Error("Swap will fail: ${simulationResult.revertReason ?: "Unknown error"}")
        }
    }
}
