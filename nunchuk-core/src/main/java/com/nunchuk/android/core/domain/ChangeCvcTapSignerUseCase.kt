package com.nunchuk.android.core.domain

import android.nfc.tech.IsoDep
import com.nunchuk.android.core.domain.data.WaitTapSignerUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ChangeCvcTapSignerUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    waitTapSignerUseCase: WaitTapSignerUseCase
) : BaseNfcUseCase<ChangeCvcTapSignerUseCase.Data, Boolean>(dispatcher, waitTapSignerUseCase) {

    override fun executeNfc(parameters: Data): Boolean {
        return nunchukNativeSdk.changeCvcTapSigner(
            isoDep = parameters.isoDep,
            oldCvc = parameters.oldCvc,
            newCvc = parameters.newCvc,
        )
    }

    class Data(isoDep: IsoDep, val oldCvc: String, val newCvc: String) : BaseNfcUseCase.Data(isoDep)
}