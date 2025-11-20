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
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.CoinCollection
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.DraftRollOverTransaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class DraftRollOverTransactionsUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<DraftRollOverTransactionsUseCase.Data, List<DraftRollOverTransaction>>(dispatcher) {
    override suspend fun execute(parameters: Data): List<DraftRollOverTransaction> {
        val isEmptyTagsAndCollections =
            parameters.tags.isEmpty() && parameters.collections.isEmpty()
        return if (isEmptyTagsAndCollections) {
            nunchukNativeSdk.draftRollOver11Transactions(
                walletId = parameters.oldWalletId,
                newWalletId = parameters.newWalletId,
                feeRate = parameters.feeRate
            ).toList().map {
                val coins = nunchukNativeSdk.getCoinsFromTxInputs(parameters.oldWalletId, it.inputs)
                DraftRollOverTransaction(
                    transaction = it,
                    tagIds = coins.map { coin -> coin.tags }.flatten().toSet(),
                    collectionIds = coins.map { coin -> coin.collection }.flatten().toSet()
                )
            }
        } else {
            nunchukNativeSdk.draftRollOverTransactions(
                walletId = parameters.oldWalletId,
                newWalletId = parameters.newWalletId,
                tags = parameters.tags,
                collections = parameters.collections,
                feeRate = parameters.feeRate
            ).toList()
        }
    }

    class Data(
        val newWalletId: String,
        val oldWalletId: String,
        val tags: List<CoinTag>,
        val collections: List<CoinCollection>,
        val feeRate: Amount
    )
}