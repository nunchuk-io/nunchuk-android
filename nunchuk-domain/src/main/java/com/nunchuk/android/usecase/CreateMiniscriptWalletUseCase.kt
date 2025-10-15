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
import com.nunchuk.android.usecase.wallet.AddWalletBannerStateUseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CreateMiniscriptWalletUseCase @Inject constructor(
    private val nunchukNativeSdk: NunchukNativeSdk,
    private val addWalletBannerStateUseCase: AddWalletBannerStateUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<CreateMiniscriptWalletUseCase.Params, Wallet>(ioDispatcher) {

    override suspend fun execute(parameters: Params): Wallet {
        val createdWallet = nunchukNativeSdk.createMiniscriptWallet(
            miniscriptTemplate = parameters.miniscriptTemplate,
            signerMap = parameters.signerMap,
            name = parameters.name,
            description = parameters.description,
            addressType = parameters.addressType,
            allowUsedSigner = parameters.allowUsedSigner,
            decoyPin = parameters.decoyPin
        )
        addWalletBannerStateUseCase(createdWallet.id).getOrNull()
        return createdWallet
    }

    data class Params(
        val miniscriptTemplate: String,
        val signerMap: Map<String, SingleSigner>,
        val name: String,
        val description: String,
        val addressType: Int,
        val allowUsedSigner: Boolean,
        val decoyPin: String
    )
}