package com.nunchuk.android.core.domain.signer

import android.nfc.tech.IsoDep
import com.nunchuk.android.core.domain.BaseNfcUseCase
import com.nunchuk.android.core.domain.WaitAutoCardUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetSignerFromTapsignerMasterSignerByPathUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    waitAutoCardUseCase: WaitAutoCardUseCase,
) : BaseNfcUseCase<GetSignerFromTapsignerMasterSignerByPathUseCase.Data, SingleSigner>(
    dispatcher,
    waitAutoCardUseCase
) {

    override suspend fun executeNfc(parameters: Data) : SingleSigner {
       return nunchukNativeSdk.getSignerFromTapsignerMasterSigner(
            isoDep = parameters.isoDep,
            cvc = parameters.cvc,
            masterSignerId = parameters.masterSignerId,
            path = parameters.path,
        )
    }

    class Data(
        isoDep: IsoDep,
        val cvc: String,
        val masterSignerId: String,
        val path: String
    ) : BaseNfcUseCase.Data(isoDep)
}