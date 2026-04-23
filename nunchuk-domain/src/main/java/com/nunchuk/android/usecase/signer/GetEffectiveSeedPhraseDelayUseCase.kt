package com.nunchuk.android.usecase.signer

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.SignerSoftwareRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetEffectiveSeedPhraseDelayUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val repository: SignerSoftwareRepository,
) : UseCase<Unit, Int>(dispatcher) {
    override suspend fun execute(parameters: Unit): Int {
        return repository.getSeedPhraseEffectiveDelayHours()
    }
}
