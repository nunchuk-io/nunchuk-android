package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.withdrawbitcoin

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
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
    onNavigateToInputAmount: () -> Unit = {},
    onNavigateToSelectWallet: () -> Unit = {},
    onNavigateToWalletIntermediary: () -> Unit = {},
    onNavigateToAddReceipt: () -> Unit = {},
) {
    composable<ClaimWithdrawBitcoinRoute> {
        val activity = LocalActivity.current as ComponentActivity
        val activityViewModel: ClaimInheritanceViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        val claimData by activityViewModel.claimData.collectAsStateWithLifecycle()

        val walletBalance = claimData.inheritanceAdditional?.balance
        walletBalance?.let { walletBalance ->
            ClaimWithdrawBitcoinScreen(
                balance = walletBalance,
                onNavigateToInputAmount = onNavigateToInputAmount,
                onNavigateToSelectWallet = onNavigateToSelectWallet,
                onNavigateToWalletIntermediary = onNavigateToWalletIntermediary,
                onNavigateToAddReceipt = onNavigateToAddReceipt,
            )
        }
    }
}

fun NavController.navigateToClaimWithdrawBitcoin() {
    navigate(ClaimWithdrawBitcoinRoute)
}

