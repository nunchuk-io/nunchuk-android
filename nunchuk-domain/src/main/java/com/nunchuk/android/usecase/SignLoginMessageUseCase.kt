package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SignLoginMessageUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<SignLoginMessageUseCase.Param, String?>(dispatcher) {
    override suspend fun execute(parameters: Param): String? {
        return nunchukNativeSdk.signLoginMessage(parameters.mnemonic, parameters.passphrase, parameters.message)
    }

    data class Param(val mnemonic: String, val passphrase: String, val message: String)
}