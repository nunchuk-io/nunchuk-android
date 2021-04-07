package com.nunchuk.android.nav

import android.content.Context
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.wallet.add.AddWalletActivity
import com.nunchuk.android.wallet.assign.AssignSignerActivity

open class WalletNavigatorImpl : WalletNavigator {

    override fun openAddWalletScreen(activityContext: Context) {
        AddWalletActivity.start(activityContext)
    }

    override fun openAssignSignerScreen(activityContext: Context, walletName: String, walletType: WalletType, addressType: AddressType) {
        AssignSignerActivity.start(activityContext, walletName, walletType, addressType)
    }
}