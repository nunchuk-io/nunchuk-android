package com.nunchuk.android.usecase

import com.nunchuk.android.model.NunchukMatrixEvent
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.utils.CrashlyticsReporter
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface ConsumeEventUseCase {
    fun execute(event: NunchukMatrixEvent): Flow<NunchukMatrixEvent>
}

internal class ConsumeEventUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : ConsumeEventUseCase {

    override fun execute(event: NunchukMatrixEvent) = flow {
        try {
            nativeSdk.consumeEvent(event)
        } catch (t: Throwable) {
            CrashlyticsReporter.recordException(t)
        }
        emit(event)
    }.flowOn(IO)

}