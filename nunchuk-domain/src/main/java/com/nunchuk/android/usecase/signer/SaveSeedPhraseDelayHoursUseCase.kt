package com.nunchuk.android.usecase.signer

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.SignerSoftwareRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SaveSeedPhraseDelayHoursUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val repository: SignerSoftwareRepository,
) : UseCase<Int, Unit>(dispatcher) {
    override suspend fun execute(parameters: Int) {
        repository.setSeedPhraseDelayHours(parameters)
    }
}
