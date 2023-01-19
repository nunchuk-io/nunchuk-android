package com.nunchuk.android.auth.domain

import com.nunchuk.android.auth.data.AuthRepository
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.setting.QrSignInData
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class TryQrSignInUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val repository: AuthRepository
) : UseCase<String, QrSignInData>(dispatcher) {
    override suspend fun execute(parameters: String): QrSignInData {
        return repository.tryLogin(parameters)
    }
}