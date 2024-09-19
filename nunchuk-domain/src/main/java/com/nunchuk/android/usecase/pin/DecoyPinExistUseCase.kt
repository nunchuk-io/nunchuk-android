package com.nunchuk.android.usecase.pin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.GetOrCreateRootDirUseCase
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class DecoyPinExistUseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    private val getOrCreateRootDirUseCase: GetOrCreateRootDirUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UseCase<String, Boolean>(ioDispatcher) {

    override suspend fun execute(parameters: String): Boolean {
        val path = getOrCreateRootDirUseCase(Unit).getOrThrow()
        return nativeSdk.decoyPinExists(storagePath = path, pin = parameters)
    }
}