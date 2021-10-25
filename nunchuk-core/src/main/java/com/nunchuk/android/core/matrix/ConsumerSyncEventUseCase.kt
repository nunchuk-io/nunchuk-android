package com.nunchuk.android.core.matrix

import com.nunchuk.android.model.NunchukMatrixEvent
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface ConsumerSyncEventUseCase {
    fun execute(events: List<NunchukMatrixEvent>): Flow<Unit>
}

internal class ConsumerSyncEventUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : ConsumerSyncEventUseCase {

    override fun execute(events: List<NunchukMatrixEvent>) = flow<Unit> {
        events.forEach {
            nativeSdk.consumeSyncEvent(it)
        }
    }

}