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

package com.nunchuk.android.usecase.signer

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetUnusedSignerFromMasterSignerV2UseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UseCase<GetUnusedSignerFromMasterSignerV2UseCase.Params, SingleSigner>(ioDispatcher) {

    override suspend fun execute(parameters: Params): SingleSigner {
        if (parameters.masterSigners.type == SignerType.NFC) {
            return nativeSdk.getDefaultSignerFromMasterSigner(
                masterSignerId = parameters.masterSigners.id,
                walletType = parameters.walletType.ordinal,
                addressType = parameters.addressType.ordinal
            )
        } else {
            return nativeSdk.getUnusedSignerFromMasterSigner(
                masterSignerId = parameters.masterSigners.id,
                walletType = parameters.walletType,
                addressType = parameters.addressType
            ).also {
                if (parameters.masterSigners.device.needPassPhraseSent) {
                    nativeSdk.clearSignerPassphrase(parameters.masterSigners.id)
                }
            }
        }
    }

    data class Params(
        val masterSigners: MasterSigner,
        val walletType: WalletType,
        val addressType: AddressType
    )
}