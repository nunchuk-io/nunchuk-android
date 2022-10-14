package com.nunchuk.android.core.domain

import android.nfc.NdefRecord
import android.nfc.tech.Ndef
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GenerateColdCardHealthCheckMessageUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk
) : BaseMk4UseCase<GenerateColdCardHealthCheckMessageUseCase.Data>(dispatcher) {

    override suspend fun executeNfc(parameters: Data): Array<NdefRecord> {
        return nativeSdk.generateColdCardHealthCheckMessage(parameters.derivationPath)
    }

    class Data(val derivationPath: String, ndef: Ndef) : BaseMk4UseCase.Data(ndef)
}