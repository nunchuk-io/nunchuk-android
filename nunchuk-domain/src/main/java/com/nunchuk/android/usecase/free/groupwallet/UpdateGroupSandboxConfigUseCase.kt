package com.nunchuk.android.usecase.free.groupwallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.GroupSandbox
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class UpdateGroupSandboxConfigUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk
) : UseCase<UpdateGroupSandboxConfigUseCase.Params, GroupSandbox>(dispatcher) {
    override suspend fun execute(parameters: Params): GroupSandbox {
        return nativeSdk.updateGroupSandbox(
            groupId = parameters.groupId,
            name = parameters.name,
            m = parameters.m,
            n = parameters.n,
            addressType = parameters.addressType.ordinal,
            scriptTmpl = parameters.scriptTmpl.orEmpty()
        )
    }

    data class Params(
        val groupId: String,
        val name: String,
        val m: Int,
        val n: Int,
        val addressType: AddressType,
        val scriptTmpl: String? = null,
    )
}