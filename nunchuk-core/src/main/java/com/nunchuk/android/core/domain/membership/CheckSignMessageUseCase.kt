package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CheckSignMessageUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<CheckSignMessageUseCase.Param, String>(dispatcher) {
    override suspend fun execute(parameters: Param): String {
        return nunchukNativeSdk.signHealthCheckMessage(parameters.signer, parameters.messageToSign)
    }

    class Param(
        val signer: SingleSigner,
        val messageToSign: String,
    )
}