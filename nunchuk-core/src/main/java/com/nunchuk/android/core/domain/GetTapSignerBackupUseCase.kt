package com.nunchuk.android.core.domain

import android.nfc.tech.IsoDep
import com.nunchuk.android.core.util.NFC_CARD_TIMEOUT
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import java.io.IOException
import javax.inject.Inject

class GetTapSignerBackupUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<GetTapSignerBackupUseCase.Data, String>(dispatcher) {
    override suspend fun execute(parameters: Data): String {
        val card = parameters.isoDep
        card.timeout = NFC_CARD_TIMEOUT
        if (card.isConnected.not()) card.connect()
        if (card.isConnected) {
            return nunchukNativeSdk.getBackupTapSignerKey(
                isoDep = parameters.isoDep,
                cvc = parameters.cvc
            )
        }
        throw IOException("Can not connect nfc card")
    }

    data class Data(val isoDep: IsoDep, val cvc: String)
}