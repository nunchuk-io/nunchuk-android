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

package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber
import javax.inject.Inject

interface GetUnusedSignerFromMasterSignerUseCase {
    fun execute(
        masterSigners: List<MasterSigner>,
        walletType: WalletType,
        addressType: AddressType
    ): Flow<List<SingleSigner>>
}

internal class GetUnusedSignerFromMasterSignerUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GetUnusedSignerFromMasterSignerUseCase {

    override fun execute(
        masterSigners: List<MasterSigner>,
        walletType: WalletType,
        addressType: AddressType
    ) = flow {
        emit(
            masterSigners.mapNotNull { masterSigner ->
                runCatching {
                    if (masterSigner.type == SignerType.NFC) {
                        nativeSdk.getDefaultSignerFromMasterSigner(
                            masterSignerId = masterSigner.id,
                            walletType = walletType.ordinal,
                            addressType = addressType.ordinal
                        )
                    } else {
                        nativeSdk.getUnusedSignerFromMasterSigner(
                            masterSignerId = masterSigner.id,
                            walletType = walletType,
                            addressType = addressType
                        ).also {
                            if (masterSigner.device.needPassPhraseSent) {
                                nativeSdk.clearSignerPassphrase(masterSigner.id)
                            }
                        }
                    }
                }.onFailure { Timber.e(it) }.getOrNull()
            }
        )
    }.flowOn(ioDispatcher)
}