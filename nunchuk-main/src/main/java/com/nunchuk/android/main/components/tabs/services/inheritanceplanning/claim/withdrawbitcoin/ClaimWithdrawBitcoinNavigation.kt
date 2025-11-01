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
        val activityViewModel: ClaimInheritanceViewModel = hiltViewModel(viewModelStoreOwner = activity)
        val claimData by activityViewModel.claimData.collectAsStateWithLifecycle()

        claimData?.let { data ->
            data.walletBalance?.let { walletBalance ->
                ClaimWithdrawBitcoinScreen(
                    balance = walletBalance,
                    onNavigateToInputAmount = {
                        onNavigateToInputAmount(
                            walletBalance,
                            data.signers,
                            data.magic,
                            data.derivationPaths
                        )
                    },
                    onNavigateToSelectWallet = {
                        onNavigateToSelectWallet(
                            walletBalance,
                            data.signers,
                            data.magic,
                            data.derivationPaths
                        )
                    },
                    onNavigateToWalletIntermediary = {
                        onNavigateToWalletIntermediary(
                            walletBalance,
                            data.signers,
                            data.magic,
                            data.derivationPaths
                        )
                    },
                    onNavigateToAddReceipt = {
                        onNavigateToAddReceipt(
                            walletBalance,
                            data.signers,
                            data.magic,
                            data.derivationPaths
                        )
                    },
                )
            }
        }
    }
}

fun NavController.navigateToClaimWithdrawBitcoin(
    walletBalance: Double,
    signers: List<SignerModel>,
    magic: String,
    derivationPaths: List<String>,
    activityViewModel: ClaimInheritanceViewModel
) {
    activityViewModel.setWithdrawData(walletBalance, signers, magic, derivationPaths)
    navigate(ClaimWithdrawBitcoinRoute)
}

