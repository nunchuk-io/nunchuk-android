package com.nunchuk.android.usecase.pin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.GetOrCreateRootDirUseCase
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ChangeDecoyPinUseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    private val getOrCreateRootDirUseCase: GetOrCreateRootDirUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UseCase<ChangeDecoyPinUseCase.Params, Boolean>(ioDispatcher) {

    override suspend fun execute(parameters: Params): Boolean {
        val path = getOrCreateRootDirUseCase(Unit).getOrThrow()
        return nativeSdk.changeDecoyPin(
            storagePath = path,
            oldPin = parameters.oldPin,
            newPin = parameters.newPin
        )
    }

    data class Params(val oldPin: String, val newPin: String)
}