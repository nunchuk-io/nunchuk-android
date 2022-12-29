package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Inheritance
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CreateOrUpdateInheritanceUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletRepository: PremiumWalletRepository,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<CreateOrUpdateInheritanceUseCase.Param, Inheritance>(dispatcher) {
    override suspend fun execute(parameters: Param): Inheritance {
        val authorizations = mutableListOf<String>()
        parameters.signatures.forEach { (masterFingerprint, signature) ->
            val requestToken = nunchukNativeSdk.createRequestToken(signature, masterFingerprint)
            authorizations.add(requestToken)
        }
        return userWalletRepository.createOrUpdateInheritance(
            authorizations = authorizations,
            verifyToken = parameters.verifyToken,
            userData = parameters.userData,
            securityQuestionToken = parameters.securityQuestionToken,
            isUpdate = parameters.isUpdate
        )
    }

    class Param(
        val signatures: Map<String, String> = emptyMap(),
        val userData: String,
        val verifyToken: String,
        val securityQuestionToken: String,
        val isUpdate: Boolean
    )
}