package com.nunchuk.android.core.domain

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.TapSignerStatus
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetTapSignerStatusUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    waitAutoCardUseCase: WaitAutoCardUseCase
) : BaseNfcUseCase<BaseNfcUseCase.Data, TapSignerStatus>(dispatcher, waitAutoCardUseCase) {
    override suspend fun executeNfc(parameters: Data): TapSignerStatus {
        return nunchukNativeSdk.tapSignerStatus(parameters.isoDep)
    }

    override val isAutoRemoveRateLimit: Boolean = false
}