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

package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.AddressType
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CreateWalletUseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    private val getOrCreateRootDirUseCase: GetOrCreateRootDirUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<CreateWalletUseCase.Params, Wallet>(ioDispatcher) {
    override suspend fun execute(parameters: Params): Wallet {
        if (parameters.decoyPin.isNotEmpty()) {
            val path = getOrCreateRootDirUseCase(Unit).getOrThrow()
            nativeSdk.createNewDecoyPin(storagePath = path, pin = parameters.decoyPin)
        }
        return nativeSdk.createWallet(
            name = parameters.name,
            totalRequireSigns = parameters.totalRequireSigns,
            signers = parameters.signers,
            addressType = parameters.addressType,
            isEscrow = parameters.isEscrow,
            description = parameters.description,
            decoyPin = parameters.decoyPin
        )
    }

    data class Params(
        val name: String,
        val totalRequireSigns: Int,
        val signers: List<SingleSigner>,
        val addressType: AddressType,
        val isEscrow: Boolean,
        val description: String = "",
        val decoyPin: String = ""
    )
}