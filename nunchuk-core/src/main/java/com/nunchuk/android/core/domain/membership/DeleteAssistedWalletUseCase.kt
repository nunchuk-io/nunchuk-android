package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Inheritance
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class DeleteAssistedWalletUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletRepository: PremiumWalletRepository,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<DeleteAssistedWalletUseCase.Param, Unit>(dispatcher) {
    override suspend fun execute(parameters: Param) {
        val authorizations = mutableListOf<String>()
        parameters.signatures.forEach { (masterFingerprint, signature) ->
            val requestToken = nunchukNativeSdk.createRequestToken(signature, masterFingerprint)
            authorizations.add(requestToken)
        }
        userWalletRepository.deleteAssistedWallet(
            authorizations = authorizations,
            verifyToken = parameters.verifyToken,
            securityQuestionToken = parameters.securityQuestionToken,
            walletId = parameters.walletId
        )
    }

    class Param(
        val signatures: Map<String, String> = emptyMap(),
        val verifyToken: String,
        val securityQuestionToken: String,
        val walletId: String
    )
}