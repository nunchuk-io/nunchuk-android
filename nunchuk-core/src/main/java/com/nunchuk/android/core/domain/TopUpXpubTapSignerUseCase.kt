package com.nunchuk.android.core.domain

import android.nfc.tech.IsoDep
import com.nunchuk.android.core.domain.data.WaitTapSignerUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class TopUpXpubTapSignerUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    waitTapSignerUseCase: WaitTapSignerUseCase
) : BaseNfcUseCase<TopUpXpubTapSignerUseCase.Data, Unit>(dispatcher, waitTapSignerUseCase) {
    override suspend fun executeNfc(parameters: Data) {
        return nunchukNativeSdk.tapSignerTopUpXpub(parameters.isoDep, parameters.cvc, parameters.masterSignerId)
    }

    class Data(isoDep: IsoDep, val cvc: String, val masterSignerId: String) : BaseNfcUseCase.Data(isoDep)
}