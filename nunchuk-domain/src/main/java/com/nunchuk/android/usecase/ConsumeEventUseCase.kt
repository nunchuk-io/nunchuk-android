package com.nunchuk.android.usecase

import com.nunchuk.android.model.NunchukMatrixEvent
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface ConsumeEventUseCase {
    fun execute(event: NunchukMatrixEvent): Flow<Unit>
}

internal class ConsumeEventUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : ConsumeEventUseCase {

    override fun execute(event: NunchukMatrixEvent) = flow {
        emit(
            nativeSdk.consumeEvent(event = event)
        )
    }

}