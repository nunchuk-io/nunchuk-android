package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.releaseschedule

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object ClaimReleaseScheduleRoute

fun NavGraphBuilder.claimReleaseSchedule(
    onWithdrawClicked: () -> Unit = {},
    onDoneClicked: () -> Unit = {},
) {
    composable<ClaimReleaseScheduleRoute> {
        ClaimReleaseScheduleScreen(
            onWithdrawClicked = onWithdrawClicked,
            onDoneClicked = onDoneClicked,
        )
    }
}

fun NavController.navigateToClaimReleaseSchedule() {
    navigate(ClaimReleaseScheduleRoute)
}
