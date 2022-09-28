package com.nunchuk.android.core.domain

import android.nfc.NdefRecord
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ImportWalletFromMk4UseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val getAppSettingUseCase: GetAppSettingUseCase,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<List<NdefRecord>, Wallet?>(dispatcher) {

    override suspend fun execute(parameters: List<NdefRecord>): Wallet? {
        val appSettings = getAppSettingUseCase.execute().first()
        return nunchukNativeSdk.importWalletFromMk4(appSettings.chain.ordinal, parameters.toTypedArray())
    }
}
