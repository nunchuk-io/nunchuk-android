package com.nunchuk.android.nav

import android.app.Activity

interface TransactionNavigator {

    fun openReceiveTransactionScreen(activityContext: Activity, walletId: String)

}