package com.nunchuk.android.auth.domain

import com.nunchuk.android.auth.data.AuthRepository
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.setting.QrSignInData
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ConfirmQrSignInUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val repository: AuthRepository
) : UseCase<QrSignInData, Unit>(dispatcher) {
    override suspend fun execute(parameters: QrSignInData) {
        return repository.confirmLogin(parameters.uuid, parameters.token)
    }
}