package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.NunchukMatrixEvent
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetMatrixEventUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk
) : UseCase<String, NunchukMatrixEvent>(dispatcher) {

    override suspend fun execute(parameters: String): NunchukMatrixEvent {
        return nativeSdk.getMatrixEvent(parameters)
    }
}