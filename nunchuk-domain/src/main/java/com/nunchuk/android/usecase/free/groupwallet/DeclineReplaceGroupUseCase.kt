package com.nunchuk.android.usecase.free.groupwallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class DeclineReplaceGroupUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk
) : UseCase<DeclineReplaceGroupUseCase.Params, Unit>(dispatcher) {
    override suspend fun execute(parameters: Params) {
        nativeSdk.declineReplaceGroup(walletId = parameters.walletId, groupId = parameters.groupId)
    }

    data class Params(
        val walletId: String,
        val groupId: String
    )
}