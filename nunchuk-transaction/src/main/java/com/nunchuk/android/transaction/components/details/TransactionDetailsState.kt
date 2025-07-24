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

package com.nunchuk.android.transaction.components.details

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.ByzantineMember
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.ScriptNode
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.transaction.ServerTransaction
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.MiniscriptTimelockBased

data class TransactionDetailsState(
    val transaction: Transaction = Transaction(),
    val serverTransaction: ServerTransaction? = null,
    val signers: List<SignerModel> = emptyList(),
    val coins: List<UnspentOutput> = emptyList(),
    val tags: Map<Int, CoinTag> = emptyMap(),
    val members: List<ByzantineMember> = emptyList(),
    val userRole: AssistedWalletRole = AssistedWalletRole.NONE,
    val txInputCoins: List<UnspentOutput> = emptyList(),
    val addressType: AddressType = AddressType.NATIVE_SEGWIT,
    val isValueKeySetDisable: Boolean = false,
    val defaultKeySetIndex: Int = 0,
    val savedAddress: Map<String, String> = emptyMap(),
    val hideFiatCurrency: Boolean = false,
    val signerMap: Map<String, SignerModel?> = emptyMap(),
)

data class TransactionMiniscriptUiState(
    val isMiniscriptWallet: Boolean = false,
    val scriptNode: ScriptNode? = null,
    val satisfiableMap: Map<String, Boolean> = emptyMap(),
    val topLevelDisableNode: ScriptNode? = null,
    val lockedTime: Long = 0L,
    val lockedBase: MiniscriptTimelockBased = MiniscriptTimelockBased.NONE,
    val chainTip: Long = 0L,
) {
    val isTimelockedActive: Boolean =
        lockedBase != MiniscriptTimelockBased.NONE && lockedTime > 0 && when (lockedBase) {
            // compare with current time (seconds)
            MiniscriptTimelockBased.TIME_LOCK -> System.currentTimeMillis() / 1000 < lockedTime
            MiniscriptTimelockBased.HEIGHT_LOCK -> chainTip < lockedTime
            else -> false
        }
}