package com.nunchuk.android.core.domain

import android.nfc.NdefRecord
import android.nfc.tech.Ndef
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ExportPsbtToMk4UseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk
) : BaseMk4UseCase<ExportPsbtToMk4UseCase.Data>(dispatcher) {

    override suspend fun executeNfc(parameters: Data): Array<NdefRecord> {
        return nativeSdk.exportPsbtToMk4(parameters.walletId, parameters.txId)
    }

    class Data(val walletId: String, val txId: String, ndef: Ndef) : BaseMk4UseCase.Data(ndef)
}