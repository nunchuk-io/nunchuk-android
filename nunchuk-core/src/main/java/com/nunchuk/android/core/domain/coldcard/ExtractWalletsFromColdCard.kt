package com.nunchuk.android.core.domain.coldcard

import android.nfc.NdefRecord
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ExtractWalletsFromColdCard @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<List<NdefRecord>, List<Wallet>>(dispatcher) {
    override suspend fun execute(parameters: List<NdefRecord>): List<Wallet> {
        return nunchukNativeSdk.getColdCardWallets(parameters.toTypedArray()).orEmpty()
    }
}