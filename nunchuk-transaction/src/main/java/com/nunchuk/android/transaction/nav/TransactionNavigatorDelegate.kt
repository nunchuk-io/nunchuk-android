package com.nunchuk.android.transaction.nav

import android.app.Activity
import com.nunchuk.android.nav.TransactionNavigator
import com.nunchuk.android.transaction.receive.ReceiveTransactionActivity

interface TransactionNavigatorDelegate : TransactionNavigator {

    override fun openReceiveTransactionScreen(activityContext: Activity, walletId: String) {
        ReceiveTransactionActivity.start(activityContext, walletId)
    }

}