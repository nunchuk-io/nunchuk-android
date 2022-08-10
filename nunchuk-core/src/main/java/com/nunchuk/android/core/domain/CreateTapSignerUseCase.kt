package com.nunchuk.android.core.domain

import android.nfc.tech.IsoDep
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CreateTapSignerUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    waitAutoCardUseCase: WaitAutoCardUseCase
) : BaseNfcUseCase<CreateTapSignerUseCase.Data, MasterSigner>(dispatcher, waitAutoCardUseCase) {

    override suspend fun executeNfc(parameters: Data): MasterSigner {
        return nunchukNativeSdk.createTapSigner(
            isoDep = parameters.isoDep,
            cvc = parameters.cvc,
            name = parameters.name
        )
    }

    class Data(isoDep: IsoDep, val cvc: String, val name: String) : BaseNfcUseCase.Data(isoDep)
}