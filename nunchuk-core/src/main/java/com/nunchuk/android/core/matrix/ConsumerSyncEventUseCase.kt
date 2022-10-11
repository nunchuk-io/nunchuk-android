package com.nunchuk.android.core.matrix

import com.nunchuk.android.core.repository.SyncEventRepository
import com.nunchuk.android.model.NunchukMatrixEvent
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.utils.CrashlyticsReporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber
import javax.inject.Inject

interface ConsumerSyncEventUseCase {
    fun execute(events: List<NunchukMatrixEvent>): Flow<Unit>
}

internal class ConsumerSyncEventUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    private val repository: SyncEventRepository,
) : ConsumerSyncEventUseCase {

    override fun execute(events: List<NunchukMatrixEvent>) = flow<Unit> {
        events.forEach {
            try {
                if (repository.isSync(it.eventId).not()) {
                    Timber.d("Consume event ${it.eventId}")
                    repository.save(it.eventId)
                    nativeSdk.consumeSyncEvent(it)
                }
            } catch (t: Throwable) {
                CrashlyticsReporter.recordException(t)
            }
        }
    }.flowOn(Dispatchers.IO)

}