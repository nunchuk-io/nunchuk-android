package com.nunchuk.android.usecase.signer

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.SignerSoftwareRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SaveSeedPhraseDecreaseTransitionUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val repository: SignerSoftwareRepository,
) : UseCase<SaveSeedPhraseDecreaseTransitionUseCase.Param, Unit>(dispatcher) {

    override suspend fun execute(parameters: Param) {
        if (parameters.pendingHours > 0) {
            repository.setSeedPhraseDecreaseTransition(parameters.pendingHours)
        } else {
            repository.clearSeedPhraseDecreaseTransition()
        }
    }

    data class Param(val pendingHours: Int = 0)
}
