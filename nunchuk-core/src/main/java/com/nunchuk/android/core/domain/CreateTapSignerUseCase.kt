package com.nunchuk.android.core.domain

import android.nfc.tech.IsoDep
import com.nunchuk.android.core.util.NFC_CARD_TIMEOUT
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import java.io.IOException
import javax.inject.Inject

class CreateTapSignerUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<CreateTapSignerUseCase.Data, MasterSigner>(dispatcher) {
    override suspend fun execute(parameters: Data): MasterSigner {
        val card = parameters.isoDep
        card.timeout = NFC_CARD_TIMEOUT
        card.connect()
        try {
            if (card.isConnected) {
                return nunchukNativeSdk.createTapSigner(
                    isoDep = parameters.isoDep,
                    cvc = parameters.cvc,
                    name = parameters.name
                )
            }
        } finally {
            runCatching { card.close() }
        }
        throw IOException("Can not connect nfc card")
    }

    data class Data(val isoDep: IsoDep, val cvc: String, val name: String)
}