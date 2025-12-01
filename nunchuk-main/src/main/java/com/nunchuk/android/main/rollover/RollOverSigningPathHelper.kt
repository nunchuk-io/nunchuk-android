package com.nunchuk.android.main.rollover

import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.nunchuk.android.core.data.model.RollOverWalletParam
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.model.SigningPath
import com.nunchuk.android.nav.NunchukNavigator
import kotlinx.coroutines.launch

/**
 * Helper function to handle miniscript signing path checking for rollover fragments.
 * 
 * @param rollOverWalletViewModel The view model instance
 * @param rollOverWalletParam The rollover wallet param (can be null for consolidation)
 * @param address The destination address
 * @param isMiniscript Whether the wallet is a miniscript wallet
 * @param selectSigningPathLauncher The launcher for selecting signing path when multiple paths exist
 * @param navigator The navigator instance
 * @param openEstimateFeeScreen Callback to open estimate fee screen with the signing path
 */
fun Fragment.handleRollOverSigningPathCheck(
    rollOverWalletViewModel: RollOverWalletViewModel,
    rollOverWalletParam: RollOverWalletParam?,
    address: String,
    isMiniscript: Boolean,
    selectSigningPathLauncher: ActivityResultLauncher<android.content.Intent>,
    navigator: NunchukNavigator,
    openEstimateFeeScreen: (signingPath: SigningPath?) -> Unit
) {
    if (!isMiniscript) {
        openEstimateFeeScreen(null)
        return
    }

    viewLifecycleOwner.lifecycleScope.launch {
        val pathSize = if (rollOverWalletParam != null) {
            rollOverWalletViewModel.checkSigningPathsForRollOver(rollOverWalletParam)
        } else {
            rollOverWalletViewModel.checkSigningPathsForConsolidation()
        }

        when (pathSize) {
            0 -> openEstimateFeeScreen(null)
            1 -> {
                val signingPath = rollOverWalletViewModel.getSigningPath()
                openEstimateFeeScreen(signingPath)
            }
            else -> {
                navigator.selectMiniscriptSigningPath(
                    launcher = selectSigningPathLauncher,
                    activityContext = requireActivity(),
                    walletId = rollOverWalletViewModel.getOldWalletId(),
                    outputAmount = rollOverWalletViewModel.getOldWallet().balance.pureBTC(),
                    address = address,
                    subtractFeeFromAmount = true,
                    rollOverWalletParam = rollOverWalletParam
                )
            }
        }
    }
}

