package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.InheritanceAdditional
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetInheritanceClaimStateUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletRepository: PremiumWalletRepository,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<GetInheritanceClaimStateUseCase.Param, InheritanceAdditional>(
    dispatcher
) {
    override suspend fun execute(parameters: Param): InheritanceAdditional {
        val userData = userWalletRepository.generateInheritanceClaimStatusUserData(parameters.magic)
        val signer = nunchukNativeSdk.getSignerFromMasterSigner(
            masterSignerId = parameters.signer.id,
            path = "m/48h/0h/0h"
        )
        val messagesToSign = nunchukNativeSdk.getHealthCheckMessage(userData)
        val signature = nunchukNativeSdk.signHealthCheckMessage(signer, messagesToSign)
        return userWalletRepository.inheritanceClaimStatus(
            userData = userData,
            masterFingerprint = signer.masterFingerprint,
            signature = signature
        )
    }

    data class Param(val signer: SignerModel, val magic: String)
}