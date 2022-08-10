package com.nunchuk.android.core.domain

import android.nfc.tech.IsoDep
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.NunchukMatrixEvent
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SignRoomTransactionByTapSignerUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    waitAutoCardUseCase: WaitAutoCardUseCase
) : BaseNfcUseCase<SignRoomTransactionByTapSignerUseCase.Data, NunchukMatrixEvent>(dispatcher, waitAutoCardUseCase) {

    override suspend fun executeNfc(parameters: Data): NunchukMatrixEvent {
        return nunchukNativeSdk.signRoomTransactionByTapSigner(
            isoDep = parameters.isoDep,
            cvc = parameters.cvc,
            initEventId = parameters.initEventId,
        )
    }

    class Data(isoDep: IsoDep, val cvc: String, val initEventId: String) : BaseNfcUseCase.Data(isoDep)
}