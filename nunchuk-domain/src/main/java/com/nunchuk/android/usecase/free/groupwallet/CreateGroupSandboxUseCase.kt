package com.nunchuk.android.usecase.free.groupwallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.GroupSandbox
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CreateGroupSandboxUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk
) : UseCase<CreateGroupSandboxUseCase.Params, GroupSandbox>(dispatcher) {
    override suspend fun execute(parameters: Params): GroupSandbox {
        return nativeSdk.createGroupSandbox(
            name = parameters.name,
            m = parameters.m,
            n = parameters.n,
            addressType = parameters.addressType,
        )
    }

    data class Params(
        val name: String,
        val m: Int,
        val n: Int,
        val addressType: AddressType,
    )
}