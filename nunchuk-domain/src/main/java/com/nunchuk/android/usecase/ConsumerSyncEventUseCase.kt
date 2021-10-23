package com.nunchuk.android.usecase

import com.nunchuk.android.callbacks.SyncProgress
import com.nunchuk.android.model.Device
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.NunchukMatrixEvent
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.utils.CrashlyticsReporter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface ConsumerSyncEventUseCase {
    fun execute(events: List<NunchukMatrixEvent>): Flow<Int>
}

internal class ConsumerSyncEventUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : ConsumerSyncEventUseCase {

    @ExperimentalCoroutinesApi
    override fun execute(events: List<NunchukMatrixEvent>) = callbackFlow<Int> {
        val callbackSync = object : SyncProgress {
            override fun onSyncProgress(finished: Boolean, progress: Int) {
                if (!finished) {
                    trySend(progress)
                }
            }
        }
        nativeSdk.consumeSyncEvent(events.first(), callbackSync)

        awaitClose ()
//        events.forEach {
//
//        }
    }

}