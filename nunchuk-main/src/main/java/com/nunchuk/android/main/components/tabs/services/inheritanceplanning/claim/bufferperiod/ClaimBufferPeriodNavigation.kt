package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.bufferperiod

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.model.BufferPeriodCountdown
import kotlinx.serialization.Serializable

@Serializable
data class ClaimBufferPeriodRoute(
    val activationTimeMilis: Long,
    val bufferInterval: String,
    val bufferIntervalCount: Int,
    val remainingCount: Int,
    val remainingDisplayName: String
) {
    fun toBufferPeriodCountdown(): BufferPeriodCountdown {
        return BufferPeriodCountdown(
            activationTimeMilis = activationTimeMilis,
            bufferInterval = bufferInterval,
            bufferIntervalCount = bufferIntervalCount,
            remainingCount = remainingCount,
            remainingDisplayName = remainingDisplayName
        )
    }
}

fun NavGraphBuilder.claimBufferPeriod(
    onBackPressed: () -> Unit = {},
    onGotItClick: () -> Unit = {},
) {
    composable<ClaimBufferPeriodRoute> {
        val route = it.toRoute<ClaimBufferPeriodRoute>()
        ClaimBufferPeriodScreen(
            countdown = route.toBufferPeriodCountdown(),
            onBackPressed = onBackPressed,
            onGotItClick = onGotItClick,
        )
    }
}

fun NavController.navigateToClaimBufferPeriod(countdown: BufferPeriodCountdown) {
    navigate(
        ClaimBufferPeriodRoute(
            activationTimeMilis = countdown.activationTimeMilis,
            bufferInterval = countdown.bufferInterval,
            bufferIntervalCount = countdown.bufferIntervalCount,
            remainingCount = countdown.remainingCount,
            remainingDisplayName = countdown.remainingDisplayName
        )
    )
}

