package com.nunchuk.android.core.domain

import android.nfc.tech.IsoDep
import com.nunchuk.android.core.util.NFC_CARD_TIMEOUT
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import java.io.IOException
import javax.inject.Inject

class ChangeCvcTapSignerUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<ChangeCvcTapSignerUseCase.Data, Boolean>(dispatcher) {
    override suspend fun execute(parameters: Data): Boolean {
        val card = parameters.isoDep
        card.timeout = NFC_CARD_TIMEOUT
        card.connect()
        try {
            if (card.isConnected) {
                return nunchukNativeSdk.changeCvcTapSigner(
                    isoDep = parameters.isoDep,
                    oldCvc = parameters.oldCvc,
                    newCvc = parameters.newCvc,
                )
            }
        } finally {
            runCatching { card.close() }
        }
        throw IOException("Can not connect nfc card")
    }

    data class Data(val isoDep: IsoDep, val oldCvc: String, val newCvc: String)
}