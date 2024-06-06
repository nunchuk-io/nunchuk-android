package com.nunchuk.android.core.base

import android.app.Activity
import com.nunchuk.android.core.R
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.widget.NCInfoDialog

fun Activity.showTransferFundDialog(
    navigator: NunchukNavigator,
    newWalletId: String,
    newWalletName: String,
) {
    NCInfoDialog(this).showDialog(
        title = getString(R.string.nc_congratulations),
        message = getString(
            R.string.nc_transfer_func_to_new_wallet_success,
            newWalletName
        ),
        btnYes = getString(R.string.nc_take_me_to_my_new_wallet),
        onYesClick = {
            navigator.openWalletDetailsScreen(
                activityContext = this,
                walletId = newWalletId,
            )
        }
    )
}