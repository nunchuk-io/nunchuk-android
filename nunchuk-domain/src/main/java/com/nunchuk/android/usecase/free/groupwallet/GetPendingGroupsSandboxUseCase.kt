package com.nunchuk.android.usecase.free.groupwallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.GroupSandbox
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetPendingGroupsSandboxUseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : UseCase<Unit, List<GroupSandbox>>(dispatcher) {
    override suspend fun execute(parameters: Unit): List<GroupSandbox> {
        return nativeSdk.getGroups()
    }
}