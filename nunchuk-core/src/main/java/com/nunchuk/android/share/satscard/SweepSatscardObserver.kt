/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
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

package com.nunchuk.android.share.satscard

import com.nunchuk.android.core.R
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.base.BaseComposeActivity
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

fun BaseComposeActivity.observerSweepSatscard(sweepSatscardViewModel: SweepSatscardViewModel, nfcViewModel: NfcViewModel, getWalletId: () -> String) {
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
    navigator.openTransactionDetailsScreen(
        activityContext = this,
        walletId = walletId,
        txId = event.transaction.txId,
        initEventId = "",
        roomId = "",
        transaction = event.transaction
    )
}

fun BaseComposeActivity.handleSweepSuccess(event: SweepEvent.SweepSuccess, walletId: String) {
    ActivityManager.popUntilRoot()
    NcToastManager.scheduleShowMessage(getString(R.string.nc_satscard_sweeped))
    if (walletId.isNotEmpty()) {
        navigator.openWalletDetailsScreen(this, walletId)
    }
    navigator.openTransactionDetailsScreen(
        activityContext = this,
        walletId = walletId,
        txId = event.transaction.txId,
        initEventId = "",
        roomId = "",
        transaction = event.transaction
    )
}