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

package com.nunchuk.android.wallet.components.review

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.GetTapSignerStatusByIdUseCase
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nav.args.ReviewWalletArgs
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.type.WalletType.ESCROW
import com.nunchuk.android.type.WalletType.SINGLE_SIG
import com.nunchuk.android.usecase.CreateWalletUseCase
import com.nunchuk.android.wallet.components.review.ReviewWalletEvent.CreateWalletErrorEvent
import com.nunchuk.android.wallet.components.review.ReviewWalletEvent.CreateWalletSuccessEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

internal class ReviewWalletViewModel @AssistedInject constructor(
    @Assisted private val args: ReviewWalletArgs,
    private val createWalletUseCase: CreateWalletUseCase,
    private val accountManager: AccountManager,
    private val getTapSignerStatusByIdUseCase: GetTapSignerStatusByIdUseCase
) : NunchukViewModel<Unit, ReviewWalletEvent>() {

    override val initialState = Unit

    fun handleContinueEvent(
        walletName: String,
        walletType: WalletType,
        addressType: AddressType,
        totalRequireSigns: Int,
        signers: List<SingleSigner>,
        decoyPin: String
    ) {
        val totalSigns = signers.size
        val normalizeWalletType =
            if (walletType == ESCROW) ESCROW else if (totalSigns > 1) WalletType.MULTI_SIG else SINGLE_SIG
        viewModelScope.launch {
            createWalletUseCase(
                CreateWalletUseCase.Params(
                    name = walletName,
                    totalRequireSigns = totalRequireSigns,
                    signers = signers,
                    addressType = addressType,
                    isEscrow = normalizeWalletType == ESCROW,
                    decoyPin = decoyPin,
                )
            ).onSuccess {
                event(CreateWalletSuccessEvent(it))
            }.onFailure {
                event(CreateWalletErrorEvent(it.message.orUnknownError()))
            }
        }
    }

    fun mapSigners(): List<SignerModel> {
        return args.signers.map {
            it.toModel(isPrimaryKey = it.hasMasterSigner && accountManager.getPrimaryKeyInfo()?.xfp == it.masterFingerprint)
        }.map {
            if (it.type == SignerType.NFC) {
                val status = runBlocking { getTapSignerStatusByIdUseCase(it.id) }
                return@map it.copy(cardId = status.getOrNull()?.ident.orEmpty())
            }
            return@map it
        }
    }

    @AssistedFactory
    internal interface Factory {
        fun create(args: ReviewWalletArgs): ReviewWalletViewModel
    }

}
