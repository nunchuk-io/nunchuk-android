package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.QuestionsAndAnswer
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class LockdownUpdateUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletRepository: PremiumWalletRepository,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<LockdownUpdateUseCase.Param, Unit>(dispatcher) {
    override suspend fun execute(parameters: Param) {
        val authorizations = mutableListOf<String>()
        parameters.signatures.forEach { (masterFingerprint, signature) ->
            val requestToken = nunchukNativeSdk.createRequestToken(signature, masterFingerprint)
            authorizations.add(requestToken)
        }
        return userWalletRepository.lockdownUpdate(
            authorizations = authorizations,
            verifyToken = parameters.verifyToken,
            userData =  parameters.userData,
            securityQuestionToken = parameters.securityQuestionToken
        )
    }

    class Param(
        val signatures: Map<String, String>,
        val userData: String,
        val verifyToken: String,
        val securityQuestionToken: String
    )
}