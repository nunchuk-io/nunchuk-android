package com.nunchuk.android.core.domain

import android.nfc.tech.IsoDep
import com.nunchuk.android.core.domain.data.WaitTapSignerUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class UnsealSatsCardSlotUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    waitTapSignerUseCase: WaitTapSignerUseCase
) : BaseNfcUseCase<UnsealSatsCardSlotUseCase.Data, SatsCardSlot>(dispatcher, waitTapSignerUseCase) {
    override suspend fun executeNfc(parameters: Data): SatsCardSlot {
        return nunchukNativeSdk.unsealSatsCard(parameters.isoDep, parameters.cvc)
    }

    class Data(isoDep: IsoDep, val cvc: String) : BaseNfcUseCase.Data(isoDep)
}