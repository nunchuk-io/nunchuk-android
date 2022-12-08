package com.nunchuk.android.core.domain.membership

import android.nfc.tech.IsoDep
import com.nunchuk.android.core.domain.BaseNfcUseCase
import com.nunchuk.android.core.domain.WaitAutoCardUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CheckSignMessageTapsignerUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    waitAutoCardUseCase: WaitAutoCardUseCase
) : BaseNfcUseCase<CheckSignMessageTapsignerUseCase.Param, String>(
    dispatcher,
    waitAutoCardUseCase
) {
    override suspend fun executeNfc(parameters: Param): String {
        return nunchukNativeSdk.signHealthCheckMessageTapSigner(
            isoDep = parameters.isoDep,
            cvc = parameters.cvc,
            signer = parameters.signer,
            messagesToSign = parameters.messageToSign
        )
    }

    class Param(
        isoDep: IsoDep,
        val signer: SingleSigner,
        val cvc: String,
        val messageToSign: String
    ) : Data(isoDep)
}