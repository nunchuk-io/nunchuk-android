package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Inheritance
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class RequestPlanningInheritanceUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletRepository: PremiumWalletRepository,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<RequestPlanningInheritanceUseCase.Param, String>(dispatcher) {
    override suspend fun execute(parameters: Param): String {
        val authorizations = mutableListOf<String>()
        parameters.signatures.forEach { (masterFingerprint, signature) ->
            val requestToken = nunchukNativeSdk.createRequestToken(signature, masterFingerprint)
            authorizations.add(requestToken)
        }
        return userWalletRepository.requestPlanningInheritance(
            authorizations = authorizations,
            userData = parameters.userData,
            walletId = parameters.walletId,
            groupId = parameters.groupId,
        )
    }

    class Param(
        val signatures: Map<String, String> = emptyMap(),
        val userData: String,
        val walletId: String,
        val groupId: String,
    )
}