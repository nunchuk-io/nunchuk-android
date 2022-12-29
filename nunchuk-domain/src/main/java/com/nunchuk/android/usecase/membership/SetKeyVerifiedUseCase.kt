package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.KeyRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SetKeyVerifiedUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val repository: KeyRepository
) : UseCase<SetKeyVerifiedUseCase.Param, Unit>(dispatcher) {
    override suspend fun execute(parameters: Param) {
        repository.setKeyVerified(parameters.masterSignerId, parameters.isAppVerified)
    }

    data class Param(val masterSignerId: String, val isAppVerified: Boolean)
}