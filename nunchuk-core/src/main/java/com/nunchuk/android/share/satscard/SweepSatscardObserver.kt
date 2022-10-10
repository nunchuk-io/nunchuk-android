package com.nunchuk.android.share.satscard

import com.nunchuk.android.core.R
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.showOrHideNfcLoading
import com.nunchuk.android.widget.NCToastMessage

fun BaseActivity<*>.observerSweepSatscard(sweepSatscardViewModel: SweepSatscardViewModel, nfcViewModel: NfcViewModel, getWalletId: () -> String) {
    flowObserver(sweepSatscardViewModel.event) { event ->
        when (event) {
            is SweepEvent.Error -> if (nfcViewModel.handleNfcError(event.e).not()) {
                NCToastMessage(this).showError(event.e?.message.orUnknownError())
            }
            is SweepEvent.NfcLoading -> showOrHideNfcLoading(event.isLoading)
            is SweepEvent.SweepLoadingEvent -> showOrHideLoading(
                event.isLoading,
                title = getString(R.string.nc_sweeping_is_progress),
                message = getString(R.string.nc_make_sure_internet)
            )
            is SweepEvent.SweepSuccess -> handleSweepSuccess(event, getWalletId())
        }
    }
}

fun BaseActivity<*>.handleSweepSuccess(event: SweepEvent.SweepSuccess, walletId: String) {
    ActivityManager.popUntilRoot()
    NcToastManager.scheduleShowMessage(getString(R.string.nc_satscard_sweeped))
    if (walletId.isNotEmpty()) {
        navigator.openWalletDetailsScreen(this, walletId)
    }
    navigator.openTransactionDetailsScreen(this, walletId, event.transaction.txId, "", "", event.transaction)
}