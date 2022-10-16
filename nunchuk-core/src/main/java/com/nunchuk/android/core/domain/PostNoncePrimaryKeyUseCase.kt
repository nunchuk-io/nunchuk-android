package com.nunchuk.android.core.domain

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.SignerSoftwareRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class PostNoncePrimaryKeyUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val signerSoftwareRepository: SignerSoftwareRepository
) : UseCase<PostNoncePrimaryKeyUseCase.Param, String>(dispatcher) {
    override suspend fun execute(parameters: Param): String {
        return signerSoftwareRepository.postPKeyNonce(
            address = parameters.address,
            username = parameters.username,
            nonce = parameters.nonce,
            isChangeKey = false
        )
    }

    class Param(val username: String, val address: String?, val nonce: String?)
}