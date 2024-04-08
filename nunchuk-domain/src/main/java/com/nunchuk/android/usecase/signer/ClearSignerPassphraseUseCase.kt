package com.nunchuk.android.usecase.signer

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ClearSignerPassphraseUseCase @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk,
) : UseCase<String, Unit>(ioDispatcher) {

    override suspend fun execute(parameters: String) {
        nativeSdk.clearSignerPassphrase(parameters)
    }
}