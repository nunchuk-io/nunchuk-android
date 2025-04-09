package com.nunchuk.android.core.util

import android.app.Activity
import com.nunchuk.android.core.data.model.QuickWalletParam
import com.nunchuk.android.nav.NunchukNavigator

fun Activity.navigateToSelectWallet(
    navigator: NunchukNavigator,
    quickWalletParam: QuickWalletParam?,
    action: () -> Unit = {}
) {
    if (quickWalletParam != null) {
        navigator.openSelectWalletScreen(
            activityContext = this,
            slots = quickWalletParam.slots,
            type = SelectWalletType.TYPE_INHERITANCE_WALLET,
            claimInheritanceTxParam = quickWalletParam.claimInheritanceTxParam
        )
    } else {
        action()
    }
}