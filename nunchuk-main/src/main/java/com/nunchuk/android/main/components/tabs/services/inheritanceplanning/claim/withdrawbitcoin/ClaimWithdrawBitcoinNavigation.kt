package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.withdrawbitcoin

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.ClaimInheritanceViewModel
import kotlinx.serialization.Serializable

@Serializable
data object ClaimWithdrawBitcoinRoute

fun NavGraphBuilder.claimWithdrawBitcoin(
    onNavigateToInputAmount: (
        walletBalance: Double,
        signers: List<SignerModel>,
        magic: String,
        derivationPaths: List<String>
    ) -> Unit = { _, _, _, _ -> },
    onNavigateToSelectWallet: (
        walletBalance: Double,
        signers: List<SignerModel>,
        magic: String,
        derivationPaths: List<String>
    ) -> Unit = { _, _, _, _ -> },
    onNavigateToWalletIntermediary: (
        walletBalance: Double,
        signers: List<SignerModel>,
        magic: String,
        derivationPaths: List<String>
    ) -> Unit = { _, _, _, _ -> },
    onNavigateToAddReceipt: (
        walletBalance: Double,
        signers: List<SignerModel>,
        magic: String,
        derivationPaths: List<String>
    ) -> Unit = { _, _, _, _ -> },
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
                onNavigateToInputAmount = {
                    onNavigateToInputAmount(
                        walletBalance,
                        claimData.signers,
                        claimData.magic,
                        claimData.derivationPaths
                    )
                },
                onNavigateToSelectWallet = {
                    onNavigateToSelectWallet(
                        walletBalance,
                        claimData.signers,
                        claimData.magic,
                        claimData.derivationPaths
                    )
                },
                onNavigateToWalletIntermediary = {
                    onNavigateToWalletIntermediary(
                        walletBalance,
                        claimData.signers,
                        claimData.magic,
                        claimData.derivationPaths
                    )
                },
                onNavigateToAddReceipt = {
                    onNavigateToAddReceipt(
                        walletBalance,
                        claimData.signers,
                        claimData.magic,
                        claimData.derivationPaths
                    )
                },
            )
        }
    }
}

fun NavController.navigateToClaimWithdrawBitcoin() {
    navigate(ClaimWithdrawBitcoinRoute)
}

