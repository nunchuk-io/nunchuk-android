package com.nunchuk.android.core.domain

import android.nfc.tech.IsoDep
import com.nunchuk.android.core.util.NFC_CARD_TIMEOUT
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.TapSignerStatus
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import java.io.IOException
import javax.inject.Inject

class GetTapSignerStatusUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<IsoDep, TapSignerStatus>(dispatcher) {
    override suspend fun execute(card: IsoDep): TapSignerStatus {
        card.timeout = NFC_CARD_TIMEOUT
        card.connect()
        card.use {
            if (card.isConnected) {
                return nunchukNativeSdk.tapSignerStatus(card)
            }
            throw IOException("Can not connect nfc card")
        }
    }
}