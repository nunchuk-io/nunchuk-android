package com.nunchuk.android.transaction.components.utils

import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.transaction.components.send.amount.InputAmountActivity
import com.nunchuk.android.widget.NCToastMessage

fun BaseActivity<*>.showCreateTransactionError(message: String) {
    hideLoading()
    NCToastMessage(this).showError("Create transaction error due to $message")
}

fun BaseActivity<*>.openTransactionDetailScreen(txId: String, walletId: String, roomId: String) {
    hideLoading()
    ActivityManager.popUntil(InputAmountActivity::class.java, true)
    navigator.openTransactionDetailsScreen(
        activityContext = this,
        walletId = walletId,
        txId = txId,
        roomId = roomId
    )
    NCToastMessage(this).showMessage("Transaction created::$txId")
}

fun BaseActivity<*>.returnActiveRoom(roomId: String) {
    hideLoading()
    finish()
    ActivityManager.popUntilRoot()
    navigator.openRoomDetailActivity(this, roomId)
}