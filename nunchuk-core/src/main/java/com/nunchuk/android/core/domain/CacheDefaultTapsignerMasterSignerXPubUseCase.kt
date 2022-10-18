package com.nunchuk.android.core.domain

import android.nfc.tech.IsoDep
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CacheDefaultTapsignerMasterSignerXPubUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    waitAutoCardUseCase: WaitAutoCardUseCase
) : BaseNfcUseCase<CacheDefaultTapsignerMasterSignerXPubUseCase.Data, Unit>(dispatcher, waitAutoCardUseCase) {
    override suspend fun executeNfc(parameters: Data) {
        return nunchukNativeSdk.cacheDefaultTapsignerMasterSignerXPub(parameters.isoDep, parameters.cvc, parameters.masterSignerId)
    }

    class Data(isoDep: IsoDep, val cvc: String, val masterSignerId: String) : BaseNfcUseCase.Data(isoDep)
}