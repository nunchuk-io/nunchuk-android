package com.nunchuk.android.usecase.signer

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.GroupSandbox
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SetSlotOccupiedUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk
) : UseCase<SetSlotOccupiedUseCase.Params, GroupSandbox?>(dispatcher) {
    override suspend fun execute(parameters: Params): GroupSandbox? {
        return nativeSdk.setSlotOccupied(
            parameters.groupId,
            parameters.index,
            parameters.value,
            keyName = parameters.keyName.orEmpty()
        )
    }

    data class Params(
        val groupId: String,
        val index: Int,
        val value: Boolean,
        val keyName: String? = null
    )
}