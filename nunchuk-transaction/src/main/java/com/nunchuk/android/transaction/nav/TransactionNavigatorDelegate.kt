package com.nunchuk.android.transaction.nav

import android.app.Activity
import com.nunchuk.android.nav.TransactionNavigator
import com.nunchuk.android.transaction.receive.ReceiveTransactionActivity
import com.nunchuk.android.transaction.receive.address.details.AddressDetailsActivity

interface TransactionNavigatorDelegate : TransactionNavigator {

    override fun openReceiveTransactionScreen(
        activityContext: Activity,
        walletId: String
    ) {
        ReceiveTransactionActivity.start(
            activityContext = activityContext,
            walletId = walletId
        )
    }

    override fun openAddressDetailsScreen(
        activityContext: Activity,
        address: String,
        balance: Long
    ) {
        AddressDetailsActivity.start(
            activityContext = activityContext,
            address = address,
            balance = balance
        )
    }

}