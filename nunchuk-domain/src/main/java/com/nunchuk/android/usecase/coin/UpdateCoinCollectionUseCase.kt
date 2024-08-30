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

package com.nunchuk.android.usecase.coin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.CoinCollection
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class UpdateCoinCollectionUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    repository: PremiumWalletRepository,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : BaseSyncCoinUseCase<UpdateCoinCollectionUseCase.Param, Boolean>(
    repository,
    nunchukNativeSdk,
    ioDispatcher
) {
    override suspend fun run(parameters: Param): Boolean {
        return nunchukNativeSdk.updateCoinCollection(
            walletId = parameters.walletId,
            coinCollection = parameters.coinCollection,
            applyToExistingCoins = parameters.applyToExistingCoins
        )
    }

    class Param(
        override val groupId: String?,
        override val walletId: String,
        val coinCollection: CoinCollection,
        val applyToExistingCoins: Boolean,
        override val isAssistedWallet: Boolean
    ) : BaseSyncCoinUseCase.Param(groupId, walletId, isAssistedWallet)
}