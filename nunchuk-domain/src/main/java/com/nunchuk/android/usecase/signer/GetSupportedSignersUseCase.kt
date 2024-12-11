package com.nunchuk.android.usecase.signer

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.repository.KeyRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetSupportedSignersUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val keyRepository: KeyRepository
) : UseCase<Unit, List<SupportedSigner>>(ioDispatcher) {

    override suspend fun execute(parameters: Unit): List<SupportedSigner> {
        return keyRepository.getSupportedSigners()
    }
}