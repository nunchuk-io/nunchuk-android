package com.nunchuk.android.core.domain

import android.nfc.NdefRecord
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ImportTransactionFromMk4UseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk
) : UseCase<ImportTransactionFromMk4UseCase.Data, Transaction?>(dispatcher) {
    override suspend fun execute(parameters: Data): Transaction? {
        val result = nativeSdk.importTransactionFromMk4(parameters.walletId, parameters.records.toTypedArray())
        if (parameters.initEventId.isNotEmpty() && parameters.masterFingerPrint.isNotEmpty()) {
            nativeSdk.signAirgapTransaction(parameters.initEventId, parameters.masterFingerPrint)
        }
        return result
    }

    data class Data(val walletId: String, val records: List<NdefRecord>, val initEventId: String, val masterFingerPrint: String)
}