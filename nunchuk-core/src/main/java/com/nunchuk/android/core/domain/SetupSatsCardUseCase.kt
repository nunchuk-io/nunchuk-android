package com.nunchuk.android.core.domain

import android.content.Context
import android.nfc.tech.IsoDep
import com.nunchuk.android.core.domain.data.WaitTapSignerUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SatsCardStatus
import com.nunchuk.android.nativelib.NunchukNativeSdk
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

@Suppress("BlockingMethodInNonBlockingContext")
class SetupSatsCardUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    @ApplicationContext private val context: Context,
    private val nunchukNativeSdk: NunchukNativeSdk,
    waitTapSignerUseCase: WaitTapSignerUseCase
) : BaseNfcUseCase<SetupSatsCardUseCase.Data, SatsCardStatus>(dispatcher, waitTapSignerUseCase) {

    override suspend fun executeNfc(parameters: Data): SatsCardStatus {
        return nunchukNativeSdk.setupSatsCard(parameters.isoDep, parameters.cvc, parameters.chainCode)
    }

    class Data(isoDep: IsoDep, val cvc: String, val chainCode: String) : BaseNfcUseCase.Data(isoDep)
}