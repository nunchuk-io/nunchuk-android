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

import com.nunchuk.android.core.util.toAmount
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.share.model.ExtendTransaction
import com.nunchuk.android.usecase.UseCase
import com.nunchuk.android.usecase.signer.GetRemoteOrMasterSignerUseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class InheritanceClaimCreateTransactionUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletRepository: PremiumWalletRepository,
    private val getRemoteOrMasterSignerUseCase: GetRemoteOrMasterSignerUseCase,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<InheritanceClaimCreateTransactionUseCase.Param, ExtendTransaction>(
    dispatcher
) {
    override suspend fun execute(parameters: Param): ExtendTransaction {
        val isDraft = parameters.isDraft || !parameters.messageId.isNullOrEmpty()
        val userData = userWalletRepository.generateInheritanceClaimCreateTransactionUserData(
            magic = parameters.magic,
            address = parameters.address,
            feeRate = nunchukNativeSdk.valueFromAmount(parameters.feeRate),
            amount = nunchukNativeSdk.valueFromAmount(parameters.amount.toAmount()),
            antiFeeSniping = parameters.antiFeeSniping,
            subtractFeeFromAmount = parameters.subtractFeeFromAmount,
            bsms = parameters.bsms,
            messageId = parameters.messageId
        )
        val signatures = arrayListOf<String>()
        val singleSigners = arrayListOf<SingleSigner>()
        if (parameters.signatures.isEmpty()) {
            parameters.masterSignerIds.forEachIndexed { index, masterSignerId ->
                val signer = getRemoteOrMasterSignerUseCase(
                    GetRemoteOrMasterSignerUseCase.Data(
                        id = masterSignerId,
                        derivationPath = parameters.derivationPaths[index]
                    )
                ).getOrThrow()
                val messagesToSign = nunchukNativeSdk.getHealthCheckMessage(userData)
                val signature = nunchukNativeSdk.signHealthCheckMessage(signer, messagesToSign)
                signatures.add(signature)
                singleSigners.add(signer)
            }
        } else {
            parameters.masterSignerIds.forEachIndexed { index, masterSignerId ->
                val signer = getRemoteOrMasterSignerUseCase(
                    GetRemoteOrMasterSignerUseCase.Data(
                        id = masterSignerId,
                        derivationPath = parameters.derivationPaths[index]
                    )
                ).getOrThrow()
                singleSigners.add(signer)
            }
            signatures.addAll(parameters.signatures)
        }
        val transactionResponse = userWalletRepository.inheritanceClaimCreateTransaction(
            userData = userData,
            masterFingerprints = singleSigners.map { it.masterFingerprint },
            signatures = signatures,
        )

        // Decode (isDraft = true) or decode + sign (isDraft = false) the server PSBT, always
        // through the same native call and therefore the same wallet the signing step uses.
        fun buildClaimTransaction(sign: Boolean): Transaction =
            nunchukNativeSdk.createInheritanceClaimTransaction(
                signers = singleSigners,
                psbt = transactionResponse.psbt,
                subAmount = transactionResponse.subAmount.toString(),
                fee = transactionResponse.fee.toString(),
                feeRate = transactionResponse.feeRate.toString(),
                isDraft = !sign,
                bsms = parameters.bsms,
                subtractFeeFromAmount = transactionResponse.subtractFeeFromAmount
            )

        if (isDraft) {
            val transaction = buildClaimTransaction(sign = false)
            return if (parameters.bsms.isNullOrEmpty() || !parameters.messageId.isNullOrEmpty()) {
                ExtendTransaction(transaction.copy(changeIndex = transactionResponse.changePos))
            } else {
                ExtendTransaction(transaction)
            }
        }

        // The heir's keys are about to sign a PSBT built entirely by the server. Look at it first.
        verifyClaimPaysRequestedDestination(
            parameters = parameters,
            decoded = buildClaimTransaction(sign = false)
        )
        val transaction = buildClaimTransaction(sign = true)

        if (parameters.bsms.isNullOrEmpty()) {
            val transactionAdditional = userWalletRepository.inheritanceClaimingClaim(
                magic = parameters.magic,
                psbt = transaction.psbt,
                bsms = parameters.bsms
            )
            return ExtendTransaction(
                transaction = transaction.copy(
                    status = transactionAdditional.status,
                    changeIndex = transactionResponse.changePos
                )
            )
        } else {
            val wallet = nunchukNativeSdk.parseWalletDescriptor(parameters.bsms)
            userWalletRepository.createServerTransaction(walletId = wallet.id, psbt = transaction.psbt)
            val finalTransaction = transaction.copy(changeIndex = transactionResponse.changePos)
            runCatching {
                nunchukNativeSdk.importPsbt(walletId = wallet.id, psbt = finalTransaction.psbt)
            }
            return ExtendTransaction(
                transaction = finalTransaction,
                walletId = wallet.id
            )
        }
    }

    /**
     * The one claim input that originates on this device is the destination address the heir
     * entered, so that is the one thing the server-built PSBT is checked against before any key
     * signs it: it must pay that address. [decoded] comes from the same native decode the signing
     * call performs, and the confirm screen already renders "Send to address" from it.
     *
     * Nothing else is checked on purpose. Output count, change position, the subtract-fee flag and
     * the amounts all come from the server or depend on the plan's shape (a release-schedule claim
     * re-locks the unreleased share), and every rule built on them has rejected legitimate claims.
     */
    private fun verifyClaimPaysRequestedDestination(parameters: Param, decoded: Transaction) {
        val expectedAddress = parameters.address.trim()
        if (expectedAddress.isEmpty()) {
            throw InheritanceClaimPsbtMismatchException(
                "The claim transaction has no destination address to verify against."
            )
        }
        // Bech32 addresses are case-insensitive; base58 ones differing only in case fail checksum.
        val paysDestination = decoded.outputs.any {
            it.first.equals(expectedAddress, ignoreCase = true)
        }
        if (!paysDestination) {
            throw InheritanceClaimPsbtMismatchException(
                "The claim transaction does not pay the destination address you entered."
            )
        }
    }

    data class Param(
        val isDraft: Boolean,
        val masterSignerIds: List<String>,
        val address: String,
        val magic: String,
        val feeRate: Amount = Amount(-1),
        val derivationPaths: List<String>,
        val amount: Double,
        val antiFeeSniping: Boolean,
        val bsms: String? = null,
        val subtractFeeFromAmount: Boolean? = null,
        val messageId: String? = null,
        val signatures: List<String> = emptyList()
    )
}

/**
 * Raised when the claim PSBT returned by the server does not pay what the heir asked to send.
 * The heir's keys have not signed anything at this point.
 */
class InheritanceClaimPsbtMismatchException(message: String) : Exception(message)
