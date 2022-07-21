package com.nunchuk.android.core.domain

import android.nfc.tech.IsoDep
import com.nunchuk.android.core.domain.data.WaitTapSignerUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetSatsCardSlotKeyUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    waitTapSignerUseCase: WaitTapSignerUseCase
) : BaseNfcUseCase<GetSatsCardSlotKeyUseCase.Data, List<SatsCardSlot>>(dispatcher, waitTapSignerUseCase) {
    override suspend fun executeNfc(parameters: Data): List<SatsCardSlot> {
        return nunchukNativeSdk.getSlotKeys(parameters.isoDep, parameters.cvc, parameters.slots)
    }

    class Data(isoDep: IsoDep, val cvc: String, val slots: List<SatsCardSlot>) : BaseNfcUseCase.Data(isoDep)
}