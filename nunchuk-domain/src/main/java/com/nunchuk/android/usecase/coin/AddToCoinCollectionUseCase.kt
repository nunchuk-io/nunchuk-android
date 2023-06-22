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
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class AddToCoinCollectionUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    repository: PremiumWalletRepository,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : BaseSyncCoinUseCase<AddToCoinCollectionUseCase.Param, Unit>(repository, nunchukNativeSdk, ioDispatcher) {
    override suspend fun run(parameters: Param) {
        parameters.coins.forEach { coin ->
            parameters.collectionIds.forEach { collectionId ->
                nunchukNativeSdk.addToCoinCollection(
                    walletId = parameters.walletId,
                    txId = coin.txid,
                    collectionId = collectionId,
                    vout = coin.vout
                )
            }
        }
    }

    class Param(
        override val walletId: String,
        val collectionIds: List<Int>,
        val coins: List<UnspentOutput>,
        override val isAssistedWallet: Boolean,
    ) : BaseSyncCoinUseCase.Param(walletId, isAssistedWallet)
}