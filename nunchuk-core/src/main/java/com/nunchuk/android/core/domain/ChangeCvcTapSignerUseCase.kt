package com.nunchuk.android.core.domain

import android.nfc.tech.IsoDep
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ChangeCvcTapSignerUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    waitAutoCardUseCase: WaitAutoCardUseCase
) : BaseNfcUseCase<ChangeCvcTapSignerUseCase.Data, Boolean>(dispatcher, waitAutoCardUseCase) {

    override suspend fun executeNfc(parameters: Data): Boolean {
        return nunchukNativeSdk.changeCvcTapSigner(
            isoDep = parameters.isoDep,
            oldCvc = parameters.oldCvc,
            newCvc = parameters.newCvc,
            masterSignerId = parameters.masterSignerId
        )
    }

    class Data(isoDep: IsoDep, val oldCvc: String, val newCvc: String, val masterSignerId: String) : BaseNfcUseCase.Data(isoDep)
}