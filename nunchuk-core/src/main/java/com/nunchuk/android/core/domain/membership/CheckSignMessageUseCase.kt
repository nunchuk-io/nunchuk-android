package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CheckSignMessageUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<CheckSignMessageUseCase.Param, String>(dispatcher) {
    override suspend fun execute(parameters: Param): String {
        val messagesToSign = nunchukNativeSdk.getHealthCheckMessage(parameters.userData)
        var signature = ""
        when (parameters.signer.type) {
            SignerType.SOFTWARE -> {
                signature =
                    nunchukNativeSdk.signHealthCheckMessage(parameters.signer, messagesToSign)
            }
            SignerType.COLDCARD_NFC -> {
                signature = nunchukNativeSdk.signMessageColdCard(
                    parameters.signer.derivationPath, messagesToSign
                )
            }
            else -> {}
        }
        return signature
    }

    class Param(
        val signer: SingleSigner,
        val userData: String
    )
}