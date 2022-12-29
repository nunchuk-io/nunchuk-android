package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class UpdateServerKeysUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletsRepository: PremiumWalletRepository,
) : UseCase<UpdateServerKeysUseCase.Param, KeyPolicy>(dispatcher) {
    override suspend fun execute(parameters: Param) : KeyPolicy {
        return userWalletsRepository.updateServerKeys(
            signatures = parameters.signatures,
            keyIdOrXfp = parameters.keyIdOrXfp,
            token = parameters.token,
            body = parameters.body,
            securityQuestionToken = parameters.securityQuestionToken
        )
    }

    data class Param(
        val body: String,
        val keyIdOrXfp: String,
        val signatures: Map<String, String> = emptyMap(),
        val token: String,
        val securityQuestionToken: String = "",
    )
}