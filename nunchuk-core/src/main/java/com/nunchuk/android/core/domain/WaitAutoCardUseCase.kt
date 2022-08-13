package com.nunchuk.android.core.domain

import android.nfc.tech.IsoDep
import android.util.ArrayMap
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaitAutoCardUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<IsoDep, Unit>(dispatcher) {
    val needWaitUnlockTap = ArrayMap<String, Boolean>()

    override suspend fun execute(parameters: IsoDep) {
        if (parameters.isConnected) {
            Timber.d("Calling waitTapSigner")
            return nunchukNativeSdk.waitAutoCard(parameters)
        }
        throw IOException("Can not connect nfc card")
    }
}