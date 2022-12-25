package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
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
            feeRate = nunchukNativeSdk.valueFromAmount(parameters.feeRate)
        )
        val signer = nunchukNativeSdk.getDefaultSignerFromMasterSigner(
            masterSignerId = parameters.masterSignerId,
            walletType = WalletType.MULTI_SIG.ordinal,
            addressType = AddressType.ANY.ordinal
        )
        val messagesToSign = nunchukNativeSdk.getHealthCheckMessage(userData)
        val signature = nunchukNativeSdk.signHealthCheckMessage(signer, messagesToSign)
        val transactionResponse = userWalletRepository.inheritanceClaimCreateTransaction(
            userData = userData,
            masterFingerprint = signer.masterFingerprint,
            signature = signature
        )
        val transaction = nunchukNativeSdk.createTransaction(
            signer = signer,
            psbt = transactionResponse.psbt,
            subAmount = transactionResponse.subAmount.toString(),
            fee = transactionResponse.fee.toString(),
            feeRate = transactionResponse.feeRate.toString()
        )
        userWalletRepository.inheritanceClaimingClaim(
            magic = parameters.magic,
            psbt = transaction.psbt
        )
        return transaction
    }

    data class Param(
        val masterSignerId: String,
        val address: String,
        val magic: String,
        val feeRate: Amount = Amount(-1)
    )
}