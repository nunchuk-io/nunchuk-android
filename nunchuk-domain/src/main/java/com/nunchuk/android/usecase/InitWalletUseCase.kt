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

import com.nunchuk.android.model.NunchukMatrixEvent
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.AddressType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface InitWalletUseCase {
    fun execute(
        roomId: String,
        name: String,
        totalSigns: Int,
        requireSigns: Int,
        addressType: AddressType,
        isEscrow: Boolean,
        des: String,
        signers: List<SingleSigner>
    ): Flow<NunchukMatrixEvent>
}

internal class InitWalletUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : InitWalletUseCase {

    override fun execute(
        roomId: String,
        name: String,
        totalSigns: Int,
        requireSigns: Int,
        addressType: AddressType,
        isEscrow: Boolean,
        des: String,
        signers: List<SingleSigner>
    ) = flow {
        emit(
            nativeSdk.initSharedWallet(
                roomId = roomId,
                name = name,
                totalSigns = totalSigns,
                requireSigns = requireSigns,
                addressType = addressType,
                isEscrow = isEscrow,
                des = des,
                signers = signers
            )
        )
    }
}