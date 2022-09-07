package com.nunchuk.android.core.domain

import android.nfc.NdefRecord
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetMk4SingersUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk
) : UseCase<Array<NdefRecord>, List<SingleSigner>>(dispatcher) {
    override suspend fun execute(parameters: Array<NdefRecord>): List<SingleSigner> {
        return nativeSdk.getMk4Signers(parameters)
    }
}