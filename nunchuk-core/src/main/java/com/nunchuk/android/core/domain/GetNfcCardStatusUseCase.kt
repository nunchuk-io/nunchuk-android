package com.nunchuk.android.core.domain

import com.nunchuk.android.core.domain.data.WaitTapSignerUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.CardStatus
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetNfcCardStatusUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    waitTapSignerUseCase: WaitTapSignerUseCase
) : BaseNfcUseCase<BaseNfcUseCase.Data, CardStatus>(dispatcher, waitTapSignerUseCase) {
    override suspend fun executeNfc(parameters: Data): CardStatus {
        return nunchukNativeSdk.getAutoCardStatus(parameters.isoDep)
    }
}