package com.nunchuk.android.usecase

import com.nunchuk.android.model.NunchukMatrixEvent
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.utils.CrashlyticsReporter
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.*
import javax.inject.Inject

interface ConsumeEventUseCase {
    fun execute(events: List<NunchukMatrixEvent>): Flow<Unit>
}

internal class ConsumeEventUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : ConsumeEventUseCase {

    override fun execute(events: List<NunchukMatrixEvent>) = events.asFlow().flatMapLatest {
        consumeEvent(it)
    }.flowOn(IO)

    private fun consumeEvent(events: NunchukMatrixEvent) = flow {
        emit(
            try {
                nativeSdk.consumeEvent(events)
            } catch (t: Throwable) {
                CrashlyticsReporter.recordException(t)
            }
        )
    }.flowOn(IO)

}