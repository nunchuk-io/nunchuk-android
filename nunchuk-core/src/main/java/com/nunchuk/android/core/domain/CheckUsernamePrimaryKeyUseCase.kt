package com.nunchuk.android.core.domain

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.SignerSoftwareRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CheckUsernamePrimaryKeyUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val signerSoftwareRepository: SignerSoftwareRepository
) : UseCase<CheckUsernamePrimaryKeyUseCase.Param, Unit>(dispatcher) {
    override suspend fun execute(parameters: Param) {
        signerSoftwareRepository.pKeyCheckUsername(parameters.username)
    }

    class Param(val username: String)
}