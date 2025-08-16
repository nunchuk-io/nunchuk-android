package com.nunchuk.android.usecase.free.groupwallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.GroupSandbox
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class RemoveSignerFromGroupUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk
) : UseCase<RemoveSignerFromGroupUseCase.Params, GroupSandbox>(dispatcher) {
    override suspend fun execute(parameters: Params): GroupSandbox {
        return nativeSdk.removeSignerFromGroup(
            groupId = parameters.groupId,
            index = parameters.index,
            keyName = parameters.keyName.orEmpty()
        )
    }

    data class Params(
        val groupId: String,
        val index: Int,
        val keyName: String? = null
    )
}