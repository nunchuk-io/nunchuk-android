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

package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class InheritanceClaimCreateTransactionUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletRepository: PremiumWalletRepository,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<InheritanceClaimCreateTransactionUseCase.Param, Transaction>(
    dispatcher
) {
    override suspend fun execute(parameters: Param): Transaction {
        val userData = userWalletRepository.generateInheritanceClaimCreateTransactionUserData(
            magic = parameters.magic,
            address = parameters.address,
            feeRate = nunchukNativeSdk.valueFromAmount(parameters.feeRate),
            amount = parameters.amount,
            antiFeeSniping = parameters.antiFeeSniping,
        )
        val signatures = arrayListOf<String>()
        val singleSigners = arrayListOf<SingleSigner>()
        parameters.masterSignerIds.forEachIndexed { index, masterSignerId ->
            val signer = nunchukNativeSdk.getSignerFromMasterSigner(
                masterSignerId = masterSignerId,
                path = parameters.derivationPaths[index]
            )
            val messagesToSign = nunchukNativeSdk.getHealthCheckMessage(userData)
            val signature = nunchukNativeSdk.signHealthCheckMessage(signer, messagesToSign)
            signatures.add(signature)
            singleSigners.add(signer)
        }
        val transactionResponse = userWalletRepository.inheritanceClaimCreateTransaction(
            userData = userData,
            masterFingerprints = singleSigners.map { it.masterFingerprint },
            signatures = signatures,
        )
        val transaction = nunchukNativeSdk.createInheritanceClaimTransaction(
            signers = singleSigners,
            psbt = transactionResponse.psbt,
            subAmount = transactionResponse.subAmount.toString(),
            fee = transactionResponse.fee.toString(),
            feeRate = transactionResponse.feeRate.toString(),
            isDraft = parameters.isDraft,
        )
        if (parameters.isDraft) return transaction
        val transactionAdditional = userWalletRepository.inheritanceClaimingClaim(
            magic = parameters.magic,
            psbt = transaction.psbt
        )
        return transaction.copy(status = transactionAdditional.status)
    }

    data class Param(
        val isDraft: Boolean,
        val masterSignerIds: List<String>,
        val address: String,
        val magic: String,
        val feeRate: Amount = Amount(-1),
        val derivationPaths: List<String>,
        val amount: Double,
        val antiFeeSniping: Boolean
    )
}