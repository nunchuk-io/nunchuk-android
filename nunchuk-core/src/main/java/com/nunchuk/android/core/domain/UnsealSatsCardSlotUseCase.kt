package com.nunchuk.android.core.domain

import android.nfc.tech.IsoDep
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class UnsealSatsCardSlotUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    waitAutoCardUseCase: WaitAutoCardUseCase
) : BaseNfcUseCase<UnsealSatsCardSlotUseCase.Data, SatsCardSlot>(dispatcher, waitAutoCardUseCase) {
    override suspend fun executeNfc(parameters: Data): SatsCardSlot {
        return nunchukNativeSdk.unsealSatsCard(parameters.isoDep, parameters.cvc, parameters.slot)
    }

    class Data(isoDep: IsoDep, val cvc: String, val slot: SatsCardSlot) : BaseNfcUseCase.Data(isoDep)
}