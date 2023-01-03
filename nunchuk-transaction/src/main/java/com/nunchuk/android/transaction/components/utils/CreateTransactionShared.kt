/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.transaction.components.utils

import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.transaction.components.send.amount.InputAmountActivity
import com.nunchuk.android.widget.NCToastMessage

fun BaseActivity<*>.showCreateTransactionError(message: String) {
    hideLoading()
    NCToastMessage(this).showError("Create transaction error due to $message")
}

fun BaseActivity<*>.openTransactionDetailScreen(txId: String, walletId: String, roomId: String, isInheritanceClaimingFlow: Boolean) {
    hideLoading()
    ActivityManager.popUntil(InputAmountActivity::class.java, true)
    navigator.openTransactionDetailsScreen(
        activityContext = this,
        walletId = walletId,
        txId = txId,
        roomId = roomId,
        isInheritanceClaimingFlow = isInheritanceClaimingFlow
    )
    NCToastMessage(this).showMessage("Transaction created::$txId")
}

fun BaseActivity<*>.returnActiveRoom(roomId: String) {
    hideLoading()
    finish()
    ActivityManager.popUntilRoot()
    navigator.openRoomDetailActivity(this, roomId)
}