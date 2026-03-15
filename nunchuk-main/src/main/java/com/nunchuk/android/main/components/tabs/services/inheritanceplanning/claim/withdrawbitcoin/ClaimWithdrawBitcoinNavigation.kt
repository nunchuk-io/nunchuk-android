package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.withdrawbitcoin

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.ClaimInheritanceViewModel
import kotlinx.serialization.Serializable

@Serializable
data object ClaimWithdrawBitcoinRoute

fun NavGraphBuilder.claimWithdrawBitcoin(
    snackState: SnackbarHostState,
    onNavigateToInputAmount: (Double) -> Unit = {},
    onNavigateToSelectWallet: () -> Unit = {},
    onNavigateToWalletIntermediary: () -> Unit = {},
    onNavigateToAddReceipt: () -> Unit = {},
    onViewReleaseSchedule: () -> Unit = {},
) {
    composable<ClaimWithdrawBitcoinRoute> {
        val activity = LocalActivity.current as ComponentActivity
        val activityViewModel: ClaimInheritanceViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        val claimData by activityViewModel.claimData.collectAsStateWithLifecycle()

        val walletBalance = claimData.inheritanceAdditional?.balance
        walletBalance?.let {
            ClaimWithdrawBitcoinScreen(
                snackState = snackState,
                bsms = claimData.bsms,
                balance = it,
                availableToWithdraw = claimData.inheritanceAdditional?.availableToWithdraw ?: it,
                hasStages = claimData.inheritanceAdditional?.stages?.isNotEmpty() == true,
                onNavigateToInputAmount = onNavigateToInputAmount,
                onNavigateToSelectWallet = onNavigateToSelectWallet,
                onNavigateToWalletIntermediary = onNavigateToWalletIntermediary,
                onNavigateToAddReceipt = onNavigateToAddReceipt,
                onViewReleaseSchedule = onViewReleaseSchedule,
            )
        }
    }
}

fun NavController.navigateToClaimWithdrawBitcoin() {
    navigate(ClaimWithdrawBitcoinRoute)
}
