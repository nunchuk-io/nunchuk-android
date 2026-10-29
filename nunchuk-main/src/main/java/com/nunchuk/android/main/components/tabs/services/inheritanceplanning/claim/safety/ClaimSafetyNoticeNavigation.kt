package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.safety

import androidx.compose.material3.SnackbarHostState
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable
data class ClaimSafetyNoticeRoute(val notice: ClaimSafetyNotice)

fun NavGraphBuilder.claimSafetyNotice(
    snackState: SnackbarHostState,
    onBackPressed: () -> Unit = {},
    onContinue: (ClaimSafetyNotice) -> Unit = {},
) {
    composable<ClaimSafetyNoticeRoute> {
        val notice = it.toRoute<ClaimSafetyNoticeRoute>().notice
        ClaimSafetyNoticeScreen(
            notice = notice,
            snackState = snackState,
            onBackPressed = onBackPressed,
            onContinue = { onContinue(notice) },
        )
    }
}

fun NavController.navigateToClaimSafetyNotice(notice: ClaimSafetyNotice) {
    navigate(ClaimSafetyNoticeRoute(notice))
}
