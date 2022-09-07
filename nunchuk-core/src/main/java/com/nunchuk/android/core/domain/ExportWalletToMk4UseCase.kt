package com.nunchuk.android.core.domain

import android.nfc.NdefRecord
import android.nfc.tech.Ndef
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ExportWalletToMk4UseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk
) : BaseMk4UseCase<ExportWalletToMk4UseCase.Data>(dispatcher) {

    override suspend fun executeNfc(parameters: Data): Array<NdefRecord> {
        return nativeSdk.exportWalletToMk4(parameters.walletId)
    }

    class Data(val walletId: String, ndef: Ndef) : BaseMk4UseCase.Data(ndef)
}