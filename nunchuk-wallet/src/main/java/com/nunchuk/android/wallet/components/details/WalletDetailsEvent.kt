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

package com.nunchuk.android.wallet.components.details

import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.BannerState
import com.nunchuk.android.model.FreeGroupMessage
import com.nunchuk.android.model.HistoryPeriod
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.type.MiniscriptTimelockBased

sealed class WalletDetailsEvent {
    data class Loading(val loading: Boolean) : WalletDetailsEvent()
    data class UpdateUnusedAddress(val address: String) : WalletDetailsEvent()
    data class SendMoneyEvent(val walletExtended: WalletExtended) : WalletDetailsEvent()
    data class WalletDetailsError(val message: String) : WalletDetailsEvent()
    data class PaginationTransactions(val hasTransactions: Boolean = true) : WalletDetailsEvent()
    data object ImportPSBTSuccess : WalletDetailsEvent()
    data class OpenSetupGroupWallet(val groupId: String) : WalletDetailsEvent()
    data class SaveLocalFile(val isSuccess: Boolean) : WalletDetailsEvent()
    data class ShareBSMS(val filePath: String) : WalletDetailsEvent()
}

data class WalletDetailsState(
    val walletExtended: WalletExtended = WalletExtended(),
    val transactions: List<Transaction> = emptyList(),
    val isLeaveRoom: Boolean = false,
    val isAssistedWallet: Boolean = false,
    val walletStatus: String? = null,
    val isForceRefreshProcessing: Boolean = false,
    val hideWalletDetailLocal: Boolean = false,
    val isHasCoin: Boolean = true,
    val groupId: String? = null,
    val role: AssistedWalletRole = AssistedWalletRole.NONE,
    val groupChatMessages: List<FreeGroupMessage> = emptyList(),
    val isFreeGroupWallet: Boolean = false,
    val historyPeriods: List<HistoryPeriod> = emptyList(),
    val selectedHistoryPeriod: HistoryPeriod? = null,
    val unreadMessagesCount: Int = 0,
    val uid: String = "",
    val replaceGroups: Map<String, Boolean> = emptyMap(),
    val isDeprecatedGroupWallet: Boolean = false,
    val isNeedBackUpGroupWallet: Boolean = false,
    val chatBarState: ChatBarState = ChatBarState.EXPANDED,
    val bannerState: BannerState? = null,
    val nearestTimeLock: Pair<MiniscriptTimelockBased, Long>? = null,
    val noTimelockCoinsAmount: Amount = Amount(),
    val currentBlock: Int = 0,
)