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

package com.nunchuk.android.wallet.shared.components.assign

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner

sealed class AssignSignerEvent {
    object ChangeBip32Success : AssignSignerEvent()
    data class Loading(val isLoading: Boolean) : AssignSignerEvent()
    data class ShowError(val message: String) : AssignSignerEvent()
    data class AssignSignerCompletedEvent(
        val roomId: String
    ) : AssignSignerEvent()

    object RequestCacheTapSignerXpub : AssignSignerEvent()
    data class CacheTapSignerXpubError(val error: Throwable?) : AssignSignerEvent()
    data class NfcLoading(val isLoading: Boolean) : AssignSignerEvent()
}

data class AssignSignerState(
    val totalRequireSigns: Int = 0,
    val masterSigners: List<MasterSigner> = emptyList(),
    val remoteSigners: List<SingleSigner> = emptyList(),
    val signers: List<SignerModel> = emptyList(),
    val masterSignerMap: Map<String, SingleSigner> = emptyMap(),
    val selectedSigner: Set<SignerModel> = setOf(),
    val filterRecSigners: List<SingleSigner> = emptyList(),
    val isShowPath: Boolean = false,
)